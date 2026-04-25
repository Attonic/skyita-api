package io.github.skyita.dto.response;

public record SatelliteResponseDto(
        int noradId, //ID NORAD: identificador universal
        String name,
        String launchDate,
        double latitude,
        double longitude,
        double altitudeKm
) {
}
