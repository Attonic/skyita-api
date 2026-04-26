package io.github.skyita.service;

import io.github.skyita.dto.response.SatelliteResponseDto;

import java.util.List;

public interface SatelliteService {

    List<SatelliteResponseDto> findAbove(double lat, double lon, double altMeters);


}
