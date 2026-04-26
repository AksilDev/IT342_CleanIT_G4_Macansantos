package com.G4.backend.repository;

import com.G4.backend.entity.ServiceAllowedAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceAllowedAddonRepository extends JpaRepository<ServiceAllowedAddon, ServiceAllowedAddon.ServiceAllowedAddonId> {
    
    @Query("SELECT saa.addOn FROM ServiceAllowedAddon saa WHERE saa.service.id = :serviceId")
    List<com.G4.backend.entity.AddOn> findAddOnsByServiceId(@Param("serviceId") UUID serviceId);
    
    @Query("SELECT COUNT(saa) > 0 FROM ServiceAllowedAddon saa WHERE saa.service.id = :serviceId AND saa.addOn.id = :addonId")
    boolean existsByServiceIdAndAddonId(@Param("serviceId") UUID serviceId, @Param("addonId") UUID addonId);
}
