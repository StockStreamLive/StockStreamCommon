package stockstream.logic.elections;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import stockstream.data.Voter;
import stockstream.database.ElectionRegistry;
import stockstream.database.ElectionVoteStub;
import stockstream.util.JSONUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
public class Election<T extends Candidate> {

    @Setter
    @Autowired
    private ElectionRegistry electionRegistry;

    private Map<T, Runnable> candidateToRunnable = new ConcurrentHashMap<>();

    @Getter
    private Set<T> preProcessedCandidates = ConcurrentHashMap.newKeySet();

    private Function<T, Void> electionCallback = null;
    private Function<String, Optional<T>> messageParser = null;

    private CandidatePreprocessor<T> candidatePreprocessor = null;

    @Getter @Setter
    private long expirationDate = 0;

    @Getter
    private long rank = 0;

    @Getter
    private final String topic;

    @Getter
    private boolean subscribersOnly = false;

    @Getter @Setter
    private InstantElectionExecutor<T> instantElectionExecutor;

    // Values <= 0 are infinite
    @Getter @Setter
    private int maximumCandidates = 0;

    @Getter @Setter
    private Collection<Voter> eligibleVoters = new HashSet<>();

    final private Class<T> candidateClassType;

    public Election(final String topic, final Class<T> candidateClassType, final int rank) {
        this.topic = topic;
        this.rank = rank;
        this.candidateClassType = candidateClassType;
    }

    public String getElectionId() {
        return String.format("%s:%s", topic, expirationDate);
    }

    public Election<T> withMaximumCandidates(final int maximumCandidates) {
        this.maximumCandidates = maximumCandidates;
        return this;
    }

    public Election<T> withSubscribersOnly(final boolean subscribersOnly) {
        this.subscribersOnly = subscribersOnly;
        return this;
    }

    public Election<T> withExpirationDate(final long expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    public Election<T> withEligibleVoters(final Collection<Voter> eligibleVoters) {
        this.eligibleVoters = eligibleVoters;
        return this;
    }

    public Election<T> withInstantElection(final InstantElectionExecutor<T> instantElectionExecutor) {
        this.instantElectionExecutor = instantElectionExecutor;
        return this;
    }

    public Election<T> withVotePreProcessor(final CandidatePreprocessor<T> candidatePreprocessor) {
        this.candidatePreprocessor = candidatePreprocessor;
        return this;
    }

    public synchronized Election<T> withOutcome(final T candidate, final Runnable runnable) {
        addOutcome(candidate, runnable);
        return this;
    }

    public Election<T> withOutcome(final Function<T, Void> onElection) {
        this.electionCallback = onElection;
        return this;
    }

    public Election<T> withMessageParser(final Function<String, Optional<T>> parseMessage) {
        this.messageParser = parseMessage;
        return this;
    }

    public Map<T, Runnable> getCandidateToRunnable() {
        return Collections.unmodifiableMap(candidateToRunnable);
    }

    public Map<T, Set<Voter>> getCandidateToVoters() {
        final Collection<ElectionVoteStub> electionVoteStubs = electionRegistry.getElectionVotes(this.getElectionId());

        final Map<T, Set<Voter>> candidateToVoters = new HashMap<>();
        electionVoteStubs.forEach(vote -> {
            final Optional<T> candidate = JSONUtil.deserializeObject(vote.getVoteObject(), this.candidateClassType);
            final Optional<Voter> voter = JSONUtil.deserializeObject(vote.getVoterObject(), Voter.class);

            if (!candidate.isPresent() || !voter.isPresent()) {
                return;
            }
            candidateToVoters.computeIfAbsent(candidate.get(), set -> new HashSet<>()).add(voter.get());
        });

        return Collections.unmodifiableMap(candidateToVoters);
    }

    public SortedMap<T, Set<Voter>> getSortedCandidateToVoters() {
        final Map<T, Set<Voter>> candidateToVoters = this.getCandidateToVoters();

        final SortedMap<T, Set<Voter>> candidates = new TreeMap<>(new VoteComparator<>(candidateToVoters));

        candidateToVoters.forEach((key, value) -> {
            if (value.size() > 0) {
                candidates.put(key, value);
            }
        });

        return candidates;
    }

    public Map<Voter, T> getVoterToCandidate() {
        final Map<T, Set<Voter>> candidateToVoters = this.getCandidateToVoters();

        final Map<Voter, T> voterToCandidate = new HashMap<>();
        candidateToVoters.forEach((candidate, voters) -> voters.forEach(voter -> voterToCandidate.put(voter, candidate)));
        return voterToCandidate;
    }

    public void executeOutcome() {
        final SortedMap<T, Set<Voter>> sortedCandidates = getSortedCandidateToVoters();

        if (CollectionUtils.isEmpty(sortedCandidates.keySet())) {
            return;
        }

        final T winningCandidate = sortedCandidates.firstKey();

        executeOutcome(winningCandidate);
    }

    private void executeOutcome(final T winningCandidate) {
        final Runnable winningRunnable = candidateToRunnable.get(winningCandidate);

        if (winningRunnable != null) {
            log.info("Executing runnable {} for topic {} candidate {} after it won the election.", winningRunnable, topic, winningCandidate);

            try {
                winningRunnable.run();
            } catch (final Exception ex) {
                log.warn("Exception executing runnable for topic {} candidate {}", topic, winningCandidate, ex);
            }
        }

        preProcessedCandidates.clear();

        if (null != electionCallback) {
            electionCallback.apply(winningCandidate);
        }
    }

    public void addOutcome(final T candidate, final Runnable runnable) {
        candidateToRunnable.put(candidate, runnable);
    }

    public Optional<String> receiveVote(final String vote, final Voter voter) {
        final boolean eligibilityCheckFailed = eligibleVoters.size() > 0 && !eligibleVoters.contains(voter);
        final boolean subscriptionCheckFailed = subscribersOnly && !voter.isSubscriber();
        final boolean maxCandidatesCheckFailed = maximumCandidates > 0 && preProcessedCandidates.size() >= maximumCandidates;

        if (eligibilityCheckFailed || subscriptionCheckFailed || maxCandidatesCheckFailed) {
            return Optional.empty();
        }

        Optional<T> optionalVoteObject = Optional.empty();

        if (messageParser != null) {
            optionalVoteObject = messageParser.apply(vote);
        }

        if (!optionalVoteObject.isPresent()) {
            return Optional.empty();
        }

        final T voteObject = optionalVoteObject.get();

        final boolean preProcessAlreadyCompleted = preProcessedCandidates.contains(voteObject);

        if (!preProcessAlreadyCompleted && candidatePreprocessor != null) {
            final Optional<String> response = candidatePreprocessor.preProcessCandidate(voteObject, voter);
            if (response.isPresent()) {
                return response;
            }
        }

        if (instantElectionExecutor != null) {
            instantElectionExecutor.executeElection(voteObject, voter);
        } else {
            persistVote(voter, voteObject);
            preProcessedCandidates.add(voteObject);
        }

        return Optional.empty();
    }

    private void persistVote(final Voter voter, final T vote) {
        final ElectionVoteStub electionVoteStub = new ElectionVoteStub(voter, vote, this.getElectionId());

        electionRegistry.saveElectionVotes(ImmutableSet.of(electionVoteStub));
    }
}