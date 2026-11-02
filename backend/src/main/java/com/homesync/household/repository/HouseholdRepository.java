package com.homesync.household.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.homesync.household.entity.Household;

public interface HouseholdRepository extends JpaRepository<Household, UUID> {

    List<Household> findByIdInAndDeletedAtIsNull(
            Collection<UUID> ids);

}