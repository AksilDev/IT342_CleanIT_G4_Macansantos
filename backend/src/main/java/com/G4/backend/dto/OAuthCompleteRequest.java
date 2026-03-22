package com.G4.backend.dto;

public class OAuthCompleteRequest {
    private String email;
    private String role;
    private String tempToken;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getTempToken() { return tempToken; }
    public void setTempToken(String tempToken) { this.tempToken = tempToken; }
}
