package group1.be_mes_project.controller;

import group1.be_mes_project.dto.ApiResponse;
import group1.be_mes_project.dto.simulation.SimulationStatusDto;
import group1.be_mes_project.service.SimulationService;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class SimulationStompController {

  private final SimulationService simulationService;

  public SimulationStompController(SimulationService simulationService) {
    this.simulationService = simulationService;
  }

  @MessageMapping("/simulation/start")
  @SendToUser("/queue/simulation/state")
  public ApiResponse<SimulationStatusDto> startSimulation() {
    return ApiResponse.success(simulationService.start());
  }

  @MessageMapping("/simulation/stop")
  @SendToUser("/queue/simulation/state")
  public ApiResponse<SimulationStatusDto> stopSimulation() {
    return ApiResponse.success(simulationService.stop());
  }

  @MessageMapping("/simulation/reset")
  @SendToUser("/queue/simulation/state")
  public ApiResponse<SimulationStatusDto> resetSimulation() {
    return ApiResponse.success(simulationService.reset());
  }

  @MessageMapping("/simulation/state")
  @SendToUser("/queue/simulation/state")
  public ApiResponse<SimulationStatusDto> getSimulationStatus() {
    return ApiResponse.success(simulationService.getStatus("Current simulation status."));
  }

  @MessageExceptionHandler(Exception.class)
  @SendToUser("/queue/errors")
  public ApiResponse<Void> handleWebSocketException(Exception exception) {
    return ApiResponse.fail("Unexpected error: " + exception.getMessage());
  }
}

