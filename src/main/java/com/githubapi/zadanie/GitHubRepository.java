package com.githubapi.zadanie;

import java.util.List;

record GitHubRepository(String name, Owner owner, boolean fork, List<Branch> branches) {

    record Owner(String login) {}

    record Branch(String name, Commit commit) {
        record Commit(String sha) {}
    }
}
