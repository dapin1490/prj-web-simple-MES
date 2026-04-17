package group1.be_mes_project.dto.execution;

import java.time.LocalDateTime;

public record ProductionLogDto(
    Long logId,
    String woId,
    LocalDateTime timestamp,
    Integer crTemp,
    Double tempSp,
    Double tempPv,
    Integer speed) {}

