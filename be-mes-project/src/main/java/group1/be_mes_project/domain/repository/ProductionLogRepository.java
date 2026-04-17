package group1.be_mes_project.domain.repository;

import group1.be_mes_project.domain.entity.ProductionLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLogRepository extends JpaRepository<ProductionLog, Long> {

  List<ProductionLog> findByWorkOrder_WoIdOrderByTimestampAsc(String woId);

  long countByWorkOrder_WoId(String woId);
}

