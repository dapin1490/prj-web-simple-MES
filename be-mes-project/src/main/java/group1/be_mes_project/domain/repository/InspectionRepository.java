package group1.be_mes_project.domain.repository;

import group1.be_mes_project.domain.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionRepository extends JpaRepository<Inspection, String> {}

