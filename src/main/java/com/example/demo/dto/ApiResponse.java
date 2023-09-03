package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponse {
    private int status;

    @JsonProperty("Message")
    private String Message;
    public ApiResponse(int status, String Message) {
        this.status = status;
        this.Message = Message;
    }

}
