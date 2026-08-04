package io.github.skyita.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sky_snapshots")
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkySnapshot implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sky_snapshot_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID skySnapshotId;

    @Column(name = "snapshot_date", nullable = false, unique = true)
    private LocalDate snapshotDate;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Lob
    @Column(name = "satellites_json", columnDefinition = "TEXT")
    private String satellitesJson;

    @Lob
    @Column(name = "planets_json", columnDefinition = "TEXT")
    private String planetsJson;

    @Column(name = "planet_count")
    private Integer planetCount;

    @Column(name = "satellite_count")
    private Integer satelliteCount;
}
