package stockstream.computer;

import com.cheddar.robinhood.data.Quote;
import com.google.common.math.DoubleMath;

public class QuoteComputer {

    public double computeMostRecentPrice(final Quote quote) {
        final double lastTradeAfterHours = quote.getLast_extended_hours_trade_price();
        final double lastTrade = quote.getLast_trade_price();

        double mostRecentPrice = lastTrade;
        if (!DoubleMath.fuzzyEquals(lastTradeAfterHours, 0.0, .001)) {
            mostRecentPrice = lastTradeAfterHours;
        }

        return mostRecentPrice;
    }

    public double computePercentChange(final Quote quote) {
        final double prevClose = quote.getPrevious_close();

        final double lastTrade = computeMostRecentPrice(quote);

        final double change = lastTrade - prevClose;
        final double percentReturn = (change / prevClose) * 100;

        return percentReturn;
    }

}
