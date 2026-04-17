package group1.be_mes_project.service;

import group1.be_mes_project.dto.simulation.SimulationStatusDto;

public interface SimulationService {

  SimulationStatusDto start();

  SimulationStatusDto stop();

  SimulationStatusDto reset();

  SimulationStatusDto getStatus(String message);
}

