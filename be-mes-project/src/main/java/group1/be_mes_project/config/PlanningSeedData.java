package group1.be_mes_project.config;

import group1.be_mes_project.domain.entity.Inspection;
import group1.be_mes_project.domain.entity.ProductionLog;
import group1.be_mes_project.domain.entity.Product;
import group1.be_mes_project.domain.entity.SalesOrder;
import group1.be_mes_project.domain.entity.WorkOrder;
import group1.be_mes_project.domain.repository.InspectionRepository;
import group1.be_mes_project.domain.repository.ProductionLogRepository;
import group1.be_mes_project.domain.repository.ProductRepository;
import group1.be_mes_project.domain.repository.SalesOrderRepository;
import group1.be_mes_project.domain.repository.WorkOrderRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlanningSeedData implements CommandLineRunner {

  private static final DateTimeFormatter CSV_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ProductRepository productRepository;
  private final SalesOrderRepository salesOrderRepository;
  private final WorkOrderRepository workOrderRepository;
  private final ProductionLogRepository productionLogRepository;
  private final InspectionRepository inspectionRepository;
  private final boolean csvSeedEnabled;

  public PlanningSeedData(
      ProductRepository productRepository,
      SalesOrderRepository salesOrderRepository,
      WorkOrderRepository workOrderRepository,
      ProductionLogRepository productionLogRepository,
      InspectionRepository inspectionRepository,
      @Value("${mes.seed.csv.enabled:true}") boolean csvSeedEnabled) {
    this.productRepository = productRepository;
    this.salesOrderRepository = salesOrderRepository;
    this.workOrderRepository = workOrderRepository;
    this.productionLogRepository = productionLogRepository;
    this.inspectionRepository = inspectionRepository;
    this.csvSeedEnabled = csvSeedEnabled;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!csvSeedEnabled) {
      return;
    }

    if (productRepository.count() > 0) {
      return;
    }

    loadProducts();
    loadSalesOrders();
    loadWorkOrders();
    loadProductionLogs();
    loadInspections();
  }

  private void loadProducts() {
    readCsvRows(
        "data/Products.csv",
        (headerMap, columns) -> {
          String productId = getColumn(columns, headerMap, "product_id");
          String name = getColumn(columns, headerMap, "name");
          String category = getColumn(columns, headerMap, "category");
          Integer safetyStock = parseInteger(getColumn(columns, headerMap, "safety_stock"));

          if (productId != null && name != null && safetyStock != null) {
            productRepository.save(new Product(productId, name, category, safetyStock));
          }
        });
  }

  private void loadSalesOrders() {
    Map<String, Product> productsById =
        productRepository.findAll().stream()
            .collect(HashMap::new, (map, product) -> map.put(product.getProductId(), product), HashMap::putAll);

    readCsvRows(
        "data/SalesOrders.csv",
        (headerMap, columns) -> {
          String orderId = getColumn(columns, headerMap, "order_id");
          String productId = getColumn(columns, headerMap, "product_id");
          LocalDate orderDate = parseDate(getColumn(columns, headerMap, "order_date"));
          Double orderQty = parseDouble(getColumn(columns, headerMap, "order_qty"));

          if (orderId == null || productId == null || orderDate == null || orderQty == null) {
            return;
          }

          Product product = productsById.get(productId);
          if (product != null) {
            salesOrderRepository.save(new SalesOrder(orderId, product, orderDate, orderQty));
          }
        });
  }

  private void loadWorkOrders() {
    Map<String, SalesOrder> ordersById =
        salesOrderRepository.findAll().stream()
            .collect(HashMap::new, (map, order) -> map.put(order.getOrderId(), order), HashMap::putAll);

    readCsvRows(
        "data/WorkOrders.csv",
        (headerMap, columns) -> {
          String woId = getColumn(columns, headerMap, "wo_id");
          String orderId = getColumn(columns, headerMap, "order_id");
          Double plannedQty = parseDouble(getColumn(columns, headerMap, "planned_qty"));
          String machineId = getColumn(columns, headerMap, "machine_id");

          if (woId == null || orderId == null || plannedQty == null) {
            return;
          }

          SalesOrder order = ordersById.get(orderId);
          if (order != null) {
            workOrderRepository.save(new WorkOrder(woId, order, plannedQty, machineId));
          }
        });
  }

  private void loadProductionLogs() {
    Map<String, WorkOrder> workOrdersById =
        workOrderRepository.findAll().stream()
            .collect(HashMap::new, (map, workOrder) -> map.put(workOrder.getWoId(), workOrder), HashMap::putAll);
    List<ProductionLog> batch = new ArrayList<>();
    final int batchSize = 1000;

    readCsvRows(
        "data/ProductionLogs.csv",
        (headerMap, columns) -> {
          Long logId = parseLong(getColumn(columns, headerMap, "log_id"));
          String woId = getColumn(columns, headerMap, "wo_id");
          LocalDateTime timestamp = parseDateTime(getColumn(columns, headerMap, "timestamp"));
          Integer crTemp = parseInteger(getColumn(columns, headerMap, "cr_temp"));
          Double tempSp = parseDouble(getColumn(columns, headerMap, "temp_sp"));
          Double tempPv = parseDouble(getColumn(columns, headerMap, "temp_pv"));
          Integer speed = parseInteger(getColumn(columns, headerMap, "speed"));

          if (logId == null
              || woId == null
              || timestamp == null
              || crTemp == null
              || tempSp == null
              || tempPv == null
              || speed == null) {
            return;
          }

          WorkOrder workOrder = workOrdersById.get(woId);
          if (workOrder == null) {
            return;
          }

          batch.add(new ProductionLog(logId, workOrder, timestamp, crTemp, tempSp, tempPv, speed));
          if (batch.size() >= batchSize) {
            productionLogRepository.saveAll(batch);
            batch.clear();
          }
        });

    if (!batch.isEmpty()) {
      productionLogRepository.saveAll(batch);
    }
  }

  private void loadInspections() {
    Map<String, WorkOrder> workOrdersById =
        workOrderRepository.findAll().stream()
            .collect(HashMap::new, (map, workOrder) -> map.put(workOrder.getWoId(), workOrder), HashMap::putAll);

    readCsvRows(
        "data/Inspections.csv",
        (headerMap, columns) -> {
          String inspId = getColumn(columns, headerMap, "insp_id");
          String woId = getColumn(columns, headerMap, "wo_id");
          Double colorDe = parseDouble(getColumn(columns, headerMap, "color_de"));
          Boolean passFail = parseBoolean(getColumn(columns, headerMap, "pass_fail"));

          if (inspId == null || woId == null || colorDe == null || passFail == null) {
            return;
          }

          WorkOrder workOrder = workOrdersById.get(woId);
          if (workOrder != null) {
            inspectionRepository.save(new Inspection(inspId, workOrder, colorDe, passFail));
          }
        });
  }

  private void readCsvRows(String classpathPath, CsvRowConsumer consumer) {
    try (InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathPath);
        BufferedReader reader =
            inputStream == null
                ? null
                : new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

      if (reader == null) {
        return;
      }

      String headerLine = reader.readLine();
      if (headerLine == null) {
        return;
      }

      Map<String, Integer> headerMap = buildHeaderMap(headerLine);
      String line;
      while ((line = reader.readLine()) != null) {
        String[] columns = line.split(",", -1);
        consumer.accept(headerMap, columns);
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read CSV: " + classpathPath, exception);
    }
  }

  private Map<String, Integer> buildHeaderMap(String headerLine) {
    String[] headers = headerLine.split(",", -1);
    Map<String, Integer> headerMap = new HashMap<>();
    for (int index = 0; index < headers.length; index++) {
      headerMap.put(headers[index].trim().toLowerCase(), index);
    }
    return headerMap;
  }

  private String getColumn(String[] columns, Map<String, Integer> headerMap, String key) {
    Integer index = headerMap.get(key.toLowerCase());
    if (index == null || index < 0 || index >= columns.length) {
      return null;
    }
    String value = columns[index].trim();
    return value.isEmpty() ? null : value;
  }

  private Integer parseInteger(String value) {
    try {
      return value == null ? null : Integer.parseInt(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private Long parseLong(String value) {
    try {
      return value == null ? null : Long.parseLong(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private Double parseDouble(String value) {
    try {
      return value == null ? null : Double.parseDouble(value);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private LocalDate parseDate(String value) {
    try {
      return value == null ? null : LocalDate.parse(value);
    } catch (Exception exception) {
      return null;
    }
  }

  private LocalDateTime parseDateTime(String value) {
    if (value == null) {
      return null;
    }

    try {
      return LocalDateTime.parse(value, CSV_TIMESTAMP_FORMATTER);
    } catch (Exception exception) {
      try {
        return LocalDateTime.parse(value);
      } catch (Exception ignored) {
        return null;
      }
    }
  }

  private Boolean parseBoolean(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim().toLowerCase();
    if (Arrays.asList("true", "1", "y", "yes").contains(normalized)) {
      return true;
    }
    if (Arrays.asList("false", "0", "n", "no").contains(normalized)) {
      return false;
    }
    return null;
  }

  @FunctionalInterface
  private interface CsvRowConsumer {
    void accept(Map<String, Integer> headerMap, String[] columns);
  }
}

