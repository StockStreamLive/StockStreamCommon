package stockstream.database;


import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
@Transactional
public class WalletRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional(readOnly = true)
    public List<Wallet> selectWallets(final Set<String> platform_usernames) {
        Session session = sessionFactory.getCurrentSession();

        final Query query = session.createQuery("FROM Wallet WHERE platform_username IN (:players)");
        query.setParameterList("players", platform_usernames);

        @SuppressWarnings("unchecked")
        List<Wallet> wallets = (List<Wallet>) query.list();

        if (wallets.size() <= 0) {
            return Collections.emptyList();
        }

        return wallets;
    }

    @Transactional
    public void updateWallets(final Collection<Wallet> wallets) {
        final Session session = sessionFactory.getCurrentSession();
        wallets.forEach(session::saveOrUpdate);
        log.info("Successfully updated {}", wallets.toString());
    }

    public List<Wallet> getWallets(final Set<String> platform_usernames) {
        return this.selectWallets(platform_usernames);
    }

    public Wallet getWallet(final String platform_username) {
        final List<Wallet> walletSet = this.selectWallets(ImmutableSet.of(platform_username));
        if (CollectionUtils.isEmpty(walletSet)) {
            final Wallet newWallet = new Wallet(platform_username, 0, 0, 0);
            updateWallets(ImmutableSet.of(newWallet));
            return newWallet;
        }
        return walletSet.iterator().next();
    }

    @Transactional
    public void modWalletValue(final Wallet wallet, final String field, final double amount) {
        final String queryString = String.format("UPDATE Wallet " +
                                                 "SET %s = %s + %s " +
                                                 "WHERE platform_username='%s'", field, field, amount, wallet.getPlatform_username());
        Session session = sessionFactory.getCurrentSession();
        session.createQuery(queryString).executeUpdate();

        log.info("Executed update wallet query {}", queryString);
    }

    public List<Wallet> getAllPlayerWallets() {
        final Session session = sessionFactory.getCurrentSession();

        final Query query = session.createQuery("FROM Wallet");

        @SuppressWarnings("unchecked")
        List<Wallet> records = (List<Wallet>) query.list();

        log.info("Found {} records.", records.size());

        return records;
    }


}
