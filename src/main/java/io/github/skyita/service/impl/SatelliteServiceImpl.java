package io.github.skyita.service.impl;

import io.github.skyita.dto.response.SatelliteResponseDto;
import io.github.skyita.exception.ExternalApiException;
import io.github.skyita.service.SatelliteService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SatelliteServiceImpl implements SatelliteService {

    @Value("${app.apis.n2yo.base-url}")
    private String baseUrl;

    @Value("${app.apis.n2yo.api-key}")
    private String apiKey;

    private final WebClient.Builder webClientBuilder;


    @Override
    public List<SatelliteResponseDto> findAbove(double lat, double lon, double altMeters) {
        String url = "%s/above/%.4f/%.4f/%.0f/70/0/&apiKey=%s"
            .formatted(baseUrl, lat, lon, altMeters, apiKey);

        log.info("Buscando satélites: lat={}, lon={}", lat, lon);

        Map<String, Object> response = webClientBuilder
            .build()
            .get()
            .uri(url)
            .retrieve()
            .onStatus(HttpStatusCode::isError, res ->
                res.bodyToMono(String.class)
                   .flatMap(body -> Mono.error(
                       new ExternalApiException("N2YO erro: " + body))))

            .bodyToMono(new ParameterizedTypeReference<
                            Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(10))
            .doOnError(e -> log.error("Erro N2YO: {}", e.getMessage()))
            .block();

        if (response == null || !response.containsKey("above")) {
            log.warn("Resposta N2YO vazia ou sem campo above");
            return List.of();
        }

         @SuppressWarnings("unchecked")
        List<Map<String, Object>> above =
            (List<Map<String, Object>>) response.get("above");


        return above.stream()
            .map(this::mapToDTO)
            .toList(); // lista imutável (Java 16+)
    }

    private SatelliteResponseDto mapToDTO(Map<String, Object> sat) {
        return new SatelliteResponseDto(
            (Integer) sat.get("satid"),
            (String)  sat.get("satname"),
            (String)  sat.get("launchDate"),
            ((Number) sat.get("satlat")).doubleValue(),
            ((Number) sat.get("satlng")).doubleValue(),
            ((Number) sat.get("satalt")).doubleValue()
        );
    }
}
