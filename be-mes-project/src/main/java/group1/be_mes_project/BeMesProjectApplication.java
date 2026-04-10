package group1.be_mes_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BeMesProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeMesProjectApplication.class, args);
	}

}
