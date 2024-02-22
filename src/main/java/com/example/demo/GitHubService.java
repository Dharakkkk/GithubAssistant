package com.example.demo;

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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import jakarta.validation.constraints.NotBlank;

@Service
public class GitHubService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);
    private final WebClient webClient;

    public GitHubService(WebClient.Builder webClientBuilder, @Value("${github.baseUrl}") String githubBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(githubBaseUrl).build();
    }

    @CircuitBreaker(name = "github", fallbackMethod = "fallbackListRepositories")
    public Flux<RepoDetails> listRepositories(@NotBlank String username) {
        logger.debug("Fetching repositories for user: {}", username);
        return webClient.get()
                .uri("/users/{username}/repos", username)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        Mono.error(new UserNotFoundException("User not found: " + username)))
                .bodyToFlux(Repository.class)
                .filter(repo -> !repo.fork())
                .flatMap(this::convertToRepoDetails)
                .doOnComplete(() -> logger.info("Finished fetching repositories for user: {}", username));
    }

    private Flux<RepoDetails> fallbackListRepositories(String username, Throwable t) {
        logger.error("Error fetching repositories for user: {}, error: {}", username, t.getMessage());
        throw new UserNotFoundException("User not found: " + username);
    }

    private Mono<RepoDetails> convertToRepoDetails(Repository repo) {
        return getBranchesForRepo(repo.owner().login() + "/" + repo.name())
                .collectList()
                .map(branches -> new RepoDetails(repo.name(), repo.owner().login(), branches));
    }

    public Flux<BranchDetails> getBranchesForRepo(String repoFullName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/"+repoFullName+"/branches")
                        .build())
                .retrieve()
                .bodyToFlux(Branch.class)
                .map(branch -> new BranchDetails(branch.name(), branch.commit().sha()));
    }

}
