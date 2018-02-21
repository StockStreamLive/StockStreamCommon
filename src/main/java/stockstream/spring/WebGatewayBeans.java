package stockstream.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stockstream.api.StockStreamAPI;
import stockstream.http.HTTPClient;

@Configuration
public class WebGatewayBeans {

    @Bean
    public StockStreamAPI stockStreamAPI() {
        return new StockStreamAPI();
    }

    @Bean
    public HTTPClient httpClient() {
        return new HTTPClient("TEST".equalsIgnoreCase(System.getenv("stage")));
    }
}
