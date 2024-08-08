package com.example.demo;

import com.example.demo.records.RepoDetails;
import reactor.core.publisher.Flux;

public interface GitHubService {
    Flux<RepoDetails> listRepositories(String username);
}

