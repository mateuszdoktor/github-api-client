package com.githubapi.zadanie;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class GitHubClient {

    private final RestClient restClient;

    GitHubClient(RestClient.Builder restClientBuilder,
                 @Value("${github.api.url:https://api.github.com}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    List<GitHubRepository> getRepositories(String username) {
        return restClient.get()
                .uri("/users/{username}/repos", username)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new UserNotFoundException(username);
                })
                .body(new ParameterizedTypeReference<>() {});
    }

    List<GitHubRepository.Branch> getBranches(String owner, String repo) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/branches", owner, repo)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
