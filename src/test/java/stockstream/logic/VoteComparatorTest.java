package stockstream.logic;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import stockstream.TestDataUtils;
import stockstream.data.Voter;
import stockstream.logic.elections.VoteComparator;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class VoteComparatorTest {

    @Test
    public void testTestSort_1VoteVs2Votes_expect2VotesAtFront() {
        final Map<String, Set<Voter>> candidatesToPlayers = ImmutableMap.of("a", ImmutableSet.of(TestDataUtils.createVoter("a"),
                                                                                                 TestDataUtils.createVoter("b")),
                                                                            "b", ImmutableSet.of(TestDataUtils.createVoter("c")),
                                                                            "c", ImmutableSet.of(TestDataUtils.createVoter("d"),
                                                                                                 TestDataUtils.createVoter("e"),
                                                                                                 TestDataUtils.createVoter("f")));

        final VoteComparator<String> comparator = new VoteComparator<>(candidatesToPlayers);

        final SortedMap<String, Set<Voter>> candidates = new TreeMap<>(comparator);
        candidates.putAll(candidatesToPlayers);

        final String topCandidate = candidates.firstKey();
        assertEquals("c", topCandidate);
    }

}
