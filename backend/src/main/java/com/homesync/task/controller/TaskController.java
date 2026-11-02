package com.homesync.task.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.homesync.task.dto.CreateTaskRequest;
import com.homesync.task.dto.TaskResponse;
import com.homesync.task.dto.UpdateTaskRequest;
import com.homesync.task.dto.UpdateTaskStatusRequest;
import com.homesync.task.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/households/{householdId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateTaskRequest request) {

        return taskService.createTask(householdId, request);
    }

    @GetMapping
    public List<TaskResponse> getTasks(@PathVariable UUID householdId) {
        return taskService.getTasks(householdId);
    }

    @GetMapping("/{taskId}")
    public TaskResponse getTask(
            @PathVariable UUID householdId,
            @PathVariable UUID taskId) {

        return taskService.getTask(householdId, taskId);
    }

    @PatchMapping("/{taskId}")
    public TaskResponse updateTask(
            @PathVariable UUID householdId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {

        return taskService.updateTask(householdId, taskId, request);
    }

    @PatchMapping("/{taskId}/status")
    public TaskResponse updateTaskStatus(
            @PathVariable UUID householdId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {

        return taskService.updateTaskStatus(
                householdId,
                taskId,
                request.status());
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveTask(
            @PathVariable UUID householdId,
            @PathVariable UUID taskId) {

        taskService.archiveTask(householdId, taskId);
    }

}