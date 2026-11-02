package com.homesync.task.entity;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_assignments")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TaskAssignment {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Madrid");

    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "occurrence_id")
    private UUID occurrenceId;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "unassigned_at")
    private OffsetDateTime unassignedAt;

    public TaskAssignment(
            UUID id,
            UUID householdId,
            UUID taskId,
            UUID membershipId) {

        this.id = id;
        this.householdId = householdId;
        this.taskId = taskId;
        this.membershipId = membershipId;
        this.occurrenceId = null;
        this.assignedAt = OffsetDateTime.now(DEFAULT_ZONE);
        this.unassignedAt = null;
    }

    public TaskAssignment(
            UUID id,
            UUID householdId,
            UUID taskId,
            UUID occurrenceId,
            UUID membershipId) {

        this.id = id;
        this.householdId = householdId;
        this.taskId = taskId;
        this.occurrenceId = occurrenceId;
        this.membershipId = membershipId;
        this.assignedAt = OffsetDateTime.now(DEFAULT_ZONE);
        this.unassignedAt = null;
    }

    public void unassign() {
        if (this.unassignedAt == null) {
            this.unassignedAt = OffsetDateTime.now(DEFAULT_ZONE);
        }
    }

}