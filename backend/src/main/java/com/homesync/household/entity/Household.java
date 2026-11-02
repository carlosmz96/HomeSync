package com.homesync.household.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Getter
@Entity
@Table(name = "households")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Household {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Household(
            UUID id,
            String name,
            String timezone,
            UUID createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.timezone = timezone;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateName(String name) {
        this.name = name;
        this.updatedAt = OffsetDateTime.now(
                ZoneId.of("Europe/Madrid"));
    }

}