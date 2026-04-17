package group1.be_mes_project.service.impl;

import group1.be_mes_project.domain.entity.Inspection;
import group1.be_mes_project.domain.entity.ProductionLog;
import group1.be_mes_project.domain.entity.WorkOrder;
import group1.be_mes_project.domain.repository.InspectionRepository;
import group1.be_mes_project.domain.repository.ProductionLogRepository;
import group1.be_mes_project.domain.repository.WorkOrderRepository;
import group1.be_mes_project.dto.execution.ProductionLogDto;
import group1.be_mes_project.dto.execution.WorkOrderDto;
import group1.be_mes_project.dto.quality.InspectionDto;
import group1.be_mes_project.dto.quality.ReportDto;
import group1.be_mes_project.service.QualityService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QualityServiceImpl implements QualityService {

  private final InspectionRepository inspectionRepository;
  private final WorkOrderRepository workOrderRepository;
  private final ProductionLogRepository productionLogRepository;

  public QualityServiceImpl(
      InspectionRepository inspectionRepository,
      WorkOrderRepository workOrderRepository,
      ProductionLogRepository productionLogRepository) {
    this.inspectionRepository = inspectionRepository;
    this.workOrderRepository = workOrderRepository;
    this.productionLogRepository = productionLogRepository;
  }

  @Override
  public List<InspectionDto> getInspections() {
    return inspectionRepository.findAll().stream()
        .map(this::toInspectionDto)
        .toList();
  }

  @Override
  public ReportDto getReportByWoId(String woId) {
    Optional<WorkOrder> workOrderOptional = workOrderRepository.findById(woId);
    if (workOrderOptional.isEmpty()) {
      return null;
    }

    WorkOrder workOrder = workOrderOptional.get();
    List<ProductionLogDto> productionLogs =
        productionLogRepository.findByWorkOrder_WoIdOrderByTimestampAsc(woId).stream()
            .map(this::toProductionLogDto)
            .toList();

    InspectionDto inspection =
        inspectionRepository.findAll().stream()
            .filter(each -> each.getWorkOrder().getWoId().equals(woId))
            .findFirst()
            .map(this::toInspectionDto)
            .orElse(null);

    WorkOrderDto workOrderDto =
        new WorkOrderDto(
            workOrder.getWoId(),
            workOrder.getSalesOrder().getOrderId(),
            workOrder.getPlannedQty(),
            workOrder.getMachineId());

    return new ReportDto(workOrderDto, productionLogs, inspection);
  }

  private InspectionDto toInspectionDto(Inspection inspection) {
    return new InspectionDto(
        inspection.getInspId(),
        inspection.getWorkOrder().getWoId(),
        inspection.getColorDe(),
        inspection.getPassFail());
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
}

