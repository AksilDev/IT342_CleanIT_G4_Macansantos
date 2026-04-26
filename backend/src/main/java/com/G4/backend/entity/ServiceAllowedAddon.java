package com.G4.backend.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "service_allowed_addons")
public class ServiceAllowedAddon {

    @Embeddable
    public static class ServiceAllowedAddonId implements Serializable {
        @Column(name = "service_id")
        private UUID serviceId;

        @Column(name = "addon_id")
        private UUID addonId;

        public ServiceAllowedAddonId() {}

        public ServiceAllowedAddonId(UUID serviceId, UUID addonId) {
            this.serviceId = serviceId;
            this.addonId = addonId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServiceAllowedAddonId that = (ServiceAllowedAddonId) o;
            return Objects.equals(serviceId, that.serviceId) && Objects.equals(addonId, that.addonId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceId, addonId);
        }
    }

    @EmbeddedId
    private ServiceAllowedAddonId id;

    @ManyToOne
    @MapsId("serviceId")
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne
    @MapsId("addonId")
    @JoinColumn(name = "addon_id")
    private AddOn addOn;

    public ServiceAllowedAddon() {}

    public ServiceAllowedAddon(Service service, AddOn addOn) {
        this.service = service;
        this.addOn = addOn;
        this.id = new ServiceAllowedAddonId(service.getId(), addOn.getId());
    }

    // Getters and Setters
    public ServiceAllowedAddonId getId() { return id; }
    public void setId(ServiceAllowedAddonId id) { this.id = id; }

    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }

    public AddOn getAddOn() { return addOn; }
    public void setAddOn(AddOn addOn) { this.addOn = addOn; }
}
