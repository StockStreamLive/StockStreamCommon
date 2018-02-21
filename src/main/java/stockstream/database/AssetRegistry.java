package stockstream.database;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;


@Slf4j
@Repository
@Transactional
public class AssetRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional
    public void saveAssets(final List<Asset> withAssets) {
        final Session session = sessionFactory.getCurrentSession();
        session.createQuery("DELETE from Asset").executeUpdate();
        withAssets.forEach(session::save);

        log.info("Saved {} Asset records.", withAssets.size());
    }

    public Collection<Asset> getAssets() {
        final Query query = sessionFactory.getCurrentSession().createQuery("FROM Asset ");

        @SuppressWarnings("unchecked")
        final List<Asset> assets = (List<Asset>) query.list();

        return assets;
    }
}
