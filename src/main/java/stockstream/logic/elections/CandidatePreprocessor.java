package stockstream.logic.elections;

import stockstream.data.Voter;

import java.util.Optional;

public interface CandidatePreprocessor<T> {

    Optional<String> preProcessCandidate(final T candidate, final Voter voter);

}
