package com.G4.backend.service.observer;

import com.G4.backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Observer Implementation: Email Notification Service
 * 
 * Sends email notifications when user events occur.
 * In production, this would integrate with SendGrid, AWS SES, or similar.
 */
@Component
public class EmailNotificationObserver implements UserEventObserver {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationObserver.class);
    
    @Override
    public void onUserRegistered(User user) {
        // In production: Send welcome email via email service
        logger.info("📧 Sending welcome email to: {}", user.getEmail());
        logger.info("   Subject: Welcome to CleanIT, {}!", user.getName());
        logger.info("   Body: Thank you for registering. Your account has been created successfully.");
    }
    
    @Override
    public void onUserProfileUpdated(User user) {
        // In production: Send profile update confirmation
        logger.info("📧 Sending profile update confirmation to: {}", user.getEmail());
        logger.info("   Subject: Your CleanIT profile has been updated");
    }
    
    @Override
    public void onOAuthCompleted(User user) {
        // In production: Send OAuth registration confirmation
        logger.info("📧 Sending OAuth completion email to: {}", user.getEmail());
        logger.info("   Subject: Google sign-in completed for CleanIT");
        logger.info("   Body: Your account has been linked with Google successfully.");
    }
}
