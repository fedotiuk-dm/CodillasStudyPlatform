package de.codillas.homework.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.homework.domain.model.Grade;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {}
