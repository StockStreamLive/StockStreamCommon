package stockstream.database;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Repository
@Transactional
public class RobinhoodAccountRegistry {

    @Autowired
    private SessionFactory sessionFactory;

    @Transactional
    public void saveAccountInfo(final RobinhoodAccountStub robinhoodAccountStub) {
        sessionFactory.getCurrentSession().saveOrUpdate(robinhoodAccountStub);
    }

    public RobinhoodAccountStub getAccountInfo() {
        @SuppressWarnings("unchecked")
        final List<RobinhoodAccountStub> accountStubs =
                (List<RobinhoodAccountStub>) sessionFactory.getCurrentSession().createQuery("FROM RobinhoodAccountStub").list();

        return accountStubs.iterator().next();
    }

}
