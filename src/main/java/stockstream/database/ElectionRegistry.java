package stockstream.database;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import stockstream.logic.elections.Election;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
@Transactional
public class ElectionRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional
    public void saveElectionStubs(final Collection<ElectionStub> electionStubs) {
        if (CollectionUtils.isEmpty(electionStubs)) {
            return;
        }
        final Session session = sessionFactory.getCurrentSession();
        electionStubs.forEach(session::saveOrUpdate);
    }

    @Transactional
    public void saveElectionVotes(final Collection<ElectionVoteStub> electionVoteStubs) {
        if (CollectionUtils.isEmpty(electionVoteStubs)) {
            return;
        }
        final Session session = sessionFactory.getCurrentSession();
        electionVoteStubs.forEach(session::saveOrUpdate);
    }

    @Transactional
    public void archiveElections(final Collection<Election<?>> elections) {
        final Set<ElectionStub> electionStubs = new HashSet<>();

        int rank = 0;
        for (final Election<?> election : elections) {
            electionStubs.add(new ElectionStub(election));
        }

        saveElectionStubs(electionStubs);
    }

    public Collection<ElectionVoteStub> getElectionVotes(final String electionId) {
        final Session session = sessionFactory.getCurrentSession();

        final Query query = session.createQuery(String.format("FROM ElectionVoteStub " +
                                                              "WHERE electionId='%s'", electionId));

        @SuppressWarnings("unchecked")
        final List<ElectionVoteStub> voteStubs = (List<ElectionVoteStub>) query.list();

        return voteStubs;
    }

    public Collection<ElectionStub> getElections() {
        final Session session = sessionFactory.getCurrentSession();

        final Query query = session.createQuery("FROM ElectionStub ORDER BY rank");

        @SuppressWarnings("unchecked")
        final List<ElectionStub> stateStubs = (List<ElectionStub>) query.list();

        return stateStubs;
    }

    public ElectionStub getElection(final String topic) {
        final Session session = sessionFactory.getCurrentSession();

        final Query query = session.createQuery(String.format("FROM ElectionStub where topic='%s'", topic));

        @SuppressWarnings("unchecked")
        final List<ElectionStub> electionStubs = (List<ElectionStub>) query.list();

        return electionStubs.iterator().next();
    }
}
