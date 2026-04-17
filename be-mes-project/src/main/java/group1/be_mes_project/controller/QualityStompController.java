package group1.be_mes_project.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import group1.be_mes_project.dto.ApiResponse;
import group1.be_mes_project.dto.quality.InspectionDto;
import group1.be_mes_project.dto.quality.ReportDto;
import group1.be_mes_project.service.QualityService;
import java.util.List;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class QualityStompController {

  private final QualityService qualityService;

  public QualityStompController(QualityService qualityService) {
    this.qualityService = qualityService;
  }

  @MessageMapping("/quality/inspections")
  @SendToUser("/queue/quality/inspections")
  public ApiResponse<List<InspectionDto>> getInspections() {
    List<InspectionDto> inspections = qualityService.getInspections();
    return ApiResponse.success(inspections);
  }

  @MessageMapping("/quality/reports")
  @SendToUser("/queue/quality/reports")
  public ApiResponse<ReportDto> getReport(@Payload QualityReportRequest request) {
    if (request == null || request.getWoId() == null) {
      return ApiResponse.fail("wo_id is required");
    }
    ReportDto report = qualityService.getReportByWoId(request.getWoId());
    if (report == null) {
      return ApiResponse.fail("Report not found for wo_id: " + request.getWoId());
    }
    return ApiResponse.success(report);
  }

  @MessageExceptionHandler(Exception.class)
  @SendToUser("/queue/errors")
  public ApiResponse<Void> handleWebSocketException(Exception exception) {
    return ApiResponse.fail("Unexpected error: " + exception.getMessage());
  }

  /**
   * 요청 바디에서 wo_id를 받기 위한 간단한 DTO
   */
  public static class QualityReportRequest {
    @JsonProperty("wo_id")
    private String woId;

    public String getWoId() {
      return woId;
    }

    public void setWoId(String woId) {
      this.woId = woId;
    }
  }
}

