package com.homesync.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.homesync.task.entity.Task;
import com.homesync.task.entity.TaskAssignment;
import com.homesync.task.entity.TaskOccurrence;
import com.homesync.task.entity.TaskPriority;
import com.homesync.task.entity.TaskRecurrenceFrequency;
import com.homesync.task.entity.TaskRecurrenceRule;
import com.homesync.task.repository.TaskAssignmentRepository;
import com.homesync.task.repository.TaskOccurrenceRepository;
import com.homesync.task.repository.TaskRecurrenceRuleRepository;
import com.homesync.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskRecurrenceServiceTest {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Madrid");

    @Mock
    private TaskRecurrenceRuleRepository recurrenceRuleRepository;

    @Mock
    private TaskOccurrenceRepository occurrenceRepository;

    @Mock
    private TaskAssignmentRepository assignmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskRecurrenceService taskRecurrenceService;

    private UUID householdId;
    private UUID taskId;
    private UUID ruleId;
    private UUID userId;
    private UUID membershipId;

    private Task task;
    private TaskRecurrenceRule recurrenceRule;

    @BeforeEach
    void setUp() {
        householdId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        userId = UUID.randomUUID();
        membershipId = UUID.randomUUID();

        task = new Task(
                taskId,
                householdId,
                userId,
                "Limpiar salón",
                null,
                TaskPriority.MEDIUM,
                null);
    }

    @Test
    void generateOccurrences_shouldReturnZeroWhenNoRulesExist() {
        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class)))
                .thenReturn(List.of());

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(0, generated);

        verify(recurrenceRuleRepository)
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class));

        verify(recurrenceRuleRepository, never())
                .findByIdForUpdate(any(), any(), any());

        verifyNoInteractions(occurrenceRepository, assignmentRepository, taskRepository);
    }

    @Test
    void generateOccurrences_shouldSkipRuleWhenItNoLongerExists() {
        recurrenceRule = createDailyRule(LocalDate.now(DEFAULT_ZONE));

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class)))
                .thenReturn(List.of(recurrenceRule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                ruleId,
                householdId,
                taskId))
                .thenReturn(Optional.empty());

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(0, generated);

        verify(taskRepository, never())
                .findByIdAndHouseholdIdAndArchivedAtIsNull(any(), any());

        verifyNoInteractions(occurrenceRepository, assignmentRepository);
    }

    @Test
    void generateOccurrences_shouldDeactivateRuleWhenTaskIsArchived() {
        recurrenceRule = createDailyRule(LocalDate.now(DEFAULT_ZONE));

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class)))
                .thenReturn(List.of(recurrenceRule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                ruleId,
                householdId,
                taskId))
                .thenReturn(Optional.of(recurrenceRule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId,
                householdId))
                .thenReturn(Optional.empty());

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(0, generated);
        assertFalse(recurrenceRule.isActive());

        verify(occurrenceRepository, never()).save(any(TaskOccurrence.class));
        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void generateOccurrences_shouldGenerateOccurrenceWithinLeadTime() {
        LocalDate occurrenceDate = LocalDate.now(DEFAULT_ZONE);

        recurrenceRule = createDailyRule(occurrenceDate);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class)))
                .thenReturn(List.of(recurrenceRule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                ruleId,
                householdId,
                taskId))
                .thenReturn(Optional.of(recurrenceRule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId,
                householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId),
                any(OffsetDateTime.class)))
                .thenReturn(false);

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId,
                        taskId))
                .thenReturn(List.of());

        int generated = taskRecurrenceService.generateOccurrences();

        assertTrue(generated >= 1);

        verify(occurrenceRepository, atLeastOnce())
                .save(any(TaskOccurrence.class));
    }

    @Test
    void generateOccurrences_shouldCopyDefaultAssignments() {
        LocalDate occurrenceDate = LocalDate.now(DEFAULT_ZONE);

        recurrenceRule = createDailyRule(occurrenceDate);

        TaskAssignment defaultAssignment = new TaskAssignment(
                UUID.randomUUID(),
                householdId,
                taskId,
                membershipId);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class)))
                .thenReturn(List.of(recurrenceRule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                ruleId,
                householdId,
                taskId))
                .thenReturn(Optional.of(recurrenceRule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId,
                householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId),
                any(OffsetDateTime.class)))
                .thenReturn(false);

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId,
                        taskId))
                .thenReturn(List.of(defaultAssignment));

        int generated = taskRecurrenceService.generateOccurrences();

        assertTrue(generated >= 1);

        verify(assignmentRepository, atLeastOnce())
                .save(argThat(assignment -> assignment.getHouseholdId().equals(householdId)
                        && assignment.getTaskId().equals(taskId)
                        && assignment.getMembershipId().equals(membershipId)
                        && assignment.getOccurrenceId() != null));
    }

    @Test
    void generateOccurrences_shouldNotCreateDuplicateOccurrence() {
        LocalDate occurrenceDate = LocalDate.now(DEFAULT_ZONE);

        recurrenceRule = createDailyRule(occurrenceDate);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class)))
                .thenReturn(List.of(recurrenceRule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                ruleId,
                householdId,
                taskId))
                .thenReturn(Optional.of(recurrenceRule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId,
                householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId),
                any(OffsetDateTime.class)))
                .thenReturn(true);

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(0, generated);

        verify(occurrenceRepository, never()).save(any(TaskOccurrence.class));
        verifyNoInteractions(assignmentRepository);
    }

    private TaskRecurrenceRule createDailyRule(LocalDate startDate) {
        recurrenceRule = new TaskRecurrenceRule(
                ruleId,
                householdId,
                taskId,
                TaskRecurrenceFrequency.DAILY,
                1,
                startDate,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null,
                null,
                60,
                7);

        return recurrenceRule;
    }

    @Test
    void generateOccurrences_shouldGenerateDailyOccurrencesAccordingToInterval() {
        LocalDate startDate = LocalDate.now(DEFAULT_ZONE);

        TaskRecurrenceRule rule = new TaskRecurrenceRule(
                UUID.randomUUID(),
                householdId,
                taskId,
                TaskRecurrenceFrequency.DAILY,
                2,
                startDate,
                startDate.plusDays(3),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null,
                null,
                60,
                7);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any()))
                .thenReturn(List.of(rule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                rule.getId(), householdId, taskId))
                .thenReturn(Optional.of(rule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId, householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId), any(OffsetDateTime.class)))
                .thenReturn(false);

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId, taskId))
                .thenReturn(List.of());

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(2, generated);

        verify(occurrenceRepository, times(2)).save(any(TaskOccurrence.class));
    }

    @Test
    void generateOccurrences_shouldGenerateWeeklyOccurrencesOnConfiguredDays() {
        LocalDate startDate = LocalDate.now(DEFAULT_ZONE)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        TaskRecurrenceRule rule = new TaskRecurrenceRule(
                UUID.randomUUID(),
                householdId,
                taskId,
                TaskRecurrenceFrequency.WEEKLY,
                1,
                startDate,
                startDate.plusWeeks(2),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                new Short[] { (short) 1 },
                null,
                60,
                7);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any()))
                .thenReturn(List.of(rule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                rule.getId(), householdId, taskId))
                .thenReturn(Optional.of(rule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId, householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId), any(OffsetDateTime.class)))
                .thenReturn(false);

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId, taskId))
                .thenReturn(List.of());

        int generated = taskRecurrenceService.generateOccurrences();

        assertTrue(generated >= 1);

        ArgumentCaptor<TaskOccurrence> captor = ArgumentCaptor.forClass(TaskOccurrence.class);

        verify(occurrenceRepository, atLeastOnce()).save(captor.capture());

        for (TaskOccurrence occurrence : captor.getAllValues()) {
            assertEquals(
                    DayOfWeek.MONDAY,
                    occurrence.getDueAt()
                            .atZoneSameInstant(DEFAULT_ZONE)
                            .getDayOfWeek());
        }
    }

    @Test
    void generateOccurrences_shouldRespectConfiguredMonthlyDayAndAdjustShortMonths() {
        LocalDate startDate = LocalDate.now(DEFAULT_ZONE)
                .plusMonths(1)
                .withDayOfMonth(1);

        LocalDate endDate = startDate.plusMonths(3)
                .with(TemporalAdjusters.lastDayOfMonth());

        TaskRecurrenceRule rule = new TaskRecurrenceRule(
                UUID.randomUUID(),
                householdId,
                taskId,
                TaskRecurrenceFrequency.MONTHLY,
                1,
                startDate,
                endDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null,
                (short) 31,
                60,
                365);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class)))
                .thenReturn(List.of(rule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                rule.getId(), householdId, taskId))
                .thenReturn(Optional.of(rule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId, householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId), any(OffsetDateTime.class)))
                .thenReturn(false);

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId, taskId))
                .thenReturn(List.of());

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(4, generated);

        ArgumentCaptor<TaskOccurrence> captor = ArgumentCaptor.forClass(TaskOccurrence.class);

        verify(occurrenceRepository, times(4)).save(captor.capture());

        List<LocalDate> generatedDates = captor.getAllValues()
                .stream()
                .map(occurrence -> occurrence.getDueAt()
                        .atZoneSameInstant(DEFAULT_ZONE)
                        .toLocalDate())
                .toList();

        YearMonth firstMonth = YearMonth.from(startDate);

        assertEquals(List.of(
                firstMonth.atEndOfMonth(),
                firstMonth.plusMonths(1).atEndOfMonth(),
                firstMonth.plusMonths(2).atEndOfMonth(),
                firstMonth.plusMonths(3).atEndOfMonth()), generatedDates);

        assertFalse(rule.isActive());
    }

    @Test
    void generateOccurrences_shouldRespectMonthlyIntervalWhenStartDayHasPassed() {
        LocalDate startDate = LocalDate.now(DEFAULT_ZONE)
                .plusMonths(1)
                .withDayOfMonth(1);

        LocalDate endDate = startDate.plusMonths(5);

        TaskRecurrenceRule rule = new TaskRecurrenceRule(
                UUID.randomUUID(),
                householdId,
                taskId,
                TaskRecurrenceFrequency.MONTHLY,
                2,
                startDate,
                endDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null,
                (short) 15,
                60,
                365);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class)))
                .thenReturn(List.of(rule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                rule.getId(), householdId, taskId))
                .thenReturn(Optional.of(rule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId, householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId), any(OffsetDateTime.class)))
                .thenReturn(false);

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId, taskId))
                .thenReturn(List.of());

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(3, generated);

        ArgumentCaptor<TaskOccurrence> captor = ArgumentCaptor.forClass(TaskOccurrence.class);

        verify(occurrenceRepository, times(3)).save(captor.capture());

        List<LocalDate> generatedDates = captor.getAllValues()
                .stream()
                .map(occurrence -> occurrence.getDueAt()
                        .atZoneSameInstant(DEFAULT_ZONE)
                        .toLocalDate())
                .toList();

        YearMonth firstMonth = YearMonth.from(startDate);

        assertEquals(List.of(
                firstMonth.atDay(15),
                firstMonth.plusMonths(2).atDay(15),
                firstMonth.plusMonths(4).atDay(15)), generatedDates);

        assertFalse(rule.isActive());
    }

    @Test
    void generateOccurrences_shouldNotGenerateOccurrencesAfterEndDate() {
        LocalDate startDate = LocalDate.now(DEFAULT_ZONE);
        LocalDate endDate = startDate.plusDays(1);

        TaskRecurrenceRule rule = new TaskRecurrenceRule(
                UUID.randomUUID(),
                householdId,
                taskId,
                TaskRecurrenceFrequency.DAILY,
                1,
                startDate,
                endDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null,
                null,
                60,
                7);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any()))
                .thenReturn(List.of(rule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                rule.getId(), householdId, taskId))
                .thenReturn(Optional.of(rule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId, householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId), any(OffsetDateTime.class)))
                .thenReturn(false);

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId, taskId))
                .thenReturn(List.of());

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(2, generated);

        ArgumentCaptor<TaskOccurrence> captor = ArgumentCaptor.forClass(TaskOccurrence.class);

        verify(occurrenceRepository, times(2)).save(captor.capture());

        for (TaskOccurrence occurrence : captor.getAllValues()) {
            LocalDate occurrenceDate = occurrence.getDueAt()
                    .atZoneSameInstant(DEFAULT_ZONE)
                    .toLocalDate();

            assertFalse(occurrenceDate.isAfter(endDate));
        }
    }

    @Test
    void generateOccurrences_shouldDeactivateRuleWhenEndDateIsReached() {
        LocalDate startDate = LocalDate.now(DEFAULT_ZONE);
        LocalDate endDate = startDate.plusDays(1);

        TaskRecurrenceRule rule = new TaskRecurrenceRule(
                UUID.randomUUID(),
                householdId,
                taskId,
                TaskRecurrenceFrequency.DAILY,
                1,
                startDate,
                endDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null,
                null,
                60,
                7);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any()))
                .thenReturn(List.of(rule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                rule.getId(), householdId, taskId))
                .thenReturn(Optional.of(rule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId, householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId), any(OffsetDateTime.class)))
                .thenReturn(false);

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId, taskId))
                .thenReturn(List.of());

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(2, generated);
        assertFalse(rule.isActive());
    }

    @Test
    void generateOccurrences_shouldRespectWeeklyInterval() {
        LocalDate startDate = LocalDate.now(DEFAULT_ZONE)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        LocalDate endDate = startDate.plusWeeks(5);

        TaskRecurrenceRule rule = new TaskRecurrenceRule(
                UUID.randomUUID(),
                householdId,
                taskId,
                TaskRecurrenceFrequency.WEEKLY,
                2,
                startDate,
                endDate,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                new Short[] { (short) 1 },
                null,
                60,
                30);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any()))
                .thenReturn(List.of(rule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                rule.getId(), householdId, taskId))
                .thenReturn(Optional.of(rule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId, householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId), any(OffsetDateTime.class)))
                .thenReturn(false);

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId, taskId))
                .thenReturn(List.of());

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(3, generated);

        ArgumentCaptor<TaskOccurrence> captor = ArgumentCaptor.forClass(TaskOccurrence.class);

        verify(occurrenceRepository, times(3)).save(captor.capture());

        List<LocalDate> generatedDates = captor.getAllValues()
                .stream()
                .map(occurrence -> occurrence.getDueAt()
                        .atZoneSameInstant(DEFAULT_ZONE)
                        .toLocalDate())
                .toList();

        assertEquals(startDate, generatedDates.get(0));
        assertEquals(startDate.plusWeeks(2), generatedDates.get(1));
        assertEquals(startDate.plusWeeks(4), generatedDates.get(2));
    }

    @Test
    void generateOccurrences_shouldSkipHistoricalOccurrencesAndGenerateFromToday() {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate startDate = today.minusDays(30);

        TaskRecurrenceRule rule = new TaskRecurrenceRule(
                UUID.randomUUID(),
                householdId,
                taskId,
                TaskRecurrenceFrequency.DAILY,
                1,
                startDate,
                today,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null,
                null,
                60,
                365);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class)))
                .thenReturn(List.of(rule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                rule.getId(), householdId, taskId))
                .thenReturn(Optional.of(rule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId, householdId))
                .thenReturn(Optional.of(task));

        when(occurrenceRepository.existsByTaskIdAndDueAt(
                eq(taskId), any(OffsetDateTime.class)))
                .thenReturn(false);

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId, taskId))
                .thenReturn(List.of());

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(1, generated);

        ArgumentCaptor<TaskOccurrence> captor = ArgumentCaptor.forClass(TaskOccurrence.class);

        verify(occurrenceRepository).save(captor.capture());

        assertEquals(
                today,
                captor.getValue().getDueAt()
                        .atZoneSameInstant(DEFAULT_ZONE)
                        .toLocalDate());

        assertFalse(rule.isActive());
    }

    @Test
    void generateOccurrences_shouldDeactivateExpiredRuleWithoutGeneratingOccurrences() {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);

        TaskRecurrenceRule rule = new TaskRecurrenceRule(
                UUID.randomUUID(),
                householdId,
                taskId,
                TaskRecurrenceFrequency.DAILY,
                1,
                today.minusDays(10),
                today.minusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null,
                null,
                60,
                365);

        when(recurrenceRuleRepository
                .findByActiveTrueAndNextOccurrenceAtLessThanEqual(any(OffsetDateTime.class)))
                .thenReturn(List.of(rule));

        when(recurrenceRuleRepository.findByIdForUpdate(
                rule.getId(), householdId, taskId))
                .thenReturn(Optional.of(rule));

        when(taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(
                taskId, householdId))
                .thenReturn(Optional.of(task));

        int generated = taskRecurrenceService.generateOccurrences();

        assertEquals(0, generated);
        assertFalse(rule.isActive());

        verify(occurrenceRepository, never())
                .save(any(TaskOccurrence.class));

        verifyNoInteractions(assignmentRepository);
    }

}