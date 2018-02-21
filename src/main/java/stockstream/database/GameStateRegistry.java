package stockstream.database;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Repository
@Transactional
public class GameStateRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional
    public void saveGameStateStub(final GameStateStub gameStateStub) {
        final Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(gameStateStub);
    }

    public GameStateStub getGameStateStub() {
        final Query query = sessionFactory.getCurrentSession().createQuery("FROM GameStateStub");

        @SuppressWarnings("unchecked")
        final List<GameStateStub> stateStubs = (List<GameStateStub>) query.list();

        return stateStubs.iterator().next();
    }

}
