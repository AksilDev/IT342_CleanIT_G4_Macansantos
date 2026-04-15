package com.G4.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Database schema fix to handle technician_id nullable constraint
 */
@Component
public class DatabaseSchemaFix implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if technician_id column allows NULL
            String checkConstraintQuery = """
                SELECT is_nullable 
                FROM information_schema.columns 
                WHERE table_name = 'bookings' 
                AND column_name = 'technician_id'
                """;
            
            String isNullable = jdbcTemplate.queryForObject(checkConstraintQuery, String.class);
            
            if ("NO".equals(isNullable)) {
                System.out.println("Fixing technician_id constraint to allow NULL values...");
                
                // Drop the NOT NULL constraint
                String alterQuery = "ALTER TABLE bookings ALTER COLUMN technician_id DROP NOT NULL";
                jdbcTemplate.execute(alterQuery);
                
                System.out.println("Successfully updated technician_id column to allow NULL values.");
            } else {
                System.out.println("technician_id column already allows NULL values.");
            }
            
        } catch (Exception e) {
            System.err.println("Error fixing database schema: " + e.getMessage());
            // Don't fail the application startup, just log the error
        }
    }
}