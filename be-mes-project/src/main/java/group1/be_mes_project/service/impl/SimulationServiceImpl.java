package group1.be_mes_project.service.impl;

import group1.be_mes_project.domain.entity.ProductionLog;
import group1.be_mes_project.domain.entity.WorkOrder;
import group1.be_mes_project.domain.repository.ProductionLogRepository;
import group1.be_mes_project.domain.repository.WorkOrderRepository;
import group1.be_mes_project.dto.simulation.ProductionTrendMessageDto;
import group1.be_mes_project.dto.simulation.SimulationStatusDto;
import group1.be_mes_project.service.SimulationService;
import group1.be_mes_project.simulation.SimulationRuntimeState;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class SimulationServiceImpl implements SimulationService {

  private static final String TREND_TOPIC = "/topic/production-trend";
  private static final DateTimeFormatter CSV_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final SimulationRuntimeState runtimeState;
  private final WorkOrderRepository workOrderRepository;
  private final ProductionLogRepository productionLogRepository;
  private final SimpMessagingTemplate messagingTemplate;

  private final String simulationSourcePath;
  private final List<SimulationRow> simulationRows = new ArrayList<>();

  public SimulationServiceImpl(
      SimulationRuntimeState runtimeState,
      WorkOrderRepository workOrderRepository,
      ProductionLogRepository productionLogRepository,
      SimpMessagingTemplate messagingTemplate,
      @Value("${mes.simulation.source-path:}") String simulationSourcePath) {
    this.runtimeState = runtimeState;
    this.workOrderRepository = workOrderRepository;
    this.productionLogRepository = productionLogRepository;
    this.messagingTemplate = messagingTemplate;
    this.simulationSourcePath = simulationSourcePath;
  }

  @PostConstruct
  void initRows() {
    loadRowsFromClasspath(simulationSourcePath);
  }

  @Override
  public SimulationStatusDto start() {
    if (simulationRows.isEmpty()) {
      return getStatus("No simulation rows loaded.");
    }
    runtimeState.start();
    return getStatus("Simulation started.");
  }

  @Override
  public SimulationStatusDto stop() {
    runtimeState.stop();
    return getStatus("Simulation stopped.");
  }

  @Override
  public SimulationStatusDto reset() {
    runtimeState.stop();
    runtimeState.resetPointer();
    productionLogRepository.deleteAll();
    return getStatus("Simulation reset.");
  }

  @Override
  public SimulationStatusDto getStatus(String message) {
    return new SimulationStatusDto(
        runtimeState.isRunning(),
        runtimeState.getPointer(),
        simulationRows.size(),
        message);
  }

  @Scheduled(fixedDelayString = "${mes.simulation.interval-ms:60000}")
  public void tick() {
    if (!runtimeState.isRunning()) {
      return;
    }

    int pointer = runtimeState.getPointer();
    if (pointer >= simulationRows.size()) {
      runtimeState.stop();
      return;
    }

    SimulationRow row = simulationRows.get(pointer);
    Optional<WorkOrder> workOrderOptional = workOrderRepository.findById(row.woId());
    runtimeState.incrementPointer();

    if (workOrderOptional.isEmpty()) {
      return;
    }

    WorkOrder workOrder = workOrderOptional.get();
    ProductionLog savedLog =
        productionLogRepository.save(
            new ProductionLog(
                row.logId(),
                workOrder,
                row.timestamp(),
                row.crTemp(),
                row.tempSp(),
                row.tempPv(),
                row.speed()));

    double progress = calculateProgress(workOrder);
    messagingTemplate.convertAndSend(
        TREND_TOPIC,
        new ProductionTrendMessageDto(
            workOrder.getWoId(),
            savedLog.getCrTemp(),
            savedLog.getTempSp(),
            savedLog.getTempPv(),
            savedLog.getSpeed(),
            savedLog.getTimestamp(),
            progress));

    if (runtimeState.getPointer() >= simulationRows.size()) {
      runtimeState.stop();
    }
  }

  private void loadRowsFromClasspath(String configuredPath) {
    simulationRows.clear();

    String effectivePath =
        (configuredPath == null || configuredPath.isBlank())
            ? "data/ProductionLogs.csv"
            : configuredPath;

    try (InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(effectivePath);
        BufferedReader reader =
            inputStream == null
                ? null
                : new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

      if (reader == null) {
        return;
      }

      String header = reader.readLine();
      if (header == null) {
        return;
      }

      Map<String, Integer> headerMap = buildHeaderMap(header);

      String line;
      while ((line = reader.readLine()) != null) {
        String[] columns = line.split(",", -1);

        Long logId = parseLong(getColumn(columns, headerMap, "log_id"));
        String woId = getColumn(columns, headerMap, "wo_id");
        LocalDateTime timestamp = parseDateTime(getColumn(columns, headerMap, "timestamp"));
        Integer crTemp = parseInteger(getColumn(columns, headerMap, "cr_temp"));
        Double tempSp = parseDouble(getColumn(columns, headerMap, "temp_sp"));
        Double tempPv = parseDouble(getColumn(columns, headerMap, "temp_pv"));
        Integer speed = parseInteger(getColumn(columns, headerMap, "speed"));

        if (logId == null
            || woId == null
            || timestamp == null
            || crTemp == null
            || tempSp == null
            || tempPv == null
            || speed == null) {
          continue;
        }

        simulationRows.add(new SimulationRow(logId, woId, timestamp, crTemp, tempSp, tempPv, speed));
      }
    } catch (Exception exception) {
      simulationRows.clear();
    }
  }

  private Map<String, Integer> buildHeaderMap(String headerLine) {
    String[] headers = headerLine.split(",", -1);
    Map<String, Integer> headerMap = new HashMap<>();
    for (int index = 0; index < headers.length; index++) {
      headerMap.put(headers[index].trim().toLowerCase(), index);
    }
    return headerMap;
  }

  private String getColumn(String[] columns, Map<String, Integer> headerMap, String key) {
    Integer index = headerMap.get(key.toLowerCase());
    if (index == null || index < 0 || index >= columns.length) {
      return null;
    }
    String value = columns[index].trim();
    return value.isEmpty() ? null : value;
  }

  private Integer parseInteger(String value) {
    try {
      return value == null ? null : Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private Long parseLong(String value) {
    try {
      return value == null ? null : Long.parseLong(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private Double parseDouble(String value) {
    try {
      return value == null ? null : Double.parseDouble(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private LocalDateTime parseDateTime(String value) {
    if (value == null) {
      return null;
    }

    try {
      return LocalDateTime.parse(value, CSV_TIMESTAMP_FORMATTER);
    } catch (Exception exception) {
      try {
        return LocalDateTime.parse(value);
      } catch (Exception ignored) {
        return null;
      }
    }
  }

  private double calculateProgress(WorkOrder workOrder) {
    double plannedQty = workOrder.getPlannedQty() == null ? 0.0 : workOrder.getPlannedQty();
    if (plannedQty <= 0.0) {
      return 0.0;
    }

    long loadedCount = productionLogRepository.countByWorkOrder_WoId(workOrder.getWoId());
    return Math.round(Math.min(100.0, (loadedCount / plannedQty) * 100.0) * 10.0) / 10.0;
  }

  private record SimulationRow(
      Long logId,
      String woId,
      LocalDateTime timestamp,
      Integer crTemp,
      Double tempSp,
      Double tempPv,
      Integer speed) {}
}

