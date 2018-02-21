package stockstream.computer;

import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.data.Position;
import com.cheddar.robinhood.data.Quote;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import stockstream.TestDataUtils;
import stockstream.cache.InstrumentCache;
import stockstream.database.*;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class AssetComputerTest {

    @Mock
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @Mock
    private WalletOrderRegistry walletOrderRegistry;

    @Mock
    private RobinhoodAPI broker;

    @Mock
    private InstrumentCache instrumentCache;

    @Mock
    private QuoteComputer quoteComputer;

    @InjectMocks
    private AssetComputer assetComputer;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsSymbol_validSymbolDollarSign_expectTrue() {
        when(instrumentCache.getValidSymbols()).thenReturn(ImmutableSet.of("ABC"));

        boolean isSymbolD = assetComputer.isSymbol("$ABC");
        boolean isSymbol = assetComputer.isSymbol("ABC");

        assertTrue(isSymbolD);
        assertTrue(isSymbol);
    }

    @Test
    public void testLoadSymbolToQuote_validSymbolDollarSign_expectTrue() throws RobinhoodException {
        final Quote quote = TestDataUtils.createQuote("ABC", 2.00);

        when(broker.getQuotes(any())).thenReturn(ImmutableList.of(quote));

        Map<String, Quote> quoteMap = assetComputer.loadSymbolToQuote(ImmutableSet.of("ABC"));

        assertTrue(quoteMap.containsKey("ABC"));
    }

    @Test
    public void testGetAssetsOwnedByPlayer_oneFilledPosition_expectOneAssetReturned() throws RobinhoodException {
        final Quote quote = TestDataUtils.createQuote("AMZN", 2.00);

        final InstrumentStub instrument = new InstrumentStub();
        instrument.setUrl("http://instrument");
        instrument.setSymbol("AMZN");

        final RobinhoodOrder robinhoodOrder = new RobinhoodOrder("123", "filled", "2017", "900.0", "901.0", "buy", "1", "AMZN", "{}");

        when(robinhoodOrderRegistry.retrieveRobinhoodOrdersById(any())).thenReturn(ImmutableList.of(robinhoodOrder));
        when(broker.getPositions()).thenReturn(ImmutableList.of(new Position(1, 900, "http://instrument")));
        when(instrumentCache.getSymbolToInstrument()).thenReturn(ImmutableMap.of("AMZN", instrument));
        when(broker.getQuotes(any())).thenReturn(ImmutableList.of(quote));

        final Collection<Asset> assets = assetComputer.getAssetsOwnedByPlayer("twitch:michrob");

        assertEquals(1, assets.size());
        assertEquals("AMZN", assets.iterator().next().getSymbol());
    }

    @Test
    public void testGetAssetsOwnedByPlayer_oneConfirmedPosition_expectZeroAssetReturned() throws RobinhoodException {
        final Quote quote = TestDataUtils.createQuote("AMZN", 2.00);

        final InstrumentStub instrument = new InstrumentStub();
        instrument.setUrl("http://instrument");
        instrument.setSymbol("AMZN");

        final RobinhoodOrder robinhoodOrder = new RobinhoodOrder("123", "confirmed", "2017", "900.0", "901.0", "buy", "1", "AMZN", "{}");

        when(robinhoodOrderRegistry.retrieveRobinhoodOrdersById(any())).thenReturn(ImmutableList.of(robinhoodOrder));
        when(broker.getPositions()).thenReturn(ImmutableList.of(new Position(1, 900, "http://instrument")));
        when(instrumentCache.getSymbolToInstrument()).thenReturn(ImmutableMap.of("AMZN", instrument));
        when(broker.getQuotes(any())).thenReturn(ImmutableList.of(quote));

        final Collection<Asset> assets = assetComputer.getAssetsOwnedByPlayer("twitch:michrob");

        assertEquals(0, assets.size());
    }


    @Test
    public void testGetAssetsOwnedByPlayer_oneConfirmedPositionOneFilled_expectZeroAssetReturned() throws RobinhoodException {
        final Quote quote = TestDataUtils.createQuote("AMZN", 2.00);

        final InstrumentStub instrument = new InstrumentStub();
        instrument.setUrl("http://instrument");
        instrument.setSymbol("AMZN");

        final RobinhoodOrder robinhoodOrder1 = new RobinhoodOrder("123", "confirmed", "2017", "900.0", "901.0", "buy", "1", "AMZN", "{}");
        final RobinhoodOrder robinhoodOrder2 = new RobinhoodOrder("123", "filled", "2017", "900.0", "901.0", "buy", "1", "AMZN", "{}");

        when(robinhoodOrderRegistry.retrieveRobinhoodOrdersById(any())).thenReturn(ImmutableList.of(robinhoodOrder1, robinhoodOrder2));
        when(broker.getPositions()).thenReturn(ImmutableList.of(new Position(1, 900, "http://instrument")));
        when(instrumentCache.getSymbolToInstrument()).thenReturn(ImmutableMap.of("AMZN", instrument));
        when(broker.getQuotes(any())).thenReturn(ImmutableList.of(quote));

        final Collection<Asset> assets = assetComputer.getAssetsOwnedByPlayer("twitch:michrob");

        assertEquals(1, assets.size());
    }

    @Test
    public void testGetPctReturn_valueDecline_expectNegativeReturn() {
        final Quote quote = new Quote();
        quote.setLast_trade_price(2.49);
        final Asset asset = new Asset("ABC", 1, 3.49d, quote);

        final double percentReturn = assetComputer.computePercentReturn(asset);

        assertTrue(percentReturn < 0);
    }

    @Test
    public void testGetPctReturn_valueIncrease_expectPositiveReturn() {
        final Quote quote = new Quote();
        quote.setLast_trade_price(2.49);
        final Asset asset = new Asset("ABC", 1, .49d, quote);

        when(quoteComputer.computeMostRecentPrice(any())).thenReturn(2.49d);

        final double percentReturn = assetComputer.computePercentReturn(asset);

        assertTrue(percentReturn > 0);
    }

    @Test
    public void testGetAssetValue_multipleShares_expectSharesTimesPrice() {
        final Quote quote = new Quote();
        quote.setLast_trade_price(5.0);
        final Asset asset = new Asset("ABC", 2, .49d, quote);

        when(quoteComputer.computeMostRecentPrice(any())).thenReturn(5d);

        final double assetValue = assetComputer.computeAssetValue(asset);

        assertEquals(assetValue, 10d, .005);
    }


}
