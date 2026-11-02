package com.homesync.household.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.homesync.household.dto.AcceptInvitationRequest;
import com.homesync.household.dto.CreateHouseholdRequest;
import com.homesync.household.dto.CreateInvitationResponse;
import com.homesync.household.dto.HouseholdMemberResponse;
import com.homesync.household.dto.HouseholdResponse;
import com.homesync.household.dto.TransferAdministrationRequest;
import com.homesync.household.dto.UpdateHouseholdRequest;
import com.homesync.household.entity.Household;
import com.homesync.household.service.HouseholdInvitationService;
import com.homesync.household.service.HouseholdMembershipService;
import com.homesync.household.service.HouseholdService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/households")
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdService householdService;
    private final HouseholdInvitationService householdInvitationService;
    private final HouseholdMembershipService householdMembershipService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdResponse createHousehold(
            @Valid @RequestBody CreateHouseholdRequest request) {

        Household household = householdService.createHousehold(request.name());

        return HouseholdResponse.from(household);
    }

    @GetMapping
    public List<HouseholdResponse> getCurrentUserHouseholds() {
        return householdService.getCurrentUserHouseholds()
                .stream()
                .map(HouseholdResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public HouseholdResponse getHouseholdById(
            @PathVariable UUID id) {

        return HouseholdResponse.from(
                householdService.getHouseholdById(id));
    }

    @PatchMapping("/{id}")
    public HouseholdResponse updateHouseholdName(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHouseholdRequest request) {

        return HouseholdResponse.from(
                householdService.updateHouseholdName(id, request.name()));
    }

    @GetMapping("/{id}/members")
    public List<HouseholdMemberResponse> getHouseholdMembers(
            @PathVariable UUID id) {

        return householdMembershipService.getHouseholdMembers(id);
    }

    @PostMapping("/{id}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInvitationResponse createInvitation(@PathVariable UUID id) {
        return householdInvitationService.createInvitation(id);
    }

    @PostMapping("/invitations/accept")
    public HouseholdResponse acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request) {

        Household household = householdInvitationService.acceptInvitation(request.token());

        return HouseholdResponse.from(household);
    }

    @DeleteMapping("/{householdId}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvitation(
            @PathVariable UUID householdId,
            @PathVariable UUID invitationId) {

        householdInvitationService.revokeInvitation(householdId, invitationId);
    }

    @DeleteMapping("/{householdId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveHousehold(@PathVariable UUID householdId) {
        householdMembershipService.leaveHousehold(householdId);
    }

    @PutMapping("/{householdId}/administrators")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferAdministration(
            @PathVariable UUID householdId,
            @Valid @RequestBody TransferAdministrationRequest request) {

        householdMembershipService.transferAdministration(
                householdId,
                request.newAdminUserId());
    }

}