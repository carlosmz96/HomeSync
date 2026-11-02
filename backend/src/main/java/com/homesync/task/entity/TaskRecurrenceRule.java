package com.homesync.task.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
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
@Table(name = "task_recurrence_rules")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class TaskRecurrenceRule {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Madrid");

    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "task_id", nullable = false, unique = true)
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskRecurrenceFrequency frequency;

    @Column(name = "interval_value", nullable = false)
    private int intervalValue;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_occurrence_at", nullable = false)
    private OffsetDateTime nextOccurrenceAt;

    @Column(name = "generation_lead_days", nullable = false)
    private int generationLeadDays = 7;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "due_time", nullable = false)
    private LocalTime dueTime;

    @Column(name = "days_of_week", columnDefinition = "smallint[]")
    private Short[] daysOfWeek;

    @Column(name = "day_of_month")
    private Short dayOfMonth;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public TaskRecurrenceRule(
            UUID id,
            UUID householdId,
            UUID taskId,
            TaskRecurrenceFrequency frequency,
            int intervalValue,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime dueTime,
            Short[] daysOfWeek,
            Short dayOfMonth,
            int durationMinutes,
            int generationLeadDays) {

        validate(
                frequency,
                intervalValue,
                startDate,
                endDate,
                startTime,
                dueTime,
                daysOfWeek,
                dayOfMonth,
                durationMinutes,
                generationLeadDays);

        OffsetDateTime now = OffsetDateTime.now(DEFAULT_ZONE);

        this.id = id;
        this.householdId = householdId;
        this.taskId = taskId;
        this.frequency = frequency;
        this.intervalValue = intervalValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.dueTime = dueTime;
        this.daysOfWeek = daysOfWeek == null ? null : daysOfWeek.clone();
        this.dayOfMonth = dayOfMonth;
        this.durationMinutes = durationMinutes;
        this.generationLeadDays = generationLeadDays;

        LocalDate firstOccurrenceDate = startDate;

        if (frequency == TaskRecurrenceFrequency.MONTHLY) {
            YearMonth startMonth = YearMonth.from(startDate);

            int configuredDay = dayOfMonth;

            LocalDate candidate = startMonth.atDay(
                    Math.min(configuredDay, startMonth.lengthOfMonth()));

            if (candidate.isBefore(startDate)) {
                candidate = startMonth.plusMonths(intervalValue).atDay(
                        Math.min(
                                configuredDay,
                                startMonth.plusMonths(intervalValue).lengthOfMonth()));
            }

            firstOccurrenceDate = candidate;
        }

        this.nextOccurrenceAt = firstOccurrenceDate.atTime(startTime)
                .atZone(DEFAULT_ZONE)
                .toOffsetDateTime();

        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = OffsetDateTime.now(DEFAULT_ZONE);
    }

    public void advanceTo(OffsetDateTime nextOccurrenceAt) {
        this.nextOccurrenceAt = nextOccurrenceAt;
        this.updatedAt = OffsetDateTime.now(DEFAULT_ZONE);
    }

    private static void validate(
            TaskRecurrenceFrequency frequency,
            int intervalValue,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime dueTime,
            Short[] daysOfWeek,
            Short dayOfMonth,
            int durationMinutes,
            int generationLeadDays) {

        if (frequency == null || startDate == null
                || startTime == null || dueTime == null) {
            throw new IllegalArgumentException(
                    "La frecuencia, fecha inicial y horas son obligatorias.");
        }

        if (intervalValue < 1) {
            throw new IllegalArgumentException(
                    "El intervalo debe ser mayor que cero.");
        }

        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "La fecha final no puede preceder a la fecha inicial.");
        }

        if (!dueTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "La hora de vencimiento debe ser posterior a la de inicio.");
        }

        if (durationMinutes < 1) {
            throw new IllegalArgumentException(
                    "La duración debe ser mayor que cero.");
        }

        if (generationLeadDays < 0 || generationLeadDays > 365) {
            throw new IllegalArgumentException(
                    "Los días de antelación deben estar entre 0 y 365.");
        }

        if (frequency == TaskRecurrenceFrequency.WEEKLY
                && (daysOfWeek == null || daysOfWeek.length == 0)) {
            throw new IllegalArgumentException(
                    "Selecciona al menos un día de la semana.");
        }

        if (daysOfWeek != null) {
            for (Short day : daysOfWeek) {
                if (day == null || day < 1 || day > 7) {
                    throw new IllegalArgumentException(
                            "Los días deben estar entre 1 y 7.");
                }
            }
        }

        if (frequency == TaskRecurrenceFrequency.MONTHLY
                && (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31)) {
            throw new IllegalArgumentException(
                    "Selecciona un día del mes entre 1 y 31.");
        }
    }

}