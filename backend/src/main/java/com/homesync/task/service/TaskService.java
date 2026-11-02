package com.homesync.task.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.homesync.household.service.HouseholdService;
import com.homesync.shared.security.CurrentUserService;
import com.homesync.task.dto.CreateTaskRequest;
import com.homesync.task.dto.TaskResponse;
import com.homesync.task.dto.UpdateTaskRequest;
import com.homesync.task.entity.Task;
import com.homesync.task.entity.TaskStatus;
import com.homesync.task.repository.TaskRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final HouseholdService householdService;
    private final CurrentUserService currentUserService;

    @Transactional
    public TaskResponse createTask(UUID householdId, CreateTaskRequest request) {
        householdService.getHouseholdById(householdId);

        Task task = new Task(
                UUID.randomUUID(),
                householdId,
                currentUserService.getCurrentUserId(),
                request.title(),
                request.description(),
                request.priority(),
                request.dueAt());

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(UUID householdId) {
        householdService.getHouseholdById(householdId);

        return taskRepository
                .findByHouseholdIdAndArchivedAtIsNullOrderByCreatedAtDesc(householdId)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID householdId, UUID taskId) {
        householdService.getHouseholdById(householdId);

        return TaskResponse.from(findActiveTask(householdId, taskId));
    }

    @Transactional
    public TaskResponse updateTask(
            UUID householdId,
            UUID taskId,
            UpdateTaskRequest request) {

        householdService.getHouseholdById(householdId);

        Task task = findActiveTask(householdId, taskId);

        task.updateDetails(
                request.title(),
                request.description(),
                request.priority(),
                request.dueAt());

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTaskStatus(
            UUID householdId,
            UUID taskId,
            TaskStatus status) {

        householdService.getHouseholdById(householdId);

        Task task = findActiveTask(householdId, taskId);
        task.changeStatus(status);

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void archiveTask(UUID householdId, UUID taskId) {
        householdService.getHouseholdById(householdId);

        Task task = findActiveTask(householdId, taskId);
        task.archive();

        taskRepository.save(task);
    }

    private Task findActiveTask(UUID householdId, UUID taskId) {
        return taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tarea no encontrada."));
    }

}