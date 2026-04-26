package com.G4.backend.repository;

import com.G4.backend.entity.AddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AddOnRepository extends JpaRepository<AddOn, UUID> {
    List<AddOn> findByIsActiveTrue();
    AddOn findByName(String name);
}
