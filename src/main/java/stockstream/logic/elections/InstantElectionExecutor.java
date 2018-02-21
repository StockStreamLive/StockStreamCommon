package stockstream.logic.elections;

import stockstream.data.Voter;

public interface InstantElectionExecutor<T> {

    void executeElection(final T candidate, final Voter vote);

}
