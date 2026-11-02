package com.homesync.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.homesync.household.service.HouseholdService;
import com.homesync.shared.security.CurrentUserService;
import com.homesync.task.dto.CreateTaskRequest;
import com.homesync.task.dto.UpdateTaskRequest;
import com.homesync.task.entity.Task;
import com.homesync.task.entity.TaskPriority;
import com.homesync.task.entity.TaskStatus;
import com.homesync.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private HouseholdService householdService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TaskService taskService;

    private UUID householdId;
    private UUID userId;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        householdId = UUID.randomUUID();
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
    }

    @Test
    void createTask_shouldCreatePendingTask() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Limpiar salón",
                "Limpiar los muebles",
                TaskPriority.HIGH,
                null);

        when(currentUserService.getCurrentUserId()).thenReturn(userId);

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = taskService.createTask(householdId, request);

        assertNotNull(response);
        assertEquals(householdId, response.householdId());
        assertEquals(userId, response.createdBy());
        assertEquals("Limpiar salón", response.title());
        assertEquals(TaskPriority.HIGH, response.priority());
        assertEquals(TaskStatus.PENDING, response.status());

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void getTasks_shouldReturnHouseholdTasks() {
        Task task = new Task(
                taskId,
                householdId,
                userId,
                "Limpiar salón",
                null,
                TaskPriority.MEDIUM,
                null);

        when(taskRepository
                .findByHouseholdIdAndArchivedAtIsNullOrderByCreatedAtDesc(householdId))
                .thenReturn(List.of(task));

        var result = taskService.getTasks(householdId);

        assertEquals(1, result.size());
        assertEquals(taskId, result.get(0).id());
        assertEquals("Limpiar salón", result.get(0).title());
    }

    @Test
    void getTask_shouldReturnTaskWhenItExists() {
        Task task = new Task(
                taskId,
                householdId,
                userId,
                "Limpiar salón",
                null,
                TaskPriority.MEDIUM,
                null);

        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        var response = taskService.getTask(householdId, taskId);

        assertEquals(taskId, response.id());
        assertEquals("Limpiar salón", response.title());
    }

    @Test
    void getTask_shouldThrow404WhenTaskDoesNotExist() {
        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> taskService.getTask(householdId, taskId));
    }

    @Test
    void updateTask_shouldUpdateTaskDetails() {
        Task task = new Task(
                taskId,
                householdId,
                userId,
                "Limpiar salón",
                null,
                TaskPriority.LOW,
                null);

        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest(
                "Limpiar salón y comedor",
                "Aspirar ambas habitaciones",
                TaskPriority.HIGH,
                null);

        var response = taskService.updateTask(householdId, taskId, request);

        assertEquals("Limpiar salón y comedor", response.title());
        assertEquals("Aspirar ambas habitaciones", response.description());
        assertEquals(TaskPriority.HIGH, response.priority());

        verify(taskRepository).save(task);
    }

    @Test
    void updateTaskStatus_shouldMarkTaskAsCompleted() {
        Task task = new Task(
                taskId,
                householdId,
                userId,
                "Limpiar salón",
                null,
                TaskPriority.MEDIUM,
                null);

        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = taskService.updateTaskStatus(
                householdId,
                taskId,
                TaskStatus.COMPLETED);

        assertEquals(TaskStatus.COMPLETED, response.status());
        assertNotNull(response.completedAt());

        verify(taskRepository).save(task);
    }

    @Test
    void archiveTask_shouldSaveArchivedTask() {
        Task task = new Task(
                taskId,
                householdId,
                userId,
                "Limpiar salón",
                null,
                TaskPriority.MEDIUM,
                null);

        when(taskRepository
                .findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId))
                .thenReturn(Optional.of(task));

        taskService.archiveTask(householdId, taskId);

        assertNotNull(task.getArchivedAt());

        verify(taskRepository).save(task);
    }

}