package com.G4.backend.dto;

public class LoginResponse {
    private String name;
    private String email;
    private String role;
    private String contactNo;
    private String message;

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
}
