package group1.be_mes_project.service.impl;

import group1.be_mes_project.domain.entity.ProductionLog;
import group1.be_mes_project.domain.entity.WorkOrder;
import group1.be_mes_project.domain.repository.ProductionLogRepository;
import group1.be_mes_project.domain.repository.WorkOrderRepository;
import group1.be_mes_project.dto.execution.ProductionLogDto;
import group1.be_mes_project.dto.execution.ProductionProgressDto;
import group1.be_mes_project.dto.execution.WorkOrderDto;
import group1.be_mes_project.dto.execution.WorkOrderProgressDto;
import group1.be_mes_project.service.ExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExecutionServiceImpl implements ExecutionService {

  private final WorkOrderRepository workOrderRepository;
  private final ProductionLogRepository productionLogRepository;

  public ExecutionServiceImpl(
      WorkOrderRepository workOrderRepository, ProductionLogRepository productionLogRepository) {
    this.workOrderRepository = workOrderRepository;
    this.productionLogRepository = productionLogRepository;
  }

  @Override
  public List<WorkOrderDto> getWorkOrders() {
    return workOrderRepository.findAllByOrderByWoIdAsc().stream().map(this::toWorkOrderDto).toList();
  }

  @Override
  public List<ProductionLogDto> getProductionLogsByWoId(String woId) {
    return productionLogRepository.findByWorkOrder_WoIdOrderByTimestampAsc(woId).stream()
        .map(this::toProductionLogDto)
        .toList();
  }

  @Override
  public ProductionProgressDto getProductionProgress() {
    List<WorkOrderProgressDto> workOrderProgresses =
        workOrderRepository.findAllByOrderByWoIdAsc().stream().map(this::toWorkOrderProgress).toList();

    double total = workOrderProgresses.stream().mapToDouble(WorkOrderProgressDto::progress).sum();
    double overallProgress = workOrderProgresses.isEmpty() ? 0.0 : round(total / workOrderProgresses.size());

    return new ProductionProgressDto(overallProgress, workOrderProgresses);
  }

  private WorkOrderDto toWorkOrderDto(WorkOrder workOrder) {
    return new WorkOrderDto(
        workOrder.getWoId(),
        workOrder.getSalesOrder().getOrderId(),
        workOrder.getPlannedQty(),
        workOrder.getMachineId());
  }

  private ProductionLogDto toProductionLogDto(ProductionLog log) {
    return new ProductionLogDto(
        log.getLogId(),
        log.getWorkOrder().getWoId(),
        log.getTimestamp(),
        log.getCrTemp(),
        log.getTempSp(),
        log.getTempPv(),
        log.getSpeed());
  }

  private WorkOrderProgressDto toWorkOrderProgress(WorkOrder workOrder) {
    double plannedQty = workOrder.getPlannedQty() == null ? 0.0 : workOrder.getPlannedQty();
    if (plannedQty <= 0.0) {
      return new WorkOrderProgressDto(workOrder.getWoId(), 0.0);
    }

    long currentLoaded = productionLogRepository.countByWorkOrder_WoId(workOrder.getWoId());
    double progress = Math.min(100.0, (currentLoaded / plannedQty) * 100.0);
    return new WorkOrderProgressDto(workOrder.getWoId(), round(progress));
  }

  private double round(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}

