package stockstream.twitch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pircbotx.PircBotX;
import org.pircbotx.output.OutputIRC;

import static org.mockito.Mockito.*;


public class TwitchChatTest {

    private TwitchChat twitchListener;

    @Mock
    private PircBotX activeBot;

    @Mock
    private OutputIRC outputIRC;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);

        when(activeBot.send()).thenReturn(outputIRC);

        twitchListener = new TwitchChat();
        twitchListener.setActiveBot(activeBot);
    }

    @Test
    public void testEnqueueMessage_moreThan250msLater_expectMessageSent() throws InterruptedException {
        twitchListener.enqueueMessage("#test", "test");

        Thread.sleep(500);
        twitchListener.tickMessageQueues();

        verify(activeBot, times(1)).send();
    }

    @Test
    public void testEnqueueMessage_moreThan250msLater2Messages_expectOneMessageSent() throws InterruptedException {
        twitchListener.enqueueMessage("#test", "test1");
        twitchListener.enqueueMessage("#test", "test2");

        Thread.sleep(1500);
        twitchListener.tickMessageQueues();
        twitchListener.tickMessageQueues();
        twitchListener.tickMessageQueues();

        verify(activeBot, times(1)).send();
    }

    @Test
    public void testEnqueueMessage_lessThan250msLater_expectMessageNotSent() throws InterruptedException {
        twitchListener.enqueueMessage("#test", "test");

        Thread.sleep(10);
        twitchListener.tickMessageQueues();

        verify(activeBot, times(0)).send();
    }

}
