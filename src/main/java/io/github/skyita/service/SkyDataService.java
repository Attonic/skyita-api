package io.github.skyita.service;

import io.github.skyita.dto.response.SkySnapshotResponseDto;
import io.github.skyita.entity.SkySnapshot;

import java.util.List;

public interface SkyDataService {

    SkySnapshot fetchAndSave();

    SkySnapshotResponseDto getToday();

    List<SkySnapshotResponseDto> getHistory(int days);

}
