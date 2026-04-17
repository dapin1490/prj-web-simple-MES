package group1.be_mes_project.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ProductionLogs")
public class ProductionLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "log_id")
  private Long logId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wo_id", nullable = false)
  private WorkOrder workOrder;

  @Column(name = "timestamp", nullable = false)
  private LocalDateTime timestamp;

  @Column(name = "cr_temp", nullable = false)
  private Integer crTemp;

  @Column(name = "temp_sp", nullable = false)
  private Double tempSp;

  @Column(name = "temp_pv", nullable = false)
  private Double tempPv;

  @Column(name = "speed", nullable = false)
  private Integer speed;

  protected ProductionLog() {}

  public ProductionLog(
      WorkOrder workOrder,
      LocalDateTime timestamp,
      Integer crTemp,
      Double tempSp,
      Double tempPv,
      Integer speed) {
    this.workOrder = workOrder;
    this.timestamp = timestamp;
    this.crTemp = crTemp;
    this.tempSp = tempSp;
    this.tempPv = tempPv;
    this.speed = speed;
  }

  public Long getLogId() {
    return logId;
  }

  public WorkOrder getWorkOrder() {
    return workOrder;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public Integer getCrTemp() {
    return crTemp;
  }

  public Double getTempSp() {
    return tempSp;
  }

  public Double getTempPv() {
    return tempPv;
  }

  public Integer getSpeed() {
    return speed;
  }
}

