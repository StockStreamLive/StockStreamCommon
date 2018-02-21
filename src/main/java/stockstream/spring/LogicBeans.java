package stockstream.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stockstream.logic.PubSub;
import stockstream.logic.Scheduler;

@Configuration
public class LogicBeans {

    @Bean
    public PubSub pubSub() {
        return new PubSub();
    }

    @Bean
    public Scheduler scheduler() {
        return new Scheduler();
    }

}
