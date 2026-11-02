package com.homesync.household.dto;

import com.homesync.household.entity.Household;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HouseholdResponse(
        UUID id,
        String name,
        String timezone,
        UUID createdBy,
        OffsetDateTime createdAt) {

    public static HouseholdResponse from(Household household) {
        return new HouseholdResponse(
                household.getId(),
                household.getName(),
                household.getTimezone(),
                household.getCreatedBy(),
                household.getCreatedAt());
    }
}