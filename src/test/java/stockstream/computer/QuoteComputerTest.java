package stockstream.computer;

import com.cheddar.robinhood.data.Quote;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class QuoteComputerTest {

    private final QuoteComputer quoteComputer = new QuoteComputer();

    @Test
    public void testGetMostRecentPrice_noAfterHours_expectMarketClosePrice() {
        final Quote quote = new Quote();
        quote.setLast_trade_price(2.49);

        assertEquals(2.49f, quoteComputer.computeMostRecentPrice(quote), .01);
    }

    @Test
    public void testGetMostRecentPrice_afterHours_expectAfterHoursPrice() {
        final Quote quote = new Quote();
        quote.setLast_trade_price(2.49);
        quote.setLast_extended_hours_trade_price(5.25);

        assertEquals(5.25f, quoteComputer.computeMostRecentPrice(quote), .01);
    }

    @Test
    public void testComputePercentChange_unchanged_expectZero() {
        final Quote quote = new Quote();
        quote.setLast_trade_price(2.49);
        quote.setPrevious_close(2.49);

        final double percentChange = quoteComputer.computePercentChange(quote);

        assertEquals(0, percentChange, .001);
    }

    @Test
    public void testComputePercentChange_doubledPrice_expect100Percent() {
        final Quote quote = new Quote();
        quote.setLast_trade_price(8.40);
        quote.setPrevious_close(4.20);

        final double percentChange = quoteComputer.computePercentChange(quote);

        assertEquals(100, percentChange, .001);
    }

    @Test
    public void testComputePercentChange_triplePrice_expect50Percent() {
        final Quote quote = new Quote();
        quote.setLast_trade_price(18.6539);
        quote.setPrevious_close(3.77);

        final double percentChange = quoteComputer.computePercentChange(quote);

        assertEquals(394.798, percentChange, .001);
    }
}
