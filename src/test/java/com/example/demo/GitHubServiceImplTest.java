package com.example.demo;

import com.example.demo.exception.ServerErrorException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.records.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

class GitHubServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GitHubServiceImpl gitHubServiceImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Zmockowanie buildera WebClient i zwrócenie zmockowanego WebClienta
        doReturn(webClientBuilder).when(webClientBuilder).baseUrl(any(String.class));
        doReturn(webClient).when(webClientBuilder).build();

        // Ręczna inicjalizacja GitHubServiceImpl
        gitHubServiceImpl = new GitHubServiceImpl(webClientBuilder, "http://mocked.base.url");
    }

    private void mockWebClientForRepositories(Repository... repositories) {
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
        doReturn(Flux.just(repositories)).when(responseSpec).bodyToFlux(Repository.class);
    }

    private void mockWebClientForBranches(Branch... branches) {
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(Function.class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(Flux.just(branches)).when(responseSpec).bodyToFlux(Branch.class);
    }

    @Test
    @DisplayName("Should return repository details when the user exists")
    void listRepositories_UserExists_ReturnsRepoDetails() {
        // Mockowanie odpowiedzi WebClient dla listy repozytoriów
        mockWebClientForRepositories(new Repository("repo1", new Owner("user1"), false));
        mockWebClientForBranches(new Branch("main", new Commit("sha1")));

        Flux<RepoDetails> repoDetailsFlux = gitHubServiceImpl.listRepositories("user1");

        // Weryfikacja wyników
        StepVerifier.create(repoDetailsFlux)
                .expectNextMatches(repoDetails ->
                        repoDetails.name().equals("repo1") &&
                                repoDetails.owner().equals("user1") &&
                                repoDetails.branches().size() == 1 &&
                                repoDetails.branches().getFirst().name().equals("main")
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when the user does not exist")
    void listRepositories_UserNotFound_ThrowsUserNotFoundException() {
        // Mockowanie odpowiedzi 404 z WebClient
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();

        // Mockowanie `onStatus` aby zwracało `responseSpec` dalej
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());

        doReturn(Flux.error(new UserNotFoundException("User not found: user1")))
                .when(responseSpec).bodyToFlux(Repository.class);

        Flux<RepoDetails> repoDetailsFlux = gitHubServiceImpl.listRepositories("user1");

        // Weryfikacja, że wyjątek zostanie rzucony
        StepVerifier.create(repoDetailsFlux)
                .expectErrorMatches(throwable -> throwable instanceof UserNotFoundException &&
                        throwable.getMessage().equals("User not found: user1"))
                .verify();
    }


    @Test
    @DisplayName("Should throw ServerErrorException when a server error occurs")
    void listRepositories_ServerError_ThrowsServerErrorException() {
        // Mockowanie odpowiedzi 500 z WebClient
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();

        // Mockowanie `onStatus` aby zwracało `responseSpec` dalej
        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());

        doReturn(Flux.error(new ServerErrorException("Server error")))
                .when(responseSpec).bodyToFlux(Repository.class);

        Flux<RepoDetails> repoDetailsFlux = gitHubServiceImpl.listRepositories("user1");

        // Weryfikacja, że wyjątek zostanie rzucony
        StepVerifier.create(repoDetailsFlux)
                .expectErrorMatches(throwable -> throwable.getCause() instanceof ServerErrorException &&
                        throwable.getCause().getMessage().equals("Server error"))
                .verify();
    }

    @Test
    @DisplayName("Should handle invalid username")
    void listRepositories_InvalidUsername_ThrowsIllegalArgumentException() {
        Flux<RepoDetails> repoDetailsFlux = gitHubServiceImpl.listRepositories("invalid username!");

        // Weryfikacja, że wyjątek zostanie rzucony
        StepVerifier.create(repoDetailsFlux)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Invalid username: invalid username!"))
                .verify();
    }

    @Test
    @DisplayName("Should handle invalid repository data")
    void convertToRepoDetails_InvalidRepositoryData_ThrowsIllegalArgumentException() {
        Repository invalidRepo = new Repository(null, new Owner("user1"), false);

        Mono<RepoDetails> repoDetailsMono = gitHubServiceImpl.convertToRepoDetails(invalidRepo);

        // Weryfikacja, że wyjątek zostanie rzucony
        StepVerifier.create(repoDetailsMono)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Invalid repository data"))
                .verify();
    }

}
