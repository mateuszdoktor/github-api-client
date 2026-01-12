package com.githubapi.zadanie;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
class GitHubService {

    private final GitHubClient gitHubClient;

    GitHubService(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    List<GitHubRepository> getUserRepositories(String username) {
        return gitHubClient.getRepositories(username).parallelStream()
                .filter(repo -> !repo.fork())
                .map(repo -> new GitHubRepository(
                        repo.name(),
                        repo.owner(),
                        repo.fork(),
                        gitHubClient.getBranches(repo.owner().login(), repo.name())
                ))
                .toList();
    }
}
