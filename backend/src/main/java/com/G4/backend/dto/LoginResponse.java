package com.G4.backend.dto;

public class LoginResponse {
    private String name;
    private String email;
    private String role;
    private String contactNo;
    private String message;
    private String token;
    private boolean verified;

    public LoginResponse() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    // Builder Pattern
    public static class Builder {
        private final LoginResponse response = new LoginResponse();

        public Builder name(String name) { response.setName(name); return this; }
        public Builder email(String email) { response.setEmail(email); return this; }
        public Builder role(String role) { response.setRole(role); return this; }
        public Builder contactNo(String contactNo) { response.setContactNo(contactNo); return this; }
        public Builder message(String message) { response.setMessage(message); return this; }
        public Builder token(String token) { response.setToken(token); return this; }
        public Builder verified(boolean verified) { response.setVerified(verified); return this; }

        public LoginResponse build() { return response; }
    }
}
