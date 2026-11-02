package com.homesync.household.dto;

import com.homesync.household.entity.HouseholdRole;

import java.util.UUID;

public record HouseholdMemberResponse(
        UUID userId,
        String displayName,
        String email,
        HouseholdRole role) {
}