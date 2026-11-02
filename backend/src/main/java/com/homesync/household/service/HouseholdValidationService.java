package com.homesync.household.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.homesync.household.entity.Household;
import com.homesync.household.repository.HouseholdRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseholdValidationService {

    private final HouseholdRepository householdRepository;

    public void validateHouseholdExists(UUID householdId) {
        getActiveHousehold(householdId);
    }

    public Household getActiveHousehold(UUID householdId) {
        return householdRepository.findByIdInAndDeletedAtIsNull(List.of(householdId))
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hogar no encontrado."));
    }

}