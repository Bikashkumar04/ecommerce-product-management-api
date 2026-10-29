package com.bikash.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"jwt.secret=VGhpcyBpcyBhIHRlc3Qgc2VjcmV0IGtleSB0aGF0IGlzIGxvbmcgZW5vdWdo",
		"spring.datasource.url=jdbc:h2:mem:ecommerce;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.flyway.enabled=false",
		"jwt.expiration-ms=3600000"
})
class EcommerceProductManagementApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
