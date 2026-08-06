package io.github.skyita.service.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import io.github.skyita.dto.response.PlanetResponseDto;
import io.github.skyita.dto.response.SatelliteResponseDto;
import io.github.skyita.dto.response.SkySnapshotResponseDto;
import io.github.skyita.entity.SkySnapshot;
import io.github.skyita.exception.ExternalApiException;
import io.github.skyita.repository.SkySnapshotRepository;
import io.github.skyita.service.PlanetaService;
import io.github.skyita.service.SatelliteService;
import io.github.skyita.service.SkyDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkyDataServiceImpl implements SkyDataService {

    private final SatelliteService satelliteService;
    private final PlanetaService planetaService;
    private final SkySnapshotRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${app.location.city}")
    private String city;

    @Value("${app.location.latitude}")
    private double latitude;

    @Value("${app.location.longitude}")
    private double longitude;

    @Value("${app.location.altitude}")
    private double altitude;

    @Override
    public SkySnapshot fetchAndSave() {
        LocalDate today = LocalDate.now();

        // Idempotência: se já existe snapshot para hoje, retorna o existente
        Optional<SkySnapshot> existing = repository.findBySnapshotDate(today);
        if (existing.isPresent()) {
            log.info("Snapshot para {} já existe. Retornando existente.", today);
            return existing.get();
        }

        // Busca dados das APIs externas
        List<SatelliteResponseDto> satellites =
                satelliteService.findAbove(latitude, longitude, altitude);
        List<PlanetResponseDto> planets =
                planetaService.findPositions(latitude, longitude);

        // Serializa as listas para JSON
        String satellitesJson = toJson(satellites);
        String planetsJson = toJson(planets);

        // Monta e persiste o snapshot
        SkySnapshot snapshot = SkySnapshot.builder()
                .snapshotDate(today)
                .generatedAt(LocalDateTime.now())
                .city(city)
                .latitude(latitude)
                .longitude(longitude)
                .satellitesJson(satellitesJson)
                .planetsJson(planetsJson)
                .planetCount(planets.size())
                .build();

        SkySnapshot saved = repository.save(snapshot);
        log.info("Snapshot do dia {} salvo com {} satélites e {} planetas.",
                today, satellites.size(), planets.size());

        return saved;
    }

    @Override
    public SkySnapshotResponseDto getToday() {
        // TODO: será implementado na US de leitura
        throw new UnsupportedOperationException("Ainda não implementado");
    }

    @Override
    public List<SkySnapshotResponseDto> getHistory(int days) {
        // TODO: será implementado na US de leitura
        throw new UnsupportedOperationException("Ainda não implementado");
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new ExternalApiException("Erro ao serializar dados para JSON: " + e.getMessage());
        }
    }
}
