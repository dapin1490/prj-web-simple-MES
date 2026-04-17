package group1.be_mes_project.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Inspections")
public class Inspection {

  @Id
  @Column(name = "insp_id", nullable = false, length = 100)
  private String inspId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wo_id", nullable = false)
  private WorkOrder workOrder;

  @Column(name = "color_de", nullable = false)
  private Double colorDe;

  @Column(name = "pass_fail", nullable = false)
  private Boolean passFail;

  protected Inspection() {}

  public Inspection(String inspId, WorkOrder workOrder, Double colorDe, Boolean passFail) {
    this.inspId = inspId;
    this.workOrder = workOrder;
    this.colorDe = colorDe;
    this.passFail = passFail;
  }

  public String getInspId() {
    return inspId;
  }

  public WorkOrder getWorkOrder() {
    return workOrder;
  }

  public Double getColorDe() {
    return colorDe;
  }

  public Boolean getPassFail() {
    return passFail;
  }
}

