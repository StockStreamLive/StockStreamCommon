package stockstream.database;


import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import stockstream.data.Constants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Repository
@Transactional
public class RobinhoodOrderRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional
    public void saveRobinhoodOrder(final RobinhoodOrder robinhoodOrder) {
        if (robinhoodOrder == null) {
            return;
        }
        sessionFactory.getCurrentSession().saveOrUpdate(robinhoodOrder);
    }

    @Transactional
    public void eraseTables() throws Exception {
        final String databaseURL = sessionFactory.getProperties().getOrDefault("stockstream.stage", "prod").toString();
        if (!databaseURL.equalsIgnoreCase("test")) {
            throw new Exception("Do not erase prod data!");
        }

        sessionFactory.getCurrentSession().createQuery("DELETE from RobinhoodOrder");
        log.info("Successfully erased tables.");
    }

    @Transactional(readOnly=true)
    public List<RobinhoodOrder> retrieveRobinhoodOrdersById(final Collection<String> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return Collections.emptyList();
        }

        final Query query = sessionFactory.getCurrentSession().createQuery("FROM RobinhoodOrder WHERE id IN (:ids)");
        query.setParameterList("ids", orderIds);

        @SuppressWarnings("unchecked")
        final List<RobinhoodOrder> records = (List<RobinhoodOrder>) query.list();

        log.info("Found {} records.", records.size());
        return records;
    }

    @Transactional(readOnly=true)
    public List<RobinhoodOrder> retrievePendingRobinhoodOrders(final Date afterDate) {
        if (Objects.isNull(afterDate)) {
            return Collections.emptyList();
        }

        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final String dateStr = dateFormat.format(afterDate);

        final Query query = sessionFactory.getCurrentSession()
                                          .createQuery(String.format("FROM RobinhoodOrder WHERE state in (:ids) AND created_at > '%s'", dateStr));
        query.setParameter("ids", Constants.PENDING_ORDER_STATES);

        @SuppressWarnings("unchecked")
        final List<RobinhoodOrder> records = (List<RobinhoodOrder>) query.list();

        log.info("Found {} records.", records.size());
        return records;
    }

    public List<RobinhoodOrder> getAllRobinhoodOrders() {
        @SuppressWarnings("unchecked")
        final List<RobinhoodOrder> records = (List<RobinhoodOrder>) sessionFactory.getCurrentSession().createQuery("FROM RobinhoodOrder").list();

        log.info("Found {} records.", records.size());

        return records;
    }

    @Transactional
    public void saveRobinhoodOrders(final Collection<RobinhoodOrder> robinhoodOrders) {
        final Session session = sessionFactory.getCurrentSession();

        robinhoodOrders.forEach(session::saveOrUpdate);

        log.info("Successfully created {} orders.", robinhoodOrders.size());
    }


}
