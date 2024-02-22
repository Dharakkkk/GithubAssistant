package com.example.demo.records;

import java.util.List;

public record RepoDetails (
    String name,
    String owner,
    List<BranchDetails> branches

) {}
