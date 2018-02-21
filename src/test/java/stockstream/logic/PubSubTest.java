package stockstream.logic;


import org.apache.commons.lang.mutable.MutableBoolean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class PubSubTest {

    @InjectMocks
    private PubSub pubSub;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPublish_functionSubscribed_expectRunnableCalled() {
        final MutableBoolean applyMethodCalled = new MutableBoolean(false);

        pubSub.subscribeFunctionToClassType((Function<String, Void>) str -> {
            assertEquals(str, "testing123");
            applyMethodCalled.setValue(true);
            return null;
        }, String.class);

        pubSub.publishClassType(String.class, "testing123");

        assertEquals(true, applyMethodCalled.booleanValue());
    }

    @Test
    public void testPublish_functionSubscribedDifferentMessage_expectRunnableNotCalled() {
        final MutableBoolean applyMethodCalled = new MutableBoolean(false);

        pubSub.subscribeFunctionToClassType((Function<Integer, Void>) integ -> {
            applyMethodCalled.setValue(true);
            return null;
        }, Integer.class);

        pubSub.publishClassType(String.class, "testing123");

        assertEquals(false, applyMethodCalled.booleanValue());
    }

    @Test
    public void testPublish_runnableSubscribed_expectRunnableCalled() {
        final MutableBoolean applyMethodCalled = new MutableBoolean(false);

        pubSub.subscribeRunnableToClassType(() -> applyMethodCalled.setValue(true), String.class);

        pubSub.publishClassType(String.class, "testing123");

        assertEquals(true, applyMethodCalled.booleanValue());
    }

    @Test
    public void testPublish_runnableSubscribedDifferentMessage_expectRunnableNotCalled() {
        final MutableBoolean applyMethodCalled = new MutableBoolean(false);

        pubSub.subscribeRunnableToClassType(() -> applyMethodCalled.setValue(true), Integer.class);

        pubSub.publishClassType(String.class, "testing123");

        assertEquals(false, applyMethodCalled.booleanValue());
    }

}
