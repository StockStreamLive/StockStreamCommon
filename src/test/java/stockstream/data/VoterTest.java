package stockstream.data;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VoterTest {

    @Test
    public void testEquals_sameChannelDifferentUser_expectNotEqual() {
        final Voter voter1 = new Voter("mike", "twitch", "#stockstream", true);
        final Voter voter2 = new Voter("ross", "twitch", "#stockstream", true);

        boolean equals1 = voter1.equals(voter2);
        boolean equals2 = voter1.hashCode() == voter2.hashCode();

        assertFalse(equals1);
        assertFalse(equals2);
    }

    @Test
    public void testEquals_sameChannelSameUser_expectEqual() {
        final Voter voter1 = new Voter("mike", "twitch", "#stockstream", true);
        final Voter voter2 = new Voter("mike", "twitch", "#stockstream", true);

        boolean equals1 = voter1.equals(voter2);
        boolean equals2 = voter1.hashCode() == voter2.hashCode();

        assertTrue(equals1);
        assertTrue(equals2);
    }

    @Test
    public void testEquals_differentChannelSameUser_expectEqual() {
        final Voter voter1 = new Voter("mike", "twitch", "#a", true);
        final Voter voter2 = new Voter("mike", "twitch", "#b", true);

        boolean equals1 = voter1.equals(voter2);
        boolean equals2 = voter1.hashCode() == voter2.hashCode();

        assertTrue(equals1);
        assertTrue(equals2);
    }
}
