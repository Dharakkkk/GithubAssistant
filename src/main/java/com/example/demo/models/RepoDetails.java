package com.example.demo.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RepoDetails {
    private String name;
    private String owner;
    private List<BranchDetails> branches;

    public RepoDetails(String name, String owner, List<BranchDetails> branches) {
        this.name = name;
        this.owner = owner;
        this.branches = branches;
    }

}
