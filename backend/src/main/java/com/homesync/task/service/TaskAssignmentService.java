package com.homesync.task.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.homesync.household.repository.HouseholdMembershipRepository;
import com.homesync.household.service.HouseholdService;
import com.homesync.task.dto.AssignTaskRequest;
import com.homesync.task.dto.TaskAssignmentResponse;
import com.homesync.task.entity.TaskAssignment;
import com.homesync.task.repository.TaskAssignmentRepository;
import com.homesync.task.repository.TaskRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskAssignmentService {

    private final TaskAssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;
    private final HouseholdMembershipRepository membershipRepository;

    private final HouseholdService householdService;

    @Transactional
    public TaskAssignmentResponse assignTask(
            UUID householdId,
            UUID taskId,
            AssignTaskRequest request) {

        householdService.getHouseholdById(householdId);

        taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tarea no encontrada."));

        var membership = membershipRepository
                .findById(request.membershipId())
                .filter(m -> m.getHouseholdId().equals(householdId))
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Miembro activo no encontrado en el hogar."));

        boolean alreadyAssigned = assignmentRepository
                .existsByTaskIdAndMembershipIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        taskId,
                        membership.getId());

        if (alreadyAssigned) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El miembro ya está asignado a esta tarea.");
        }

        TaskAssignment assignment = new TaskAssignment(
                UUID.randomUUID(),
                householdId,
                taskId,
                membership.getId());

        try {
            TaskAssignment saved = assignmentRepository.saveAndFlush(assignment);
            return TaskAssignmentResponse.from(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "El miembro ya tiene una asignación activa para esta tarea.");
        }
    }

    @Transactional(readOnly = true)
    public List<TaskAssignmentResponse> getTaskAssignments(
            UUID householdId,
            UUID taskId) {

        householdService.getHouseholdById(householdId);

        taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tarea no encontrada."));

        return assignmentRepository
                .findByHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        householdId,
                        taskId)
                .stream()
                .map(TaskAssignmentResponse::from)
                .toList();
    }

    @Transactional
    public void unassignTask(
            UUID householdId,
            UUID taskId,
            UUID assignmentId) {

        householdService.getHouseholdById(householdId);

        taskRepository.findByIdAndHouseholdIdAndArchivedAtIsNull(taskId, householdId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tarea no encontrada."));

        TaskAssignment assignment = assignmentRepository
                .findByIdAndHouseholdIdAndTaskIdAndOccurrenceIdIsNullAndUnassignedAtIsNull(
                        assignmentId,
                        householdId,
                        taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Asignación activa no encontrada."));

        assignment.unassign();
        assignmentRepository.save(assignment);
    }

}