package stockstream.database;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Repository
@Transactional
public class HistoricalEquityValueRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    public List<HistoricalEquityValue> getAllHistoricalEquityValues() {
        final Query query = sessionFactory.getCurrentSession().createQuery("FROM HistoricalEquityValue");

        @SuppressWarnings("unchecked")
        final List<HistoricalEquityValue> records = (List<HistoricalEquityValue>) query.list();

        log.info("Found {} records.", records.size());

        return records;
    }

    public List<HistoricalEquityValue> getHistoricalEquityValuesForDate(final Date date) {
        final String dateString = new SimpleDateFormat("yyyy-MM-dd").format(date);

        final Query query = sessionFactory.getCurrentSession()
                                          .createQuery(String.format("FROM HistoricalEquityValue " +
                                                                     "WHERE begins_at LIKE '%s%%'", dateString));

        @SuppressWarnings("unchecked")
        final List<HistoricalEquityValue> records = (List<HistoricalEquityValue>) query.list();

        log.info("Found {} records.", records.size());

        return records;
    }

    @Transactional
    public void saveHistoricalEquityValues(final List<HistoricalEquityValue> historicalEquityValues) {
        final Session session = sessionFactory.getCurrentSession();

        historicalEquityValues.forEach(session::saveOrUpdate);

        log.info("Successfully created {} values.", historicalEquityValues.size());
    }


}
