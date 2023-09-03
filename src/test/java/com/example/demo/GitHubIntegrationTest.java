package com.example.demo;

import com.example.demo.models.BranchDetails;
import com.example.demo.models.RepoDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import com.example.demo.dto.ApiResponse;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GitHubIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate = new RestTemplate();
    private String baseUrl;

    @BeforeEach
    public void setup() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    public void testListRepositories_ValidUser_JsonAcceptHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<RepoDetails[]> response = restTemplate.exchange(
                baseUrl + "/repositories/validUsername",
                HttpMethod.GET,
                entity,
                RepoDetails[].class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());

        for (RepoDetails repo : response.getBody()) {
            assertNotNull(repo.getName());
            assertNotNull(repo.getOwner());
            assertNotNull(repo.getBranches());
            for (BranchDetails branch : repo.getBranches()) {
                assertNotNull(branch.getName());
                assertNotNull(branch.getLastCommitSha());
            }
        }
    }

    @Test
    public void testListRepositories_InvalidUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                baseUrl + "/repositories/invalidUsername",
                HttpMethod.GET,
                entity,
                ApiResponse.class
        );

        assertEquals(404, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("not found"));
    }

    @Test
    public void testListRepositories_XmlAcceptHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/xml");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                baseUrl + "/repositories/validUsername",
                HttpMethod.GET,
                entity,
                ApiResponse.class
        );

        assertEquals(406, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(406, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Unsupported data type"));
    }

}
