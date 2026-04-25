package io.github.skyita.dto.response;

public record PlanetResponse(
        String name, // Nome em português: Mercúrio, Vênus, Marte...
        double aximuthDegrees, // Direção no horizonte:
        double altitudeDegress, // Altura acima do horizonte em graus
        double visible
) {
}
