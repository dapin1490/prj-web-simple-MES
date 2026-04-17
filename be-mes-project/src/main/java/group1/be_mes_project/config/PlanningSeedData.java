package group1.be_mes_project.config;

import group1.be_mes_project.domain.entity.ProductionLog;
import group1.be_mes_project.domain.entity.Product;
import group1.be_mes_project.domain.entity.SalesOrder;
import group1.be_mes_project.domain.entity.WorkOrder;
import group1.be_mes_project.domain.repository.ProductionLogRepository;
import group1.be_mes_project.domain.repository.ProductRepository;
import group1.be_mes_project.domain.repository.SalesOrderRepository;
import group1.be_mes_project.domain.repository.WorkOrderRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanningSeedData implements CommandLineRunner {

  private final ProductRepository productRepository;
  private final SalesOrderRepository salesOrderRepository;
  private final WorkOrderRepository workOrderRepository;
  private final ProductionLogRepository productionLogRepository;
  private final String datasourceUrl;

  public PlanningSeedData(
      ProductRepository productRepository,
      SalesOrderRepository salesOrderRepository,
      WorkOrderRepository workOrderRepository,
      ProductionLogRepository productionLogRepository,
      @Value("${spring.datasource.url:}") String datasourceUrl) {
    this.productRepository = productRepository;
    this.salesOrderRepository = salesOrderRepository;
    this.workOrderRepository = workOrderRepository;
    this.productionLogRepository = productionLogRepository;
    this.datasourceUrl = datasourceUrl;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (datasourceUrl == null || !datasourceUrl.startsWith("jdbc:h2:")) {
      return;
    }

    if (productRepository.count() > 0 || salesOrderRepository.count() > 0) {
      return;
    }

    Product product1 =
        productRepository.save(new Product("P-1001", "High Stretch Poly Fabric", "Dyeing", 120));
    Product product2 =
        productRepository.save(new Product("P-1002", "Cotton Blend Fabric", "Dyeing", 150));

    SalesOrder order1 =
        salesOrderRepository.save(new SalesOrder("SO-20220101-001", product1, LocalDate.of(2022, 1, 1), 400.0));
    SalesOrder order2 =
        salesOrderRepository.save(new SalesOrder("SO-20220102-001", product1, LocalDate.of(2022, 1, 2), 350.0));
    SalesOrder order3 =
        salesOrderRepository.save(new SalesOrder("SO-20220101-002", product2, LocalDate.of(2022, 1, 1), 500.0));

    WorkOrder wo1 =
        workOrderRepository.save(new WorkOrder("WO-220101-001", order1, 400.0, "M-01"));
    WorkOrder wo2 =
        workOrderRepository.save(new WorkOrder("WO-220102-001", order2, 350.0, "M-02"));
    WorkOrder wo3 =
        workOrderRepository.save(new WorkOrder("WO-220101-002", order3, 500.0, "M-03"));

    productionLogRepository.save(
        new ProductionLog(wo1, LocalDateTime.of(2022, 1, 1, 9, 0), 70, 70.0, 68.4, 62));
    productionLogRepository.save(
        new ProductionLog(wo1, LocalDateTime.of(2022, 1, 1, 9, 1), 70, 70.0, 69.1, 64));
    productionLogRepository.save(
        new ProductionLog(wo1, LocalDateTime.of(2022, 1, 1, 9, 2), 70, 70.0, 69.7, 65));

    productionLogRepository.save(
        new ProductionLog(wo2, LocalDateTime.of(2022, 1, 2, 10, 0), 69, 69.0, 67.8, 58));
    productionLogRepository.save(
        new ProductionLog(wo2, LocalDateTime.of(2022, 1, 2, 10, 1), 69, 69.0, 68.3, 60));

    productionLogRepository.save(
        new ProductionLog(wo3, LocalDateTime.of(2022, 1, 1, 11, 0), 71, 71.0, 70.2, 67));
  }
}

