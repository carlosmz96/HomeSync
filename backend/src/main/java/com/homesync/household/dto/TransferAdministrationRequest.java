package com.homesync.household.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransferAdministrationRequest(
        @NotNull
        UUID newAdminUserId) {
}