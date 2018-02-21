package stockstream.database;

import com.cheddar.robinhood.data.Quote;
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
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Assets")
public class Asset {

    @Id
    @Column(name = "symbol")
    private String symbol;

    @Column(name = "shares")
    private int shares;

    @Column(name = "avg_buy_price")
    private double avgBuyPrice;

    @Column(name = "quote")
    private String quote;

    public Quote getQuote() {
        return JSONUtil.deserializeString(this.quote, new TypeReference<Quote>() {}).orElse(new Quote());
    }

    public void setQuote(final Quote quote) {
        this.quote = JSONUtil.serializeObject(quote).orElse("{}");
    }

    public Asset(final String symbol, final int shares, final double avgBuyPrice, final Quote quote) {
        this.symbol = symbol;
        this.shares = shares;
        this.avgBuyPrice = avgBuyPrice;
        setQuote(quote);
    }

    public Asset(final Asset otherAsset) {
        symbol = otherAsset.symbol;
        shares = otherAsset.shares;
        avgBuyPrice = otherAsset.avgBuyPrice;
        setQuote(otherAsset.getQuote());
    }

    @Override
    public String toString() {
        return String.format("%s x %s @ $%.2f", shares, symbol, avgBuyPrice);
    }
}
