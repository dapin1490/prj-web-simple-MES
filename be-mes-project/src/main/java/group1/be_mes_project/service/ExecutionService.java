package group1.be_mes_project.service;

import group1.be_mes_project.dto.execution.ProductionLogDto;
import group1.be_mes_project.dto.execution.ProductionProgressDto;
import group1.be_mes_project.dto.execution.WorkOrderDto;
import java.util.List;

public interface ExecutionService {

  List<WorkOrderDto> getWorkOrders();

  List<ProductionLogDto> getProductionLogsByWoId(String woId);

  ProductionProgressDto getProductionProgress();
}

