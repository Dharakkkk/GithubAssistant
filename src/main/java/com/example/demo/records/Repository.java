package com.example.demo.records;

public record Repository (
    String name,
    Owner owner,
    boolean fork

){}
