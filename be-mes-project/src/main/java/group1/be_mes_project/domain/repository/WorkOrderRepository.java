package group1.be_mes_project.domain.repository;

import group1.be_mes_project.domain.entity.WorkOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, String> {

  List<WorkOrder> findAllByOrderByWoIdAsc();
}

