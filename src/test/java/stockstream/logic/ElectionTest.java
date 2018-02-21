package stockstream.logic;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import stockstream.TestDataUtils;
import stockstream.data.Voter;
import stockstream.database.ElectionRegistry;
import stockstream.logic.elections.Election;
import stockstream.spring.DatabaseBeans;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class ElectionTest {

    private static ElectionRegistry electionRegistry;

    private Election<TestCandidate> testElection;

    @BeforeClass
    public static void setupTestStatic() throws Exception {
        final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

        context.register(DatabaseBeans.class);
        context.refresh();
        electionRegistry = context.getBean(ElectionRegistry.class);
    }

    @Before
    public void setupTest() {
        testElection = new Election<>("test", TestCandidate.class, 1);
        testElection.setExpirationDate(new Date().getTime());
        testElection.setElectionRegistry(electionRegistry);
    }

    @Test
    public void testWithEligibleVoters_voterEligible_expectVoteCounted() {
        final String player = "mike";

        testElection.withMessageParser(s -> Optional.of(new TestCandidate(s)))
                    .withEligibleVoters(ImmutableList.of(TestDataUtils.createVoter(player)));

        final MutableBoolean outcomeExecuted = new MutableBoolean(false);

        testElection.addOutcome(new TestCandidate("!test"), () -> outcomeExecuted.setValue(true));
        testElection.receiveVote("!test", TestDataUtils.createVoter(player));

        testElection.executeOutcome();

        assertTrue(outcomeExecuted.booleanValue());
    }

    @Test
    public void testWithEligibleVoters_voterNotEligible_expectVoteNotCounted() {
        testElection.withMessageParser(s -> Optional.of(new TestCandidate(s)))
                    .withEligibleVoters(ImmutableList.of(TestDataUtils.createVoter("notmike")));

        final MutableBoolean outcomeExecuted = new MutableBoolean(false);

        testElection.addOutcome(new TestCandidate("!test"),  () -> outcomeExecuted.setValue(true));
        testElection.receiveVote("!test", TestDataUtils.createVoter("p1"));

        testElection.executeOutcome();

        assertFalse(outcomeExecuted.booleanValue());
    }

    @Test
    public void testWithNoEligibleVoters_voteCast_expectVoteCounted() {
        final String player = "mike";

        testElection.withMessageParser(s -> Optional.of(new TestCandidate(s)))
                    .withEligibleVoters(ImmutableList.of());

        final MutableBoolean outcomeExecuted = new MutableBoolean(false);

        testElection.addOutcome(new TestCandidate("!test"), () -> outcomeExecuted.setValue(true));
        testElection.receiveVote("!test", TestDataUtils.createVoter(player));

        testElection.executeOutcome();

        assertTrue(outcomeExecuted.booleanValue());
    }

    @Test
    public void testWithInstantElection_voteCast_expectVoteCounted() {
        final MutableBoolean outcomeExecuted = new MutableBoolean(false);

        testElection.withMessageParser(s -> Optional.of(new TestCandidate(s)))
                    .withInstantElection((candidate, vote) -> outcomeExecuted.setValue(true));

        testElection.receiveVote("!test", TestDataUtils.createVoter("p1"));

        assertTrue(outcomeExecuted.booleanValue());
    }

    @Test
    public void testSubscribersOnly_subscriberVote_expectVoteCounted() {
        testElection.withMessageParser(s -> Optional.of(new TestCandidate(s))).withSubscribersOnly(true);
        final MutableBoolean outcomeExecuted = new MutableBoolean(false);

        testElection.addOutcome(new TestCandidate("!test"), () -> outcomeExecuted.setValue(true));
        testElection.receiveVote("!test", TestDataUtils.createVoter("p1", true));
        testElection.executeOutcome();

        assertTrue(outcomeExecuted.booleanValue());
    }

    @Test
    public void testSubscribersOnly_nonSubscriberVote_expectVoteNotCounted() {
        testElection.withMessageParser(s -> Optional.of(new TestCandidate(s))).withSubscribersOnly(true);
        final MutableBoolean outcomeExecuted = new MutableBoolean(false);

        testElection.addOutcome(new TestCandidate("!test"), () -> outcomeExecuted.setValue(true));
        testElection.receiveVote("!test", TestDataUtils.createVoter("p1"));
        testElection.executeOutcome();

        assertFalse(outcomeExecuted.booleanValue());
    }

    @Test
    public void testPreProcessCandidate_votePassed_expectCacheFilledAndCleared() {
        testElection.withMessageParser(s -> Optional.of(new TestCandidate(s))).withSubscribersOnly(false);
        final MutableBoolean outcomeExecuted = new MutableBoolean(false);

        testElection.addOutcome(new TestCandidate("!test"), () -> outcomeExecuted.setValue(true));
        testElection.receiveVote("!test", TestDataUtils.createVoter("p1"));

        assertEquals(1, testElection.getPreProcessedCandidates().size());

        testElection.executeOutcome();

        assertTrue(outcomeExecuted.booleanValue());
        assertEquals(0, testElection.getPreProcessedCandidates().size());
    }

    @Test
    public void testWithVotePreProcessor_preProcessorRejects_expectVoteNotCounted() {
        testElection.withMessageParser(s -> Optional.of(new TestCandidate(s)));
        testElection.withVotePreProcessor((candidate, voter) -> Optional.of("failed"));

        final MutableBoolean outcomeExecuted = new MutableBoolean(false);
        testElection.addOutcome(new TestCandidate("!test"), () -> outcomeExecuted.setValue(true));
        testElection.receiveVote("!test", TestDataUtils.createVoter("p1"));
        testElection.executeOutcome();

        assertFalse(outcomeExecuted.booleanValue());
    }

    @Test
    public void testWithMessageParser_voteCast_expectVoteCounted() {
        testElection.withEligibleVoters(ImmutableList.of());
        testElection.withMessageParser(s -> Optional.of(new TestCandidate("!transformedvote")));

        final MutableBoolean outcomeExecuted = new MutableBoolean(false);

        testElection.addOutcome(new TestCandidate("!transformedvote"), () -> outcomeExecuted.setValue(true));
        testElection.receiveVote("!untransformed", TestDataUtils.createVoter("p1"));
        testElection.executeOutcome();

        assertTrue(outcomeExecuted.booleanValue());
    }

    @Test
    public void testGetSortedCandidateToVoters_threeVotesTwoCandidates_expectTopVotedFirst() {
        testElection.withEligibleVoters(ImmutableList.of())
                    .withMessageParser(s -> Optional.of(new TestCandidate(s)));

        testElection.addOutcome(new TestCandidate("!candidate1"), () -> {});
        testElection.addOutcome(new TestCandidate("!candidate2"), () -> {});
        testElection.receiveVote("!candidate1", TestDataUtils.createVoter("p1"));
        testElection.receiveVote("!candidate2", TestDataUtils.createVoter("p2"));
        testElection.receiveVote("!candidate2", TestDataUtils.createVoter("p3"));

        final Map<TestCandidate, Set<Voter>> candidateToVoters = testElection.getSortedCandidateToVoters();

        assertEquals(new TestCandidate("!candidate2"), candidateToVoters.keySet().iterator().next());
    }

    @Test
    public void testGetVoterToCandidate_voteCast_expectMapContainsVoterAndCandidate() {
        testElection.withEligibleVoters(ImmutableList.of())
                    .withMessageParser(s -> Optional.of(new TestCandidate(s)));

        testElection.addOutcome(new TestCandidate("!candidate1"), () -> {});
        testElection.receiveVote("!candidate1", TestDataUtils.createVoter("p1"));

        final Map<Voter, TestCandidate> voterToCandidates = testElection.getVoterToCandidate();

        assertEquals(new TestCandidate("!candidate1"), voterToCandidates.get(TestDataUtils.createVoter("p1")));
    }

    @Test
    public void testGetSortedCandidateToVoters_maxCandidates_expectMaxCandidatesRespected() {
        final int max = 2;

        testElection.withEligibleVoters(ImmutableList.of())
                    .withMessageParser(s -> Optional.of(new TestCandidate(s)))
                    .withMaximumCandidates(max);

        testElection.receiveVote("!candidate1", TestDataUtils.createVoter("p1"));
        testElection.receiveVote("!candidate2", TestDataUtils.createVoter("p2"));
        testElection.receiveVote("!candidate3", TestDataUtils.createVoter("p3"));

        final Map<TestCandidate, Set<Voter>> candidateToVoters = testElection.getSortedCandidateToVoters();

        assertEquals(max, candidateToVoters.keySet().size());
    }
}
