package com.G4.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminConfig {
    
    @Value("${admin.email:admin@cleanit.com}")
    private String adminEmail;
    
    @Value("${admin.password:SuperAdmin123!}")
    private String adminPassword;
    
    @Value("${admin.name:Super Admin}")
    private String adminName;
    
    public String getAdminEmail() {
        return adminEmail;
    }
    
    public String getAdminPassword() {
        return adminPassword;
    }
    
    public String getAdminName() {
        return adminName;
    }
}
