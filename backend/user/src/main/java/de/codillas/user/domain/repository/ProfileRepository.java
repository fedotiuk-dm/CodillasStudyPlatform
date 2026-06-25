package de.codillas.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.user.domain.model.Profile;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

  Optional<Profile> findByUserId(UUID userId);

  Page<Profile> findByDisplayNameContainingIgnoreCase(String displayName, Pageable pageable);
}
