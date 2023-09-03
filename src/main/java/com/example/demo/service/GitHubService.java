package com.example.demo.service;

import com.example.demo.exception.RepositoriesFetchFailedException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.models.Branch;
import com.example.demo.models.BranchDetails;
import com.example.demo.models.RepoDetails;
import com.example.demo.models.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GitHubService {

    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://api.github.com";

    public GitHubService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<RepoDetails> listRepositories(String username) {
        String url = String.format("%s/users/%s/repos", BASE_URL, username);
        Repository[] repositories;

        try {
            repositories = restTemplate.getForObject(url, Repository[].class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new UserNotFoundException("User not found");
        }

        if (repositories == null) {
            throw new RepositoriesFetchFailedException("Failed to fetch repositories");
        }

        return Stream.of(repositories)
                .filter(repo -> !repo.isFork())
                .map(this::convertToRepoDetails)
                .collect(Collectors.toList());
    }

    private RepoDetails convertToRepoDetails(Repository repo) {
        List<BranchDetails> branches = getBranchesForRepo(repo.getOwner().getLogin() + "/" + repo.getName());

        return new RepoDetails(repo.getName(), repo.getOwner().getLogin(), branches);
    }

    private List<BranchDetails> getBranchesForRepo(String repoFullName) {
        String url = BASE_URL + "/repos/" + repoFullName + "/branches";
        Branch[] branches = restTemplate.getForObject(url, Branch[].class);

        if (branches == null) {
            throw new RuntimeException("Failed to fetch branches");
        }

        return Stream.of(branches)
                .map(branch -> new BranchDetails(branch.getName(),
                        branch.getCommit().getSha()))
                .collect(Collectors.toList());
    }

}
