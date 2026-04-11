package com.G4.backend.service.observer;

import com.G4.backend.entity.User;

/**
 * PATTERN: Observer (Behavioral)
 * 
 * PROBLEM SOLVED:
 * When a user registers or updates their profile, multiple actions need to happen:
 * - Send welcome email
 * - Create default settings
 * - Log registration event
 * - Trigger verification process
 * 
 * Without Observer pattern, AuthService would be bloated with all these responsibilities,
 * violating Single Responsibility Principle and making it hard to add/remove actions.
 * 
 * HOW IT WORKS:
 * Define an observer interface. Multiple observers implement this interface.
 * Subject (UserEventPublisher) maintains a list of observers and notifies them
 * when events occur.
 * 
 * REAL-WORLD EXAMPLE:
 * Event listeners in React (onClick, onSubmit) use observer pattern.
 * Spring's ApplicationEventPublisher is also an observer implementation.
 * 
 * USE CASE IN THIS PROJECT:
 * When user registers, observers handle: email notification, analytics logging,
 * and default profile setup - all decoupled from the registration logic.
 */
public interface UserEventObserver {
    
    /**
     * Called when a new user registers
     * @param user The newly registered user
     */
    void onUserRegistered(User user);
    
    /**
     * Called when user updates their profile
     * @param user The updated user
     */
    void onUserProfileUpdated(User user);
    
    /**
     * Called when user completes OAuth registration
     * @param user The user who completed OAuth
     */
    void onOAuthCompleted(User user);
}
