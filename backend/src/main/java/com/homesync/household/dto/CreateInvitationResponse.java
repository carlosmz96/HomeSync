package com.homesync.household.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateInvitationResponse(
        UUID invitationId,
        UUID householdId,
        String token,
        OffsetDateTime expiresAt) {
}