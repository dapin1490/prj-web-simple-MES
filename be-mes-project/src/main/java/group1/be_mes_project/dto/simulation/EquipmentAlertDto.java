package group1.be_mes_project.dto.simulation;

import java.time.LocalDateTime;

public record EquipmentAlertDto(
    String machineId,
    String woId,
    String alertType,
    String message,
    LocalDateTime timestamp) {}

