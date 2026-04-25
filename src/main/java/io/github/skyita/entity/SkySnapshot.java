package io.github.skyita.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sky_snapshots")
@Getter @Setter
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SkySnapshot {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID skySnapshotId;

    @Column(name = "snapshot_date", nullable = false, unique = true)
    private LocalDate snapshotDate;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Lob
    @Column(name = "satellites_json", columnDefinition = "TEXT")
    private String satellitesJson;

    @Lob
    @Column(name = "planets_json", columnDefinition = "TEXT")
    private String planetsJson;

    @Column(name = "planet_count")
    private Integer planetCount;
}
