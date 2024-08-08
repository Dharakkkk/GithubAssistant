package com.example.demo;

import com.example.demo.exception.ServerErrorException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.records.Branch;
import com.example.demo.records.BranchDetails;
import com.example.demo.records.RepoDetails;
import com.example.demo.records.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class GitHubServiceImpl implements GitHubService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubServiceImpl.class);
    private final WebClient webClient;

    public GitHubServiceImpl(WebClient.Builder webClientBuilder, @Value("${github.baseUrl}") String githubBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(githubBaseUrl).build();
    }

    @Override
    public Flux<RepoDetails> listRepositories(String username) {
        if (username == null || username.isEmpty() || !username.matches("^[a-zA-Z0-9-]+$")) {
            logger.warn("Invalid username: {}", username);
            return Flux.error(new IllegalArgumentException("Invalid username: " + username));
        }

        logger.debug("Fetching repositories for user: {}", username);
        return webClient.get()
                .uri("/users/{username}/repos", username)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        clientResponse -> {
                            logger.warn("User not found: {}", username);
                            return Mono.error(new UserNotFoundException("User not found: " + username));
                        })
                .onStatus(HttpStatusCode::is5xxServerError,
                        clientResponse -> {
                            logger.error("Server error occurred for user: {}", username);
                            return Mono.error(new ServerErrorException("Server error occurred while fetching repositories for user: " + username));
                        })
                .bodyToFlux(Repository.class)
                .filter(repo -> !repo.fork())
                .flatMap(this::convertToRepoDetails)
                .limitRate(10)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .doAfterRetry(signal -> logger.warn("Retrying... Attempt: {}", signal.totalRetriesInARow())))
                .doOnError(e -> logger.error("Error fetching repositories for user: {}", username, e))
                .doOnComplete(() -> logger.info("Finished fetching repositories for user: {}", username));
    }

    Mono<RepoDetails> convertToRepoDetails(Repository repo) {
        if (repo == null || repo.name() == null || repo.owner() == null) {
            logger.error("Invalid repository data: {}", repo);
            return Mono.error(new IllegalArgumentException("Invalid repository data"));
        }

        return getBranchesForRepo(repo.owner().login(), repo.name())
                .collectList()
                .map(branches -> new RepoDetails(repo.name(), repo.owner().login(), branches));
    }

    private Flux<BranchDetails> getBranchesForRepo(String ownerLogin, String repoName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/repos/{owner}/{repo}/branches")
                        .build(ownerLogin, repoName))
                .retrieve()
                .bodyToFlux(Branch.class)
                .map(branch -> new BranchDetails(branch.name(), branch.commit().sha()))
                .doOnError(e -> logger.error("Error fetching branches for repo: {}/{}", ownerLogin, repoName, e));
    }
}
