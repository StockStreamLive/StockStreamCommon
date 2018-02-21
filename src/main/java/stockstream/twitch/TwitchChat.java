package stockstream.twitch;

import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.logic.Scheduler;

import javax.annotation.PostConstruct;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TwitchChat {

    @Data
    @AllArgsConstructor
    private class ChannelResponse {
        private final String message;
        private final String channel;
    }

    @Autowired
    private Configuration configuration;

    @Autowired
    private Scheduler scheduler;

    private Queue<ChannelResponse> messageQueue = new ConcurrentLinkedQueue<>();
    private Long lastMessageSendTime = new DateTime().getMillis();

    @Setter
    private PircBotX activeBot;

    @PostConstruct
    public void init() {
        this.scheduler.scheduleJob(this::tickMessageQueues, 5000, 500, TimeUnit.MILLISECONDS);

        final Thread runThread = new Thread(() -> {
            while (true) {
                log.info("Starting Twitch chat (IRC) listener.");

                final PircBotX bot = new PircBotX(configuration);
                activeBot = bot;

                try {
                    bot.startBot();
                } catch (final Exception e) {
                    log.error(e.getMessage(), e);
                }

                log.info("Twitch chat (IRC) listener stopped for some reason, sleeping for 60 seconds.");

                try {
                    Thread.sleep(60000L);
                } catch (final InterruptedException e) {
                    log.error(e.getMessage(), e);
                    log.info("Failed to sleep for 60 seconds (WTF?).");
                }
            }
        });

        runThread.start();
    }

    public void enqueueMessage(final String channel, final String message) {
        messageQueue.add(new ChannelResponse(message, channel));
    }

    public void broadcastMessage(final String message) {
        configuration.getAutoJoinChannels().keySet().forEach(channel -> messageQueue.add(new ChannelResponse(message, channel)));
    }

    @VisibleForTesting
    protected void tickMessageQueues() {
        if (new DateTime().minusMillis(250).isAfter(lastMessageSendTime) && messageQueue.size() > 0) {
            sendMessage(messageQueue.poll());
        }
    }

    private void sendMessage(final ChannelResponse channelResponse) {
        if (StringUtils.isEmpty(channelResponse.getMessage()) || StringUtils.isEmpty(channelResponse.getChannel())) {
            return;
        }

        try {
            activeBot.send().message(channelResponse.getChannel(), channelResponse.getMessage());
            lastMessageSendTime = new DateTime().getMillis();
        } catch (final Throwable t) {
            log.warn(t.getMessage(), t);
        }
    }
}
