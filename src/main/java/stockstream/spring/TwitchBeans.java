package stockstream.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stockstream.twitch.TwitchAPI;
import stockstream.twitch.TwitchChat;

@Configuration
public class TwitchBeans {

    @Bean
    public TwitchAPI twitchAPI() {
        return new TwitchAPI();
    }

    @Bean
    public TwitchChat twitchChat() {
        return new TwitchChat();
    }

}
