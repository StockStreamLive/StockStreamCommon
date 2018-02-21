package stockstream.database;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
@Transactional
public class PlayerVoteRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional
    public void savePlayerVotes(final Collection<PlayerVote> playerVote) {
        if (CollectionUtils.isEmpty(playerVote)) {
            return;
        }
        final Session session = sessionFactory.getCurrentSession();
        playerVote.forEach(session::save);
    }

    public List<PlayerVote> getAllPlayerVotes() {
        @SuppressWarnings("unchecked")
        List<PlayerVote> records = (List<PlayerVote>) sessionFactory.getCurrentSession().createQuery("FROM PlayerVote").list();

        log.info("Found {} records.", records.size());

        return records;
    }

}
