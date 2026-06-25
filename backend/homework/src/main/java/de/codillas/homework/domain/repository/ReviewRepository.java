package de.codillas.homework.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.homework.domain.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {}
