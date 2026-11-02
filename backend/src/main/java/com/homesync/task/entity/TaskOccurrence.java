package com.homesync.task.entity;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_occurrences")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TaskOccurrence {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Madrid");

    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TaskOccurrenceStatus status = TaskOccurrenceStatus.PENDING;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public TaskOccurrence(
            UUID id,
            UUID householdId,
            UUID taskId,
            OffsetDateTime startsAt,
            OffsetDateTime dueAt) {

        if (startsAt == null || dueAt == null || !dueAt.isAfter(startsAt)) {
            throw new IllegalArgumentException(
                    "El vencimiento debe ser posterior al inicio.");
        }

        OffsetDateTime now = OffsetDateTime.now(DEFAULT_ZONE);

        this.id = id;
        this.householdId = householdId;
        this.taskId = taskId;
        this.startsAt = startsAt;
        this.dueAt = dueAt;
        this.status = TaskOccurrenceStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void changeStatus(TaskOccurrenceStatus status) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "El estado de la ocurrencia es obligatorio.");
        }

        this.status = status;
        this.completedAt = status == TaskOccurrenceStatus.COMPLETED
                ? OffsetDateTime.now(DEFAULT_ZONE)
                : null;
        this.updatedAt = OffsetDateTime.now(DEFAULT_ZONE);
    }

}