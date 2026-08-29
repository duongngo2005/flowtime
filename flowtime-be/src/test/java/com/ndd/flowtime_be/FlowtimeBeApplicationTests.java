package com.ndd.flowtime_be;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-client-secret",
        "spring.ai.google.genai.api-key=test-gemini-api-key",
        "app.jwt.secret=test-jwt-secret-that-is-long-enough-for-hmac-signing"
})
class FlowtimeBeApplicationTests {

	@Test
	void contextLoads() {
	}

}
