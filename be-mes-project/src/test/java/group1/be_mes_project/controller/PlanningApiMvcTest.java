package group1.be_mes_project.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group1.be_mes_project.domain.entity.Product;
import group1.be_mes_project.domain.entity.SalesOrder;
import group1.be_mes_project.domain.repository.ProductRepository;
import group1.be_mes_project.domain.repository.SalesOrderRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "mes.seed.csv.enabled=false")
@AutoConfigureMockMvc
class PlanningApiMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ProductRepository productRepository;

  @Autowired private SalesOrderRepository salesOrderRepository;

  @BeforeEach
  void setUp() {
    salesOrderRepository.deleteAll();
    productRepository.deleteAll();

    Product productA =
        productRepository.save(new Product("P-1001", "High Stretch Poly Fabric", "Dyeing", 120));
    Product productB =
        productRepository.save(new Product("P-1002", "Cotton Blend Fabric", "Dyeing", 150));

    salesOrderRepository.save(
        new SalesOrder("SO-20220101-001", productA, LocalDate.of(2022, 1, 1), 400.0));
    salesOrderRepository.save(
        new SalesOrder("SO-20220102-001", productA, LocalDate.of(2022, 1, 2), 350.0));
    salesOrderRepository.save(
        new SalesOrder("SO-20220101-002", productB, LocalDate.of(2022, 1, 1), 500.0));
  }

  @Test
  void getProductsReturnsEnvelopeAndList() throws Exception {
    mockMvc
        .perform(get("/api/v1/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data", hasSize(2)))
        .andExpect(jsonPath("$.data[0].product_id").exists())
        .andExpect(jsonPath("$.data[0].safety_stock").exists());
  }

  @Test
  void getOrdersReturnsEnvelopeAndList() throws Exception {
    mockMvc
        .perform(get("/api/v1/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data", hasSize(3)))
        .andExpect(jsonPath("$.data[0].order_id").exists())
        .andExpect(jsonPath("$.data[0].product_id").exists())
        .andExpect(jsonPath("$.data[0].order_qty").exists());
  }

  @Test
  void getOrdersByProductIdReturnsFilteredResult() throws Exception {
    mockMvc
        .perform(get("/api/v1/orders/{product_id}", "P-1001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data", hasSize(2)))
        .andExpect(jsonPath("$.data[0].product_id").value("P-1001"))
        .andExpect(jsonPath("$.data[1].product_id").value("P-1001"));
  }
}

