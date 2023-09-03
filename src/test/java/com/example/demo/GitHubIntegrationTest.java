package com.example.demo;

import com.example.demo.models.BranchDetails;
import com.example.demo.models.RepoDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import com.example.demo.dto.ApiResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GitHubIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();
    private String baseUrl;

    @BeforeEach
    public void setup() {
        baseUrl = "http://localhost:" + port;
    }

    private HttpHeaders createHeadersWithAccept(String mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", mediaType);
        return headers;
    }

    @Test
    @DisplayName("Should return valid repo details when given a valid user with JSON accept header")
    public void shouldReturnValidRepoDetailsWhenGivenValidUserJsonAcceptHeader() {
        HttpHeaders headers = createHeadersWithAccept("application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);


        ResponseEntity<RepoDetails[]> response = restTemplate.exchange(
                baseUrl + "/repositories/validUsername",
                HttpMethod.GET,
                entity,
                RepoDetails[].class
        );

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
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
    @DisplayName("Should return user not found error when given an invalid user")
    public void shouldReturnUserNotFoundErrorWhenGivenInvalidUser() {
        HttpHeaders headers = createHeadersWithAccept("application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                    baseUrl + "/repositories/invalidUsername12311354124313",
                    HttpMethod.GET,
                    entity,
                    ApiResponse.class
            );

            fail("Expected HttpClientErrorException.NotFound to be thrown");
        } catch (HttpClientErrorException.NotFound e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getStatusCode().value());
            assertTrue(e.getResponseBodyAsString().contains("User not found"));
        }
    }

    @Test
    @DisplayName("Should return unsupported data type error when XML accept header is used")
    public void shouldReturnUnsupportedDataTypeErrorWhenXmlAcceptHeaderIsUsed() {
        HttpHeaders headers = createHeadersWithAccept("application/xml");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                    baseUrl + "/repositories/validUsername",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            fail("Expected HttpClientErrorException to be thrown");
        } catch (HttpClientErrorException.NotAcceptable e) {
            assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), e.getStatusCode().value());
            assertTrue(e.getResponseBodyAsString().contains("Unsupported data type"));
        }
    }

}
