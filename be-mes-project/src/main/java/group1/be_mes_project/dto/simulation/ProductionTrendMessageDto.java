package group1.be_mes_project.dto.simulation;

import java.time.LocalDateTime;

public record ProductionTrendMessageDto(
    String woId,
    Integer crTemp,
    Double tempSp,
    Double tempPv,
    Integer speed,
    LocalDateTime timestamp,
    Double progress) {}

