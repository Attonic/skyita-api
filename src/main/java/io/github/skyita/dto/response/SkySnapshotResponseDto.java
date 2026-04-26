package io.github.skyita.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SkySnapshotResponseDto(
        UUID id,
        LocalDate snapshotDate,
        LocalDateTime generatedAt,
        String city,
        double latitude,
        double longitude,
        List<SatelliteResponseDto> satellites,
        List<PlanetResponseDto> planets,
        int satelliteCount,
        int planetCount
) {
}
