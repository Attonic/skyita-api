package io.github.skyita.service;

import io.github.skyita.dto.response.PlanetResponseDto;

import java.util.List;

public interface PlanetaService {
    List<PlanetResponseDto> findPositions(double lat, double lon);

    List<PlanetResponseDto> findVisible(double lat, double lon);
}
