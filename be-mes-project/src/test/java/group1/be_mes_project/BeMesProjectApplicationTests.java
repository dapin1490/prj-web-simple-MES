package group1.be_mes_project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "mes.seed.csv.enabled=false")
class BeMesProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
