package stockstream.spring;

import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.client.RobinhoodClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import stockstream.cache.BrokerCache;
import stockstream.cache.InstrumentCache;

@Import({ComputerBeans.class})
@Configuration
public class CommonCacheBeans {

    @Bean
    public RobinhoodAPI robinhoodAPI() {
        return new RobinhoodClient(System.getenv("ROBINHOOD_USERNAME"), System.getenv("ROBINHOOD_PASSWORD"));
    }

    @Bean
    public BrokerCache brokerCache() {
        return new BrokerCache();
    }

    @Bean
    public InstrumentCache instrumentCache() {
        return new InstrumentCache();
    }

}
