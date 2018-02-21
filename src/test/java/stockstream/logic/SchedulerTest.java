package stockstream.logic;


import org.apache.commons.lang.mutable.MutableBoolean;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SchedulerTest {

    private enum Event {
        ORDER_PLACED,
        GAME_TICK,
    }

    private Scheduler scheduler;

    @Before
    public void setupTest() {
        scheduler = new Scheduler();
    }

    @Test
    public void testSchedule_validEventScheduledAndNotified_expectScheduledMethodCalled() {
        final MutableBoolean runnableCalled = new MutableBoolean(false);

        scheduler.scheduleJob(() -> runnableCalled.setValue(true), Event.GAME_TICK);
        scheduler.notifyEvent(Event.GAME_TICK);

        assertEquals(true, runnableCalled.booleanValue());
    }

    @Test
    public void testSchedule_validEventScheduledDiffNotified_expectScheduledMethodNotCalled() {
        final MutableBoolean runnableCalled = new MutableBoolean(false);

        scheduler.scheduleJob(() -> runnableCalled.setValue(true), Event.ORDER_PLACED);
        scheduler.notifyEvent(Event.GAME_TICK);

        assertEquals(false, runnableCalled.booleanValue());
    }

    @Test
    public void testSchedule_validDateScheduled_expectScheduledMethodCalled() throws InterruptedException {
        final MutableBoolean runnableCalled = new MutableBoolean(false);

        scheduler.scheduleJob(() -> runnableCalled.setValue(true), new DateTime().plusMillis(1));
        Thread.sleep(200);
        scheduler.notifyEvent(Event.GAME_TICK);

        assertEquals(true, runnableCalled.booleanValue());
    }

}
