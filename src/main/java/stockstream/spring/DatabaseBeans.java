package stockstream.spring;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.beans.PropertyVetoException;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ComponentScans(value = {
        @ComponentScan("stockstream.database")
})
public class DatabaseBeans {

    public ComboPooledDataSource getDataSource() throws PropertyVetoException {
        ComboPooledDataSource dataSource = new ComboPooledDataSource();

        dataSource.setDriverClass("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl(System.getenv("DATABASE_ENDPOINT") + "?rewriteBatchedStatements=true");
        dataSource.setUser(System.getenv("DATABASE_USER"));
        dataSource.setPassword(System.getenv("DATABASE_PASSWORD"));
        dataSource.setMinPoolSize(3);
        dataSource.setMaxPoolSize(150);
        dataSource.setInitialPoolSize(30);
        dataSource.setMaxConnectionAge(300);
        dataSource.setMaxIdleTime(60);

        return dataSource;
    }

    @Bean
    public LocalSessionFactoryBean getSessionFactory() throws PropertyVetoException {
        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
        factoryBean.setDataSource(getDataSource());

        final Properties props = new Properties();
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQL57Dialect");
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.format_sql", "false");
        props.put("hibernate.use_sql_comments", "false");
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.connection.is-connection-validation-required", "true");
        props.put("hibernate.connection.autoReconnectForPools", "true");
        props.put("hibernate.connection.autoReconnect", "true");
        props.put("hibernate.connection.maxReconnects", "9999");
        props.put("hibernate.jdbc.batch_size", "1000");

        props.put("stockstream.stage", System.getenv().getOrDefault("STAGE", "TEST"));

        factoryBean.setPackagesToScan("stockstream.database");

        factoryBean.setHibernateProperties(props);

        return factoryBean;
    }

    @Bean
    public HibernateTransactionManager getTransactionManager() throws PropertyVetoException {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(getSessionFactory().getObject());
        return transactionManager;
    }

}
