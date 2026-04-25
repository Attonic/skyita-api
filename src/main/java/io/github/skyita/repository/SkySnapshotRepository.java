package io.github.skyita.repository;

import io.github.skyita.entity.SkySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkySnapshotRepository extends JpaRepository<SkySnapshot, UUID> {

    Optional<SkySnapshot> findBySnapshotDate(LocalDate snapshotDate);

    boolean existsBySnapshotDate(LocalDate snapshotDate);
}
