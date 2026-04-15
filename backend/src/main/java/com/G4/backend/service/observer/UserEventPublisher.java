package com.G4.backend.service.observer;

import com.G4.backend.entity.User;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer Pattern: Subject/Publisher
 * 
 * Manages a list of observers and notifies them of user events.
 * Thread-safe implementation using CopyOnWriteArrayList.
 */
@Component
public class UserEventPublisher {
    
    private final List<UserEventObserver> observers = new CopyOnWriteArrayList<>();
    
    /**
     * Register a new observer
     * @param observer The observer to add
     */
    public void addObserver(UserEventObserver observer) {
        observers.add(observer);
    }
    
    /**
     * Remove an observer
     * @param observer The observer to remove
     */
    public void removeObserver(UserEventObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notify all observers of user registration
     * @param user The newly registered user
     */
    public void publishUserRegistered(User user) {
        observers.forEach(observer -> observer.onUserRegistered(user));
    }
    
    /**
     * Notify all observers of profile update
     * @param user The updated user
     */
    public void publishUserProfileUpdated(User user) {
        observers.forEach(observer -> observer.onUserProfileUpdated(user));
    }
    
    /**
     * Notify all observers of OAuth completion
     * @param user The user who completed OAuth
     */
    public void publishOAuthCompleted(User user) {
        observers.forEach(observer -> observer.onOAuthCompleted(user));
    }
}
