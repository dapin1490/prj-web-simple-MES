package group1.be_mes_project.simulation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class SimulationRuntimeState {

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicInteger pointer = new AtomicInteger(0);

  public boolean isRunning() {
    return running.get();
  }

  public void start() {
    running.set(true);
  }

  public void stop() {
    running.set(false);
  }

  public int getPointer() {
    return pointer.get();
  }

  public int incrementPointer() {
    return pointer.incrementAndGet();
  }

  public void resetPointer() {
    pointer.set(0);
  }
}

