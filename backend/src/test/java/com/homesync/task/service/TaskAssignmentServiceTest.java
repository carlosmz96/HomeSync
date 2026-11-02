package com.homesync.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import com.homesync.household.entity.HouseholdMembership;
import com.homesync.household.repository.HouseholdMembershipRepository;
import com.homesync.household.service.HouseholdService;
import com.homesync.task.dto.AssignTaskRequest;
import com.homesync.task.entity.Task;
import com.homesync.task.entity.TaskAssignment;
import com.homesync.task.entity.TaskPriority;
import com.homesync.task.repository.TaskAssignmentRepository;
import com.homesync.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskAssignmentServiceTest {

    @Mock
    private TaskAssignmentRepository assignmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private HouseholdMembershipRepository membershipRepository;

    @Mock
    private HouseholdService householdService;

    @InjectMocks
    private TaskAssignmentService taskAssignmentService;

    private UUID householdId;
    private UUID taskId;
    private UUID membershipId;
    private UUID assignmentId;
    private UUID userId;

    private Task task;
    private HouseholdMembership membership;

    @BeforeEach
    void setUp() {
        householdId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        membershipId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        task = new Task(
                taskId,
                householdId,
                userId,
                "Limpiar salón",
                null,
                TaskPriority.MEDIUM,
                null);

        membership = mock(HouseholdMembership.class);
    }

    @Test
    void assignTask_shouldAssignActiveMember() {
        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        when(membershipRepository.findById(membershipId))
                .thenReturn(Optional.of(membership));

        when(membership.getId()).thenReturn(membershipId);
        when(membership.getHouseholdId()).thenReturn(householdId);
        when(membership.getStatus()).thenReturn("ACTIVE");

        when(assignmentRepository
                .existsByTaskIdAndMembershipIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        taskId, membershipId))
                .thenReturn(false);

        when(assignmentRepository.saveAndFlush(any(TaskAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = taskAssignmentService.assignTask(
                householdId,
                taskId,
                new AssignTaskRequest(membershipId));

        assertNotNull(response);
        assertEquals(householdId, response.householdId());
        assertEquals(taskId, response.taskId());
        assertEquals(membershipId, response.membershipId());

        verify(assignmentRepository).saveAndFlush(any(TaskAssignment.class));
    }

    @Test
    void assignTask_shouldRejectArchivedOrMissingTask() {
        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskAssignmentService.assignTask(
                        householdId,
                        taskId,
                        new AssignTaskRequest(membershipId)));

        assertEquals(404, exception.getStatusCode().value());

        verifyNoInteractions(membershipRepository);
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void assignTask_shouldRejectMembershipFromAnotherHousehold() {
        UUID otherHouseholdId = UUID.randomUUID();

        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        when(membershipRepository.findById(membershipId))
                .thenReturn(Optional.of(membership));

        when(membership.getHouseholdId()).thenReturn(otherHouseholdId);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskAssignmentService.assignTask(
                        householdId,
                        taskId,
                        new AssignTaskRequest(membershipId)));

        assertEquals(404, exception.getStatusCode().value());

        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void assignTask_shouldRejectInactiveMembership() {
        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        when(membershipRepository.findById(membershipId))
                .thenReturn(Optional.of(membership));

        when(membership.getHouseholdId()).thenReturn(householdId);
        when(membership.getStatus()).thenReturn("REMOVED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskAssignmentService.assignTask(
                        householdId,
                        taskId,
                        new AssignTaskRequest(membershipId)));

        assertEquals(404, exception.getStatusCode().value());

        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void assignTask_shouldRejectDuplicateAssignment() {
        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        when(membershipRepository.findById(membershipId))
                .thenReturn(Optional.of(membership));

        when(membership.getId()).thenReturn(membershipId);
        when(membership.getHouseholdId()).thenReturn(householdId);
        when(membership.getStatus()).thenReturn("ACTIVE");

        when(assignmentRepository
                .existsByTaskIdAndMembershipIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        taskId, membershipId))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskAssignmentService.assignTask(
                        householdId,
                        taskId,
                        new AssignTaskRequest(membershipId)));

        assertEquals(409, exception.getStatusCode().value());

        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void assignTask_shouldReturnConflictWhenDatabaseRejectsDuplicate() {
        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        when(membershipRepository.findById(membershipId))
                .thenReturn(Optional.of(membership));

        when(membership.getId()).thenReturn(membershipId);
        when(membership.getHouseholdId()).thenReturn(householdId);
        when(membership.getStatus()).thenReturn("ACTIVE");

        when(assignmentRepository
                .existsByTaskIdAndMembershipIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        taskId, membershipId))
                .thenReturn(false);

        when(assignmentRepository.saveAndFlush(any(TaskAssignment.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate assignment"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskAssignmentService.assignTask(
                        householdId,
                        taskId,
                        new AssignTaskRequest(membershipId)));

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void getTaskAssignments_shouldReturnActiveAssignments() {
        TaskAssignment assignment = new TaskAssignment(
                assignmentId,
                householdId,
                taskId,
                membershipId);

        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        when(assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId, taskId))
                .thenReturn(List.of(assignment));

        var responses = taskAssignmentService.getTaskAssignments(householdId, taskId);

        assertEquals(1, responses.size());
        assertEquals(assignmentId, responses.get(0).id());
        assertEquals(membershipId, responses.get(0).membershipId());
    }

    @Test
    void unassignTask_shouldCloseAssignment() {
        TaskAssignment assignment = new TaskAssignment(
                assignmentId,
                householdId,
                taskId,
                membershipId);

        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        when(assignmentRepository
                .findByIdAndHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        assignmentId, householdId, taskId))
                .thenReturn(Optional.of(assignment));

        taskAssignmentService.unassignTask(householdId, taskId, assignmentId);

        assertNotNull(assignment.getUnassignedAt());
        verify(assignmentRepository).save(assignment);
    }

    @Test
    void unassignTask_shouldReturn404WhenAssignmentDoesNotExist() {
        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        when(assignmentRepository
                .findByIdAndHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        assignmentId, householdId, taskId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskAssignmentService.unassignTask(
                        householdId,
                        taskId,
                        assignmentId));

        assertEquals(404, exception.getStatusCode().value());
    }

}