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
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Task {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Madrid");

    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "is_recurring", nullable = false)
    private boolean recurring;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    public Task(
            UUID id,
            UUID householdId,
            UUID createdBy,
            String title,
            String description,
            TaskPriority priority,
            OffsetDateTime dueAt) {

        OffsetDateTime now = OffsetDateTime.now(DEFAULT_ZONE);

        this.id = id;
        this.householdId = householdId;
        this.createdBy = createdBy;
        this.title = title.trim();
        this.description = description;
        this.priority = priority != null ? priority : TaskPriority.MEDIUM;
        this.status = TaskStatus.PENDING;
        this.dueAt = dueAt;
        this.recurring = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateDetails(
            String title,
            String description,
            TaskPriority priority,
            OffsetDateTime dueAt) {

        this.title = title.trim();
        this.description = description;
        this.priority = priority != null ? priority : TaskPriority.MEDIUM;
        this.dueAt = dueAt;
        this.updatedAt = OffsetDateTime.now(DEFAULT_ZONE);
    }

    public void changeStatus(TaskStatus status) {
        this.status = status;
        this.completedAt = status == TaskStatus.COMPLETED
                ? OffsetDateTime.now(DEFAULT_ZONE)
                : null;
        this.updatedAt = OffsetDateTime.now(DEFAULT_ZONE);
    }

    public void archive() {
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_ZONE);
        this.archivedAt = now;
        this.updatedAt = now;
    }

}