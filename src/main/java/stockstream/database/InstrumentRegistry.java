package stockstream.database;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@Transactional
public class InstrumentRegistry {

    private static final int BUFFER_SIZE = 1000;

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional
    public void saveInstrumentStubs(final List<InstrumentStub> withInstruments) {
        final Session session = sessionFactory.getCurrentSession();

        session.createQuery("DELETE from InstrumentStub").executeUpdate();

        final List<List<InstrumentStub>> instrumentPartitions = Lists.partition(withInstruments, BUFFER_SIZE);

        int totalUpdated = 0;

        for (final List<InstrumentStub> instrumentStubs : instrumentPartitions) {
            instrumentStubs.forEach(session::save);
            session.flush();
            session.clear();
            totalUpdated += instrumentStubs.size();
            log.info("Saved {} total instruments so far.", totalUpdated);
        }

        log.info("Saved {} InstrumentStub records using {} partitions.", withInstruments.size(), instrumentPartitions.size());
    }

    public List<InstrumentStub> getAllInstrumentStubs() {
        @SuppressWarnings("unchecked")
        List<InstrumentStub> records = (List<InstrumentStub>) sessionFactory.getCurrentSession().createQuery("FROM InstrumentStub").list();

        log.info("Found {} InstrumentStub records.", records.size());

        return records;
    }

    public Optional<InstrumentStub> getInstrumentByURL(final String instrumentUrl) {

        final String queryString = String.format("FROM InstrumentStub " +
                                                 "WHERE url='%s'", instrumentUrl);

        @SuppressWarnings("unchecked")
        List<InstrumentStub> records = (List<InstrumentStub>) sessionFactory.getCurrentSession().createQuery(queryString).list();

        log.info("Found {} InstrumentStub records.", records.size());

        if (records.size() <= 0) {
            return Optional.empty();
        }

        return Optional.ofNullable(records.iterator().next());
    }
}
