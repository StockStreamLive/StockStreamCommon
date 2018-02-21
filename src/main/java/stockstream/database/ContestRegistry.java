package stockstream.database;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
@Transactional
public class ContestRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional
    public void saveContestEntry(final ContestEntry contestEntry) {
        if (!ObjectUtils.isEmpty(contestEntry)) {
            sessionFactory.getCurrentSession().saveOrUpdate(contestEntry);
        }
    }

    public Collection<ContestEntry> getContestEntries(final String contestName) {
        final Query query = sessionFactory.getCurrentSession()
                                          .createQuery(String.format("FROM ContestEntry " +
                                                                     "WHERE contest_name='%s'", contestName));

        @SuppressWarnings("unchecked")
        final List<ContestEntry> contestEntries = (List<ContestEntry>) query.list();

        return contestEntries;
    }
}
