package com.example.demo.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchDetails {
    private String name;
    private String lastCommitSha;

    public BranchDetails(String name, String lastCommitSha) {
        this.name = name;
        this.lastCommitSha = lastCommitSha;
    }

}
