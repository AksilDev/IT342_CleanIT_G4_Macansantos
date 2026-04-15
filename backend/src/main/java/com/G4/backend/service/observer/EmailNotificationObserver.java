package com.G4.backend.service.observer;

import com.G4.backend.entity.User;
import org.springframework.stereotype.Component;

/**
 * Observer Implementation: Email Notification Service
 * 
 * Sends email notifications when user events occur.
 * In production, this would integrate with SendGrid, AWS SES, or similar.
 */
@Component
public class EmailNotificationObserver implements UserEventObserver {
    
    @Override
    public void onUserRegistered(User user) {
        // In production: Send welcome email via email service
        System.out.println("📧 Sending welcome email to: " + user.getEmail());
        System.out.println("   Subject: Welcome to CleanIT, " + user.getName() + "!");
        System.out.println("   Body: Thank you for registering. Your account has been created successfully.");
    }
    
    @Override
    public void onUserProfileUpdated(User user) {
        // In production: Send profile update confirmation
        System.out.println("📧 Sending profile update confirmation to: " + user.getEmail());
        System.out.println("   Subject: Your CleanIT profile has been updated");
    }
    
    @Override
    public void onOAuthCompleted(User user) {
        // In production: Send OAuth registration confirmation
        System.out.println("📧 Sending OAuth completion email to: " + user.getEmail());
        System.out.println("   Subject: Google sign-in completed for CleanIT");
        System.out.println("   Body: Your account has been linked with Google successfully.");
    }
}
