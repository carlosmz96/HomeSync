package com.homesync.task.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.homesync.task.dto.AssignTaskRequest;
import com.homesync.task.dto.TaskAssignmentResponse;
import com.homesync.task.service.TaskAssignmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/households/{householdId}/tasks/{taskId}/assignments")
@RequiredArgsConstructor
public class TaskAssignmentController {

    private final TaskAssignmentService taskAssignmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskAssignmentResponse assignTask(
            @PathVariable UUID householdId,
            @PathVariable UUID taskId,
            @Valid @RequestBody AssignTaskRequest request) {

        return taskAssignmentService.assignTask(householdId, taskId, request);
    }

    @GetMapping
    public List<TaskAssignmentResponse> getTaskAssignments(
            @PathVariable UUID householdId,
            @PathVariable UUID taskId) {

        return taskAssignmentService.getTaskAssignments(householdId, taskId);
    }

    @DeleteMapping("/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignTask(
            @PathVariable UUID householdId,
            @PathVariable UUID taskId,
            @PathVariable UUID assignmentId) {

        taskAssignmentService.unassignTask(householdId, taskId, assignmentId);
    }

}