package com.homesync.task.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homesync.task.entity.Task;
import com.homesync.task.entity.TaskAssignment;
import com.homesync.task.entity.TaskOccurrence;
import com.homesync.task.entity.TaskRecurrenceRule;
import com.homesync.task.repository.TaskAssignmentRepository;
import com.homesync.task.repository.TaskOccurrenceRepository;
import com.homesync.task.repository.TaskRecurrenceRuleRepository;
import com.homesync.task.repository.TaskRepository;

@Service
public class TaskRecurrenceService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Madrid");

    private final TaskRecurrenceRuleRepository recurrenceRuleRepository;
    private final TaskOccurrenceRepository occurrenceRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;

    public TaskRecurrenceService(
            TaskRecurrenceRuleRepository recurrenceRuleRepository,
            TaskOccurrenceRepository occurrenceRepository,
            TaskAssignmentRepository assignmentRepository,
            TaskRepository taskRepository) {

        this.recurrenceRuleRepository = recurrenceRuleRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.assignmentRepository = assignmentRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Genera las ocurrencias que entran dentro del horizonte de generación
     * configurado para cada regla activa.
     *
     * La operación completa se ejecuta dentro de una transacción.
     */
    @Transactional
    public int generateOccurrences() {

        OffsetDateTime now = OffsetDateTime.now(DEFAULT_ZONE);

        List<TaskRecurrenceRule> rules = recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(
                        now.plusDays(365));

        int generated = 0;

        for (TaskRecurrenceRule candidate : rules) {

            TaskRecurrenceRule rule = recurrenceRuleRepository.findByIdForUpdate(
                    candidate.getId(),
                    candidate.getHouseholdId(),
                    candidate.getTaskId())
                    .orElse(null);

            if (rule == null || !rule.isActive()) {
                continue;
            }

            OffsetDateTime generationLimit = now.plusDays(rule.getGenerationLeadDays());

            if (rule.getNextOccurrenceAt().isAfter(generationLimit)) {
                continue;
            }

            Task task = taskRepository
                    .findByIdAndHouseholdIdAndArchivedAtIsNull(
                            rule.getTaskId(),
                            rule.getHouseholdId())
                    .orElse(null);

            if (task == null) {
                rule.deactivate();
                continue;
            }

            generated += generateForRule(rule, generationLimit);
        }

        return generated;
    }

    private int generateForRule(
            TaskRecurrenceRule rule,
            OffsetDateTime generationLimit) {

        int generated = 0;
        LocalDate today = LocalDate.now(DEFAULT_ZONE);

        // Si la regla terminó antes de hoy, no hay ocurrencias que generar.
        if (rule.getEndDate() != null && rule.getEndDate().isBefore(today)) {
            rule.deactivate();
            return generated;
        }

        OffsetDateTime nextOccurrenceAt = rule.getNextOccurrenceAt();

        /*
         * Saltar las fechas históricas antes de generar ocurrencias. Se conserva el
         * patrón de recurrencia configurado.
         */
        while (nextOccurrenceAt.atZoneSameInstant(DEFAULT_ZONE)
                .toLocalDate().isBefore(today)) {

            LocalDate pastDate = nextOccurrenceAt
                    .atZoneSameInstant(DEFAULT_ZONE)
                    .toLocalDate();

            LocalDate nextDate = calculateNextDate(rule, pastDate);

            if (nextDate == null) {
                rule.deactivate();
                return generated;
            }

            nextOccurrenceAt = nextDate
                    .atTime(rule.getStartTime())
                    .atZone(DEFAULT_ZONE)
                    .toOffsetDateTime();

            rule.advanceTo(nextOccurrenceAt);
        }

        // Generar únicamente dentro del horizonte permitido.
        while (!nextOccurrenceAt.isAfter(generationLimit)) {

            LocalDate occurrenceDate = nextOccurrenceAt
                    .atZoneSameInstant(DEFAULT_ZONE)
                    .toLocalDate();

            if (rule.getEndDate() != null
                    && occurrenceDate.isAfter(rule.getEndDate())) {
                rule.deactivate();
                break;
            }

            OffsetDateTime startsAt = occurrenceDate
                    .atTime(rule.getStartTime())
                    .atZone(DEFAULT_ZONE)
                    .toOffsetDateTime();

            OffsetDateTime dueAt = occurrenceDate
                    .atTime(rule.getDueTime())
                    .atZone(DEFAULT_ZONE)
                    .toOffsetDateTime();

            if (!occurrenceRepository.existsByTaskIdAndDueAt(
                    rule.getTaskId(), dueAt)) {

                TaskOccurrence occurrence = new TaskOccurrence(
                        UUID.randomUUID(),
                        rule.getHouseholdId(),
                        rule.getTaskId(),
                        startsAt,
                        dueAt);

                occurrenceRepository.save(occurrence);
                copyDefaultAssignments(rule, occurrence);
                generated++;
            }

            LocalDate nextDate = calculateNextDate(rule, occurrenceDate);

            if (nextDate == null) {
                rule.deactivate();
                break;
            }

            nextOccurrenceAt = nextDate
                    .atTime(rule.getStartTime())
                    .atZone(DEFAULT_ZONE)
                    .toOffsetDateTime();

            rule.advanceTo(nextOccurrenceAt);
        }

        return generated;
    }

    private void copyDefaultAssignments(
            TaskRecurrenceRule rule,
            TaskOccurrence occurrence) {

        List<TaskAssignment> defaultAssignments = assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        rule.getHouseholdId(),
                        rule.getTaskId());

        for (TaskAssignment assignment : defaultAssignments) {

            TaskAssignment occurrenceAssignment = new TaskAssignment(
                    UUID.randomUUID(),
                    rule.getHouseholdId(),
                    rule.getTaskId(),
                    occurrence.getId(),
                    assignment.getMembershipId());

            assignmentRepository.save(occurrenceAssignment);
        }
    }

    private LocalDate calculateNextDate(
            TaskRecurrenceRule rule,
            LocalDate currentDate) {

        return switch (rule.getFrequency()) {

        case DAILY -> currentDate.plusDays(
                rule.getIntervalValue());

        case WEEKLY -> calculateNextWeeklyDate(
                rule, currentDate);

        case MONTHLY -> calculateNextMonthlyDate(
                rule, currentDate);
        };
    }

    private LocalDate calculateNextWeeklyDate(
            TaskRecurrenceRule rule,
            LocalDate currentDate) {

        Short[] configuredDays = rule.getDaysOfWeek();

        if (configuredDays == null || configuredDays.length == 0) {
            throw new IllegalStateException(
                    "La regla semanal no tiene días configurados.");
        }

        LocalDate anchorWeek = rule.getStartDate()
                .with(DayOfWeek.MONDAY);

        LocalDate candidate = currentDate.plusDays(1);

        while (true) {

            long weeksBetween = ChronoUnit.WEEKS.between(
                    anchorWeek,
                    candidate.with(DayOfWeek.MONDAY));

            boolean validInterval = weeksBetween >= 0
                    && weeksBetween % rule.getIntervalValue() == 0;

            int currentDayOfWeek = candidate.getDayOfWeek().getValue();

            boolean validDay = Arrays.stream(configuredDays)
                    .anyMatch(day -> day == currentDayOfWeek);

            if (validInterval && validDay
                    && !candidate.isBefore(rule.getStartDate())) {

                return candidate;
            }

            candidate = candidate.plusDays(1);

            if (rule.getEndDate() != null
                    && candidate.isAfter(rule.getEndDate())) {
                return null;
            }
        }
    }

    private LocalDate calculateNextMonthlyDate(
            TaskRecurrenceRule rule,
            LocalDate currentDate) {

        LocalDate startDate = rule.getStartDate();

        int interval = rule.getIntervalValue();
        int configuredDay = rule.getDayOfMonth() != null
                ? rule.getDayOfMonth()
                : startDate.getDayOfMonth();

        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth currentMonth = YearMonth.from(currentDate);

        long monthsBetween = ChronoUnit.MONTHS.between(
                startMonth,
                currentMonth);

        // Primer mes del patrón estrictamente posterior al mes actual.
        long intervalIndex = Math.max(
                0,
                Math.floorDiv(monthsBetween, interval) + 1);

        YearMonth targetMonth = startMonth.plusMonths(
                intervalIndex * interval);

        LocalDate candidate = targetMonth.atDay(
                Math.min(configuredDay, targetMonth.lengthOfMonth()));

        // Respetar el inicio de la regla.
        if (candidate.isBefore(startDate)) {
            targetMonth = targetMonth.plusMonths(interval);

            candidate = targetMonth.atDay(
                    Math.min(configuredDay, targetMonth.lengthOfMonth()));
        }

        // No generar fechas posteriores al final de la regla.
        if (rule.getEndDate() != null
                && candidate.isAfter(rule.getEndDate())) {
            return null;
        }

        return candidate;
    }

}