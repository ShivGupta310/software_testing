package ilpREST.ilp_submission_1.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EndpointConfig {
    @Bean
    public String ilpEndpoint() {
        return System.getenv().getOrDefault(
                "ILP_ENDPOINT",
                    "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net"
                );
    }
}
