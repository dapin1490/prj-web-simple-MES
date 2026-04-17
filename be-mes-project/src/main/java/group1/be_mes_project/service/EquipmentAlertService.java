package group1.be_mes_project.service;

import group1.be_mes_project.dto.simulation.EquipmentAlertDto;

public interface EquipmentAlertService {

  /**
   * 설비 이상 감지 및 알림 생성
   *
   * @param machineId 설비 ID
   * @param woId 작업 지시 ID
   * @param tempSp 지시 온도
   * @param tempPv 실측 온도
   * @param speed 설비 속도
   * @return 감지된 이상 알림 (없으면 null)
   */
  EquipmentAlertDto detectAndCreateAlert(
      String machineId, String woId, Double tempSp, Double tempPv, Integer speed);
}

