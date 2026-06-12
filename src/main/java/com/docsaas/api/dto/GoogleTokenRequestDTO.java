package com.docsaas.api.dto;
import jakarta.validation.constraints.NotBlank;
public class GoogleTokenRequestDTO {
	@NotBlank(message = "Google token is required")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
