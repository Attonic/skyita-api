package io.github.skyita.service.impl;

import io.github.skyita.dto.response.PlanetResponseDto;
import io.github.skyita.service.PlanetaService;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PlanetServiceImpl implements PlanetaService {

    @Override
    public List<PlanetResponseDto> findPositions(double lat, double lon) {
        return List.of();
    }

    @Override
    public List<PlanetResponseDto> findVisible(double lat, double lon) {
        return List.of();
    }
}
