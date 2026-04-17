package group1.be_mes_project.service.impl;

import group1.be_mes_project.dto.simulation.EquipmentAlertDto;
import group1.be_mes_project.service.EquipmentAlertService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class EquipmentAlertServiceImpl implements EquipmentAlertService {

  // 이상 감지 임계값
  private static final double TEMP_DIFF_THRESHOLD = 5.0; // |temp_sp - temp_pv| > 5
  private static final double TEMP_HIGH_THRESHOLD = 10.0; // temp_pv > temp_sp + 10
  private static final double TEMP_LOW_THRESHOLD = 10.0; // temp_pv < temp_sp - 10
  private static final int SPEED_LOW_THRESHOLD = 10; // speed < 10

  @Override
  public EquipmentAlertDto detectAndCreateAlert(
      String machineId, String woId, Double tempSp, Double tempPv, Integer speed) {
    
    // 온도 지시값과 실측값의 차이 확인
    if (tempSp != null && tempPv != null) {
      double tempDiff = Math.abs(tempSp - tempPv);
      
      // 온도가 너무 높음
      if (tempPv > tempSp + TEMP_HIGH_THRESHOLD) {
        return new EquipmentAlertDto(
            machineId,
            woId,
            "TEMP_HIGH",
            String.format(
                "설비 %s 이상 감지: 온도 높음 (지시: %.1f°C, 실측: %.1f°C). 확인 요망.",
                machineId, tempSp, tempPv),
            LocalDateTime.now());
      }
      
      // 온도가 너무 낮음
      if (tempPv < tempSp - TEMP_LOW_THRESHOLD) {
        return new EquipmentAlertDto(
            machineId,
            woId,
            "TEMP_LOW",
            String.format(
                "설비 %s 이상 감지: 온도 낮음 (지시: %.1f°C, 실측: %.1f°C). 확인 요망.",
                machineId, tempSp, tempPv),
            LocalDateTime.now());
      }
      
      // 온도 지시값과 실측값 불일치 (HIGH/LOW 범위 내에서)
      if (tempDiff > TEMP_DIFF_THRESHOLD) {
        return new EquipmentAlertDto(
            machineId,
            woId,
            "TEMP_MISMATCH",
            String.format(
                "설비 %s 주의: 온도 편차 (지시: %.1f°C, 실측: %.1f°C, 편차: %.1f°C). 확인 요망.",
                machineId, tempSp, tempPv, tempDiff),
            LocalDateTime.now());
      }
    }
    
    // 설비 속도 확인
    if (speed != null && speed < SPEED_LOW_THRESHOLD) {
      return new EquipmentAlertDto(
          machineId,
          woId,
          "SPEED_LOW",
          String.format("설비 %s 이상 감지: 속도 낮음 (현재: %d). 확인 요망.", machineId, speed),
          LocalDateTime.now());
    }
    
    return null;
  }
}

