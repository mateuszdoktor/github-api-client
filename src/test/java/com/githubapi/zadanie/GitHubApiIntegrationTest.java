package com.githubapi.zadanie;

import java.util.List;

import org.apache.commons.lang3.time.StopWatch;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GitHubApiIntegrationTest {

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
    }

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("github.api.url", () -> "http://localhost:" + wireMockServer.port());
    }

    @Test
    void shouldReturnUserRepositoriesWithBranchesExcludingForks() {
        stubFor(WireMock.get(urlEqualTo("/users/testuser/repos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(1000)
                        .withBody("""
                                [
                                    {
                                        "name": "repo1",
                                        "owner": {"login": "testuser"},
                                        "fork": false
                                    },
                                    {
                                        "name": "repo2",
                                        "owner": {"login": "testuser"},
                                        "fork": false
                                    },
                                    {
                                        "name": "forked-repo",
                                        "owner": {"login": "testuser"},
                                        "fork": true
                                    }
                                ]
                                """)));

        stubFor(WireMock.get(urlEqualTo("/repos/testuser/repo1/branches"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(1000)
                        .withBody("""
                                [
                                    {
                                        "name": "main",
                                        "commit": {"sha": "abc123"}
                                    },
                                    {
                                        "name": "develop",
                                        "commit": {"sha": "def456"}
                                    }
                                ]
                                """)));

        stubFor(WireMock.get(urlEqualTo("/repos/testuser/repo2/branches"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(1000)
                        .withBody("""
                                [
                                    {
                                        "name": "main",
                                        "commit": {"sha": "ghi789"}
                                    }
                                ]
                                """)));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<GitHubRepository> repositories = restClient.get()
                .uri("/api/users/testuser/repositories")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTime();

        WireMock.verify(3, getRequestedFor(urlMatching(".*")));

        assertThat(totalTimeMillis)
                .as("Total time should be between 2000-3000 ms")
                .isGreaterThanOrEqualTo(2000)
                .isLessThan(3000);

        assertThat(repositories).hasSize(2);

        GitHubRepository repo1 = repositories.stream()
                .filter(r -> r.name().equals("repo1"))
                .findFirst()
                .orElseThrow();
        assertThat(repo1.owner().login()).isEqualTo("testuser");
        assertThat(repo1.branches()).hasSize(2);
        assertThat(repo1.branches().get(0).name()).isEqualTo("main");
        assertThat(repo1.branches().get(0).commit().sha()).isEqualTo("abc123");
        assertThat(repo1.branches().get(1).name()).isEqualTo("develop");
        assertThat(repo1.branches().get(1).commit().sha()).isEqualTo("def456");

        GitHubRepository repo2 = repositories.stream()
                .filter(r -> r.name().equals("repo2"))
                .findFirst()
                .orElseThrow();
        assertThat(repo2.owner().login()).isEqualTo("testuser");
        assertThat(repo2.branches()).hasSize(1);
        assertThat(repo2.branches().get(0).name()).isEqualTo("main");
        assertThat(repo2.branches().get(0).commit().sha()).isEqualTo("ghi789");
    }

    @Test
    void shouldReturn404WhenUserNotFound() {
        stubFor(WireMock.get(urlEqualTo("/users/nonexistent/repos"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"message": "Not Found"}
                                """)));

        ErrorResponse response = restClient.get()
                .uri("/api/users/nonexistent/repositories")
                .exchange((request, clientResponse) -> {
                    assertThat(clientResponse.getStatusCode().value()).isEqualTo(404);
                    ErrorResponse body = clientResponse.bodyTo(ErrorResponse.class);
                    assertThat(body.status()).isEqualTo(404);
                    assertThat(body.message()).isEqualTo("User not found: nonexistent");
                    return body;
                });
    }
}
