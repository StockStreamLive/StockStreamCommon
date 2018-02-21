package stockstream.database;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Repository
@Transactional
public class WalletOrderRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional(readOnly = true)
    public List<WalletOrder> selectWalletOrders(final String queryString) {
        @SuppressWarnings("unchecked")
        List<WalletOrder> records = (List<WalletOrder>) sessionFactory.getCurrentSession().createQuery(queryString).list();

        log.info("Found {} records.", records.size());
        return records;
    }

    @Transactional
    public void updateWalletOrder(final WalletOrder walletOrder) {
        sessionFactory.getCurrentSession().update(walletOrder);
        log.info("Successfully updated {}", walletOrder.toString());
    }

    @Transactional
    public void saveWalletOrder(final WalletOrder walletOrder) {
        sessionFactory.getCurrentSession().save(walletOrder);
        log.info("Successfully updated {}", walletOrder.toString());
    }

    public List<WalletOrder> findUnmatchedBuyOrders(final String platform_username) {
        final String queryString = String.format("FROM WalletOrder " +
                                                 "WHERE platform_username='%s' " +
                                                 "AND sell_order_id IS NULL " +
                                                 "AND side='buy'", platform_username);

        return this.selectWalletOrders(queryString);
    }

    public List<WalletOrder> findUnsoldOrPendingBuyOrders(final String platform_username) {

        final String queryString = String.format("SELECT wo " +
                                                 "FROM WalletOrder wo, RobinhoodOrder ro " +
                                                 "WHERE wo.platform_username='%s' " +
                                                 "AND ((wo.sell_order_id IS NULL AND wo.side='buy' AND ro.id=wo.id AND ro.state NOT IN ('cancelled', 'failed')) " +
                                                 "OR (ro.state NOT IN ('filled') AND ro.id=wo.sell_order_id)) ", platform_username);

        final List<WalletOrder> walletOrders = this.selectWalletOrders(queryString);

        log.info("Found {} records.", walletOrders.size());

        return walletOrders;
    }

    public List<WalletOrder> findUnmatchedFilledBuyOrders(final String platform_username, final String symbol) {

        final String queryString = String.format("SELECT wo " +
                                                 "FROM WalletOrder wo, RobinhoodOrder ro " +
                                                 "WHERE wo.platform_username='%s' " +
                                                 "AND wo.symbol='%s' " +
                                                 "AND ro.state='filled' " +
                                                 "AND wo.id=ro.id " +
                                                 "AND wo.sell_order_id IS NULL " +
                                                 "AND wo.side='buy'", platform_username, symbol);

        final List<WalletOrder> walletOrders = this.selectWalletOrders(queryString);

        log.info("Found {} records.", walletOrders.size());

        return walletOrders;
    }

    public List<WalletOrder> findUnmatchedFilledBuyOrdersForSymbol(final String symbol) {

        final String queryString = String.format("SELECT wo " +
                                                 "FROM WalletOrder wo, RobinhoodOrder ro " +
                                                 "WHERE wo.symbol='%s' " +
                                                 "AND ro.state='filled' " +
                                                 "AND wo.id=ro.id " +
                                                 "AND wo.sell_order_id IS NULL " +
                                                 "AND wo.side='buy'", symbol);

        final List<WalletOrder> walletOrders = this.selectWalletOrders(queryString);

        log.info("Found {} records.", walletOrders.size());

        return walletOrders;
    }

    public Optional<WalletOrder> findNextSellableBuyOrder(final String player, final String symbol) {

        final String queryString = String.format("SELECT wo " +
                                                 "FROM WalletOrder wo, RobinhoodOrder ro " +
                                                 "WHERE wo.platform_username='%s' " +
                                                 "AND wo.id=ro.id " +
                                                 "AND wo.symbol='%s' " +
                                                 "AND ro.state='filled' " +
                                                 "AND wo.sell_order_id IS NULL " +
                                                 "AND wo.side='buy' " +
                                                 "ORDER BY wo.created_at", player, symbol);

        final List<WalletOrder> walletOrders = this.selectWalletOrders(queryString);

        log.info("Found {} records.", walletOrders.size());

        if (walletOrders.size() <= 0) {
            return Optional.empty();
        }

        return Optional.of(walletOrders.iterator().next());
    }

    @Transactional
    public void eraseFailedSellOrders(final Set<String> failedSellOrders) {
        final Query query = sessionFactory.getCurrentSession()
                                          .createQuery("Update WalletOrder " +
                                                       "SET sell_order_id=NULL " +
                                                       "WHERE sell_order_id in (:ids)");
        query.setParameterList("ids", failedSellOrders);

        int result = query.executeUpdate();

        log.info("Got result {}", result);
    }

    public Set<String> findUnfilledWalletSellOrders() {
        final String queryString = String.format("FROM WalletOrder wo, RobinhoodOrder ro " +
                                                 "WHERE wo.sell_order_id IS NOT NULL " +
                                                 "AND wo.sell_order_id=ro.id " +
                                                 "AND ro.state IN ('failed', 'cancelled') " +
                                                 "AND wo.side='buy' ");

        @SuppressWarnings("unchecked")
        final List<Object[]> records = (List<Object[]>) sessionFactory.getCurrentSession().createQuery(queryString).list();

        if (records.size() <= 0) {
            return Collections.emptySet();
        }

        final Set<String> failedSellOrders = new HashSet<>();
        for (final Object[] objectArray : records) {
            for (final Object object : objectArray) {
                if (object instanceof RobinhoodOrder) {
                    failedSellOrders.add(((RobinhoodOrder) object).getId());
                }
            }
        }

        log.info("Found failedSellOrders {}", failedSellOrders);

        return failedSellOrders;
    }

    @Transactional
    public void eraseTables() throws Exception {
        final String databaseURL = sessionFactory.getProperties().getOrDefault("stockstream.stage", "prod").toString();
        if (!databaseURL.equalsIgnoreCase("test")) {
            throw new Exception("Do not erase prod data!");
        }

        final Session session = sessionFactory.getCurrentSession();

        final List<WalletOrder> walletOrders = selectWalletOrders("FROM WalletOrder");
        walletOrders.forEach(session::delete);

        session.createQuery("DELETE from Wallet");
        log.info("Successfully erased tables.");
    }

    public List<WalletOrder> getAllWalletOrders() {
        @SuppressWarnings("unchecked")
        List<WalletOrder> records = (List<WalletOrder>) sessionFactory.getCurrentSession().createQuery("FROM WalletOrder").list();

        log.info("Found {} records.", records.size());

        return records;
    }


}
