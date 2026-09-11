package com.omnisync.hubspot.model;

import java.util.Objects;

/**
 * Typed domain representation of a HubSpot CRM contact.
 */
public record HubSpotContact(
        String id,
        String email,
        String firstName,
        String lastName,
        String company,
        String phone,
        String createdAt,
        String updatedAt,
        boolean archived
) {
    public HubSpotContact {
        Objects.requireNonNull(id, "id must not be null");
        email = email != null ? email : "";
        firstName = firstName != null ? firstName : "";
        lastName = lastName != null ? lastName : "";
        company = company != null ? company : "";
        phone = phone != null ? phone : "";
        createdAt = createdAt != null ? createdAt : "";
        updatedAt = updatedAt != null ? updatedAt : "";
    }
}
