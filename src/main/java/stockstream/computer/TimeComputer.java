package stockstream.computer;

import com.cheddar.robinhood.data.MarketState;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.cache.BrokerCache;

public class TimeComputer {

    @Autowired
    private BrokerCache brokerCache;

    public boolean isMarketOpenNow() {
        final DateTime now = new DateTime();
        final MarketState marketState = this.brokerCache.getMarketState(now);
        return marketState.isOpenNow();
    }

    public boolean isAfterHours() {
        final DateTime now = new DateTime();
        final MarketState marketState = this.brokerCache.getMarketState(now);
        return marketState.isAfterHoursNow();
    }

    public boolean isMarketOpenToday() {
        final DateTime now = new DateTime();
        final MarketState marketState;

        marketState = this.brokerCache.getMarketState(now);

        return marketState.isOpenThisDay();
    }

    public MarketState findNextBusinessDay(final DateTime fromDate) {
        DateTime dayIterator = fromDate.plusDays(1);

        while (true) {
            final MarketState marketState;

            marketState = this.brokerCache.getMarketState(dayIterator);

            if (marketState.isOpenThisDay()) {
                return marketState;
            }

            dayIterator = dayIterator.plusDays(1);
        }
    }

}
