package stockstream.database;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Repository
@Transactional
public class VotedOrderRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional(readOnly = true)
    public List<VotedOrder> selectVotedOrder(final String queryString) {
        @SuppressWarnings("unchecked")
        List<VotedOrder> records = (List<VotedOrder>) sessionFactory.getCurrentSession().createQuery(queryString).list();

        log.info("Found {} records.", records.size());
        return records;
    }

    @Transactional
    public void updateVotedOrder(final VotedOrder votedOrder) {
        sessionFactory.getCurrentSession().update(votedOrder);
        log.info("Successfully updated {}", votedOrder.toString());
    }

    @Transactional
    public void saveVotedOrder(final VotedOrder votedOrder) {
        sessionFactory.getCurrentSession().save(votedOrder);
        log.info("Successfully updated {}", votedOrder.toString());
    }

    @Transactional(readOnly=true)
    public List<RobinhoodOrder> findUnmergedRobinhoodOrders(final String afterDate) {
        final String queryString = String.format("SELECT ro " +
                                                 "FROM RobinhoodOrder ro " +
                                                 "WHERE ro.created_at > '%s' " +
                                                 "AND NOT EXISTS (select id from VotedOrder where id=ro.id) " +
                                                 "AND NOT EXISTS (select id from WalletOrder where id=ro.id)", afterDate);

        final Query query = sessionFactory.getCurrentSession().createQuery(queryString);

        @SuppressWarnings("unchecked")
        final List<RobinhoodOrder> records = (List<RobinhoodOrder>) query.list();

        log.info("Found {} records.", records.size());
        return records;
    }

    public List<VotedOrder> findUnmatchedBuyOrders() {
        final String queryString = String.format("FROM VotedOrder " +
                                                 "WHERE sell_order_id IS NULL " +
                                                 "AND side='buy'");

        return this.selectVotedOrder(queryString);
    }

}
