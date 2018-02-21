package stockstream.database;

import com.cheddar.robinhood.data.MarginBalances;
import com.cheddar.robinhood.data.Portfolio;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stockstream.util.JSONUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RobinhoodAccount")
public class RobinhoodAccountStub {

    private static final String ACCOUNT_KEY = "stockstream";

    @Id
    @Column(name = "accountName")
    private String accountName = ACCOUNT_KEY;

    @Column(name = "balances")
    private String balances;

    @Column(name = "portfolio")
    private String portfolio;

    public MarginBalances getBalances() {
        return JSONUtil.deserializeString(balances, new TypeReference<MarginBalances>() {}).orElse(new MarginBalances());
    }

    public Portfolio getPortfolio() {
        return JSONUtil.deserializeString(portfolio, new TypeReference<Portfolio>() {}).orElse(new Portfolio());
    }

    public void setBalances(final MarginBalances marginBalances) {
        this.balances = JSONUtil.serializeObject(marginBalances).orElse("{}");
    }

    public void setPortfolio(final Portfolio portfolio) {
        this.portfolio = JSONUtil.serializeObject(portfolio).orElse("{}");
    }

    public RobinhoodAccountStub(final MarginBalances marginBalances, final Portfolio portfolio) {
        this.accountName = ACCOUNT_KEY;
        setBalances(marginBalances);
        setPortfolio(portfolio);
    }

}
