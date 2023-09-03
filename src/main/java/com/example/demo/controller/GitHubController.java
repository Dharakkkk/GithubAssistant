package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.models.RepoDetails;
import com.example.demo.service.GitHubService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/repositories")
public class GitHubController {

    private static final String APPLICATION_JSON = "application/json";

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getRepositories(@PathVariable String username, HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        if (!APPLICATION_JSON.equals(acceptHeader)) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(new ApiResponse(406, "Unsupported data type"));
        }

        try {
            List<RepoDetails> repos = gitHubService.listRepositories(username);

            return ResponseEntity.ok(repos);
        } catch (UserNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(404, "User not found"));
        }
    }

}
