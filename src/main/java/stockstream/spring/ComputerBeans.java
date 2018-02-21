package stockstream.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stockstream.computer.*;

@Configuration
public class ComputerBeans {

    @Bean
    public TimeComputer timeComputer() {
        return new TimeComputer();
    }

    @Bean
    public AssetComputer assetComputer() {
        return new AssetComputer();
    }

    @Bean
    public WalletComputer walletComputer() {
        return new WalletComputer();
    }

    @Bean
    public OrderComputer orderComputer() {
        return new OrderComputer();
    }

    @Bean
    public QuoteComputer quoteComputer() {
        return new QuoteComputer();
    }
}
