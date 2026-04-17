package group1.be_mes_project.controller;

import group1.be_mes_project.dto.ApiResponse;
import group1.be_mes_project.dto.execution.ExecutionLogFilterDto;
import group1.be_mes_project.dto.execution.ProductionLogDto;
import group1.be_mes_project.dto.execution.ProductionProgressDto;
import group1.be_mes_project.dto.execution.WorkOrderDto;
import group1.be_mes_project.service.ExecutionService;
import java.util.List;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class ExecutionStompController {

  private final ExecutionService executionService;

  public ExecutionStompController(ExecutionService executionService) {
    this.executionService = executionService;
  }

  @MessageMapping("/execution/work-orders")
  @SendToUser("/queue/execution/work-orders")
  public ApiResponse<List<WorkOrderDto>> getWorkOrders() {
    return ApiResponse.success(executionService.getWorkOrders());
  }

  @MessageMapping("/execution/production/logs")
  @SendToUser("/queue/execution/production/logs")
  public ApiResponse<List<ProductionLogDto>> getProductionLogsByWoId(ExecutionLogFilterDto filter) {
    if (filter == null || filter.woId() == null || filter.woId().isBlank()) {
      return ApiResponse.fail("wo_id is required");
    }
    return ApiResponse.success(executionService.getProductionLogsByWoId(filter.woId()));
  }

  @MessageMapping("/execution/production/progress")
  @SendToUser("/queue/execution/production/progress")
  public ApiResponse<ProductionProgressDto> getProductionProgress() {
    return ApiResponse.success(executionService.getProductionProgress());
  }

  @MessageExceptionHandler(Exception.class)
  @SendToUser("/queue/errors")
  public ApiResponse<Void> handleWebSocketException(Exception exception) {
    return ApiResponse.fail("Unexpected error: " + exception.getMessage());
  }
}

