package group1.be_mes_project.service;

import group1.be_mes_project.dto.quality.InspectionDto;
import group1.be_mes_project.dto.quality.ReportDto;
import java.util.List;

public interface QualityService {

  /**
   * 모든 품질 검사 결과 조회
   */
  List<InspectionDto> getInspections();

  /**
   * 특정 로트(WorkOrder)의 공정 보고서 데이터 조회
   *
   * @param woId 작업 지시 ID
   * @return 보고서 데이터
   */
  ReportDto getReportByWoId(String woId);
}

