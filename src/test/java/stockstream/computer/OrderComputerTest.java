package stockstream.computer;

import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.data.Execution;
import com.cheddar.robinhood.data.MarginBalances;
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
import stockstream.cache.BrokerCache;
import stockstream.cache.InstrumentCache;
import stockstream.data.*;
import stockstream.database.*;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.when;

public class OrderComputerTest {

    @Mock
    private WalletComputer walletComputer;

    @Mock
    private RobinhoodAPI broker;

    @Mock
    private BrokerCache brokerCache;

    @Mock
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @Mock
    private WalletRegistry walletRegistry;

    @Mock
    private WalletOrderRegistry walletOrderRegistry;

    @Mock
    private AssetComputer assetComputer;

    @Mock
    private TimeComputer timeComputer;

    @Mock
    private QuoteComputer quoteComputer;

    @Mock
    private InstrumentCache instrumentCache;

    @InjectMocks
    private OrderComputer orderComputer;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPreProcessTradeCommand_sellCommandOneShareNoAdjustments_expectOrderOK() throws RobinhoodException, ExecutionException {

        when(brokerCache.getAssets()).thenReturn(ImmutableList.of(new Asset("AMZN", 1, 900d, new Quote())));
        when(walletOrderRegistry.findUnmatchedFilledBuyOrdersForSymbol(any())).thenReturn(Collections.emptyList());
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(Collections.emptyList());
        when(instrumentCache.getValidSymbols()).thenReturn(ImmutableSet.of("AMZN"));
        when(brokerCache.getAccountBalance()).thenReturn(new MarginBalances(100, 0, 123, 4321.0));
        when(walletComputer.computeBuyingPower(anySet())).thenReturn(500d);

        final OrderStatus status = orderComputer.preProcessTradeCommand(new TradeCommand(TradeAction.SELL, "AMZN"),
                                                                        ImmutableSet.of(new Voter("michrob", "twitch", "#stockstream", false)));

    assertEquals(OrderStatus.OK, status);
}

    @Test
    public void testPreProcessTradeCommand_sellCommandUnownedExcessCash_expectExcessCash() throws RobinhoodException, ExecutionException {

        when(brokerCache.getAssets()).thenReturn(ImmutableList.of(new Asset("AMZN", 1, 900d, new Quote())));
        when(walletOrderRegistry.findUnmatchedFilledBuyOrdersForSymbol(any())).thenReturn(Collections.emptyList());
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(Collections.emptyList());
        when(instrumentCache.getValidSymbols()).thenReturn(ImmutableSet.of("AMZN"));
        when(brokerCache.getAccountBalance()).thenReturn(new MarginBalances(100, 0, 123, 4321.0));
        when(walletComputer.computeBuyingPower(anySet())).thenReturn(50d);

        final OrderStatus status = orderComputer.preProcessTradeCommand(new TradeCommand(TradeAction.SELL, "AMZN"),
                                                                        ImmutableSet.of(new Voter("michrob", "twitch", "#stockstream", false)));

        assertEquals(OrderStatus.EXCESS_CASH_AVAILABLE, status);
    }


    @Test
    public void testPreProcessTradeCommand_sellCommandOneShareOnePending_expectNoShares() throws RobinhoodException, ExecutionException {
        final RobinhoodOrder pendingOrder = new RobinhoodOrder("123", "confirmed", "0", "1", "1", "sell", "1", "AMZN", "{}");

        when(walletOrderRegistry.findUnmatchedFilledBuyOrdersForSymbol(any())).thenReturn(Collections.emptyList());
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(ImmutableList.of(pendingOrder));
        when(instrumentCache.getValidSymbols()).thenReturn(ImmutableSet.of("AMZN"));

        final OrderStatus status = orderComputer.preProcessTradeCommand(new TradeCommand(TradeAction.SELL, "AMZN"),
                                                                        ImmutableSet.of(new Voter("michrob", "twitch", "#stockstream", false)));
        assertEquals(OrderStatus.NO_SHARES, status);
    }

    @Test
    public void testPreProcessTradeCommand_sellCommandTwoSharesOnePendingOnePlayerOwned_expectNoShares() throws RobinhoodException, ExecutionException {
        final RobinhoodOrder pendingOrder = new RobinhoodOrder("123", "confirmed", "0", "1", "1", "sell", "1", "AMZN", "{}");
        final WalletOrder playerOwned = new WalletOrder(1, "123", "0", "sell", "1", "mike", null, "AMZN");

        when(walletOrderRegistry.findUnmatchedFilledBuyOrdersForSymbol(any())).thenReturn(ImmutableList.of(playerOwned));
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(ImmutableList.of(pendingOrder));
        when(instrumentCache.getValidSymbols()).thenReturn(ImmutableSet.of("AMZN"));

        final OrderStatus status = orderComputer.preProcessTradeCommand(new TradeCommand(TradeAction.SELL, "AMZN"),
                                                                        ImmutableSet.of(new Voter("michrob", "twitch", "#stockstream", false)));

        assertEquals(OrderStatus.NO_SHARES, status);
    }

    @Test
    public void testPreProcessTradeCommand_sellCommandTwoSharesTwoPendingOneForSymbol_expectOK() throws RobinhoodException, ExecutionException {
        final RobinhoodOrder pendingOrder1 = new RobinhoodOrder("123", "confirmed", "0", "1", "1", "sell", "1", "AMZN", "{}");
        final RobinhoodOrder pendingOrder2 = new RobinhoodOrder("123", "confirmed", "0", "1", "1", "sell", "1", "GOOG", "{}");

        final Quote quote = new Quote();
        quote.setLast_trade_price(900.0);

        when(brokerCache.getAssets()).thenReturn(ImmutableList.of(new Asset("AMZN", 2, 900d, new Quote())));
        when(walletOrderRegistry.findUnmatchedFilledBuyOrdersForSymbol(any())).thenReturn(ImmutableList.of());
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(ImmutableList.of(pendingOrder1, pendingOrder2));
        when(instrumentCache.getValidSymbols()).thenReturn(ImmutableSet.of("AMZN"));
        when(brokerCache.getAccountBalance()).thenReturn(new MarginBalances(100, 0, 123, 4321.0));
        when(walletComputer.computeBuyingPower(anySet())).thenReturn(500d);

        final OrderStatus status = orderComputer.preProcessTradeCommand(new TradeCommand(TradeAction.SELL, "AMZN"),
                                                                        ImmutableSet.of(new Voter("michrob", "twitch", "#stockstream", false)));

        assertEquals(OrderStatus.OK, status);
    }

    @Test
    public void testPreProcessTradeCommand_buyCommandNoCash_expectCantAfford() throws RobinhoodException, ExecutionException {
        final Quote quote = new Quote();
        quote.setLast_trade_price(900.0);

        when(quoteComputer.computeMostRecentPrice(any())).thenReturn(900.0);
        when(brokerCache.getAccountBalance()).thenReturn(new MarginBalances(100, 0, 123, 4321.0));
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(instrumentCache.getValidSymbols()).thenReturn(ImmutableSet.of("AMZN"));

        final OrderStatus status = orderComputer.preProcessTradeCommand(new TradeCommand(TradeAction.BUY, "AMZN"),
                                                                        ImmutableSet.of(new Voter("michrob", "twitch", "#stockstream", false)));

        assertEquals(OrderStatus.CANT_AFFORD, status);
    }

    @Test
    public void testPreProcessTradeCommand_buyCommandEnoughCash_expectOk() throws RobinhoodException, ExecutionException {
        final Quote quote = new Quote();
        quote.setLast_trade_price(900.0);

        when(brokerCache.getAccountBalance()).thenReturn(new MarginBalances(5000f, 0, 123, 4321.0));
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(quoteComputer.computeMostRecentPrice(any())).thenReturn(2.49d);
        when(instrumentCache.getValidSymbols()).thenReturn(ImmutableSet.of("AMZN"));

        final OrderStatus status = orderComputer.preProcessTradeCommand(new TradeCommand(TradeAction.BUY, "AMZN"),
                                                                        ImmutableSet.of(new Voter("michrob", "twitch", "#stockstream", false)));

        assertEquals(OrderStatus.OK, status);
    }

    @Test
    public void testPreProcessWalletCommand_sellCommandLimitOkNoPending_expectOk() throws RobinhoodException, ExecutionException {
        final Quote quote = new Quote();
        quote.setLast_trade_price(900.0);

        final WalletOrder playerOwned = new WalletOrder(1, "123", "0", "sell", "1", "mike", null, "AMZN");

        when(brokerCache.getAssets()).thenReturn(ImmutableList.of(new Asset("AMZN", 1, 900d, new Quote())));
        when(walletOrderRegistry.findUnmatchedFilledBuyOrders(any(), any())).thenReturn(ImmutableList.of(playerOwned));
        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 1000d, 0d, 0d));
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(Collections.emptyList());
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(instrumentCache.getSymbolToInstrument()).thenReturn(ImmutableMap.of("AMZN", new InstrumentStub("", "AMZN", "", 0f, true, 0f)));

        final WalletCommand walletCommand = new WalletCommand(WalletAction.SELL, 1, "AMZN", 900d);
        final OrderStatus status = orderComputer.preProcessWalletCommand("twitch:michrob", walletCommand);

        assertEquals(OrderStatus.OK, status);
    }

    @Test
    public void testPreProcessWalletCommand_sellCommandLimitOkNoPositions_expectNoShares() throws RobinhoodException, ExecutionException {
        final Quote quote = new Quote();
        quote.setLast_trade_price(900.0);

        when(brokerCache.getAssets()).thenReturn(ImmutableList.of(new Asset("AMZN", 1, 900d, new Quote())));
        when(walletOrderRegistry.findUnmatchedBuyOrders(any())).thenReturn(Collections.emptyList());
        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 1000d, 0d, 0d));
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(Collections.emptyList());
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(instrumentCache.getSymbolToInstrument()).thenReturn(ImmutableMap.of("AMZN", new InstrumentStub("", "AMZN", "", 0f, true, 0f)));

        final WalletCommand walletCommand = new WalletCommand(WalletAction.SELL, 1, "AMZN", 900d);
        final OrderStatus status = orderComputer.preProcessWalletCommand("twitch:michrob", walletCommand);

        assertEquals(OrderStatus.NO_SHARES, status);
    }

    @Test
    public void testPreProcessWalletCommand_sellCommandLimitOkNoAsset_expectNoShares() throws RobinhoodException, ExecutionException {
        final Quote quote = new Quote();
        quote.setLast_trade_price(900.0);

        when(brokerCache.getAssets()).thenReturn(Collections.emptyList());
        when(walletOrderRegistry.findUnmatchedBuyOrders(any())).thenReturn(Collections.emptyList());
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(Collections.emptyList());
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(instrumentCache.getSymbolToInstrument()).thenReturn(ImmutableMap.of("AMZN", new InstrumentStub("", "AMZN", "", 0f, true, 0f)));

        final WalletCommand walletCommand = new WalletCommand(WalletAction.SELL, 1, "AMZN", 900d);

        final OrderStatus status = orderComputer.preProcessWalletCommand("twitch:michrob", walletCommand);

        assertEquals(OrderStatus.NO_SHARES, status);
    }

    @Test
    public void testPreProcessWalletCommand_buyCommandBadLimit_expectBadLimit() throws RobinhoodException, ExecutionException {
        final Quote quote = new Quote();
        quote.setLast_trade_price(900.0);

        when(brokerCache.getAssets()).thenReturn(Collections.emptyList());
        when(walletOrderRegistry.findUnmatchedBuyOrders(any())).thenReturn(Collections.emptyList());
        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 1000d, 0d, 0d));
        when(brokerCache.getAccountBalance()).thenReturn(new MarginBalances(5000f, 0, 123, 4321.0));
        when(walletComputer.computeSpendingBalance(any())).thenReturn(5000d);
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(Collections.emptyList());
        when(instrumentCache.getSymbolToInstrument()).thenReturn(ImmutableMap.of("AMZN", new InstrumentStub("", "AMZN", "", 0f, true, 0f)));
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(quoteComputer.computeMostRecentPrice(any())).thenReturn(900.0);

        final WalletCommand walletCommand = new WalletCommand(WalletAction.BUY, 1, "AMZN", 2000);
        final OrderStatus status = orderComputer.preProcessWalletCommand("twitch:michrob", walletCommand);

        assertEquals(OrderStatus.BAD_LIMIT, status);
    }

    @Test
    public void testPreProcessWalletCommand_buyCommand5CentIntervalEvenLimit_expectGoodLimit() throws RobinhoodException, ExecutionException {
        final Quote quote = new Quote();
        quote.setLast_trade_price(2.30);

        when(brokerCache.getAssets()).thenReturn(Collections.emptyList());
        when(walletOrderRegistry.findUnmatchedBuyOrders(any())).thenReturn(Collections.emptyList());
        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 1000d, 0d, 0d));
        when(brokerCache.getAccountBalance()).thenReturn(new MarginBalances(5000f, 0, 123, 4321.0));
        when(walletComputer.computeSpendingBalance(any())).thenReturn(5000d);
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(Collections.emptyList());
        when(instrumentCache.getSymbolToInstrument()).thenReturn(ImmutableMap.of("AMZN", new InstrumentStub("", "AMZN", "", 0f, true, 0.05f)));
        when(brokerCache.getQuoteForSymbol(any())).thenReturn(quote);
        when(quoteComputer.computeMostRecentPrice(any())).thenReturn(2.32);

        final WalletCommand walletCommand = new WalletCommand(WalletAction.BUY, 1, "AMZN", 2.30);
        final OrderStatus status = orderComputer.preProcessWalletCommand("twitch:michrob", walletCommand);

        assertEquals(OrderStatus.OK, status);
    }

    @Test
    public void testPreProcessWalletCommand_buyCommandWalletBalanceTooLow_expectCantAfford() throws RobinhoodException, ExecutionException {
        when(brokerCache.getAssets()).thenReturn(Collections.emptyList());
        when(walletOrderRegistry.findUnmatchedBuyOrders(any())).thenReturn(Collections.emptyList());
        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 1000d, 0d, 0d));
        when(broker.getMarginBalances()).thenReturn(new MarginBalances(5000f, 0, 123, 4321.0));
        when(walletComputer.computeSpendingBalance(any())).thenReturn(400d);
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(Collections.emptyList());

        final WalletCommand walletCommand = new WalletCommand(WalletAction.BUY, 1, "AMZN", 900);

        final OrderStatus status = orderComputer.preProcessWalletCommand("twitch:michrob", walletCommand);

        assertEquals(OrderStatus.BALANCE_TOO_LOW, status);
    }

    @Test
    public void testPreProcessWalletCommand_buyCommandTotalCashTooLow_expectCantAfford() throws RobinhoodException, ExecutionException {
        when(brokerCache.getAssets()).thenReturn(Collections.emptyList());
        when(walletOrderRegistry.findUnmatchedBuyOrders(any())).thenReturn(Collections.emptyList());
        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 1000d, 0d, 0d));
        when(brokerCache.getAccountBalance()).thenReturn(new MarginBalances(200, 0, 123, 4321.0));
        when(walletComputer.computeSpendingBalance(any())).thenReturn(4000d);
        when(robinhoodOrderRegistry.retrievePendingRobinhoodOrders(any())).thenReturn(Collections.emptyList());

        final WalletCommand walletCommand = new WalletCommand(WalletAction.BUY, 1, "AMZN", 900);
        final OrderStatus status = orderComputer.preProcessWalletCommand("twitch:michrob", walletCommand);

        assertEquals(OrderStatus.CANT_AFFORD, status);
    }

    @Test
    public void testCalculateBuyOrderCeiling_somePriceNotAfterHours_expectLessThan6Percent() throws RobinhoodException {
        final Quote quote = new Quote();
        quote.setLast_trade_price(4.20);

        when(timeComputer.isAfterHours()).thenReturn(false);
        when(quoteComputer.computeMostRecentPrice(any())).thenReturn(4.20d);

        final double ceiling = orderComputer.calculateBuyOrderCeiling(quote);

        assertTrue(ceiling > 4.20d && ceiling < 4.45d);
    }

    @Test
    public void testCalculateBuyOrderCeiling_somePriceIsAfterHours_expectLessThan6Percent() throws RobinhoodException {
        final Quote quote = new Quote();
        quote.setLast_trade_price(4.20);

        when(timeComputer.isAfterHours()).thenReturn(true);
        when(quoteComputer.computeMostRecentPrice(any())).thenReturn(4.20d);

        final double ceiling = orderComputer.calculateBuyOrderCeiling(quote);

        assertTrue(ceiling > 4.20d && ceiling < 4.25d);
    }

    @Test
    public void testConstructLimitOrderString_extraDigits_expectRoundedAndTruncated() throws RobinhoodException {
        String limit = orderComputer.constructLimitOrderString(231.502, .00f);
        assertEquals("231.50", limit);

        limit = orderComputer.constructLimitOrderString(2323451.501234, .02f);
        assertEquals("2323451.52", limit);

        limit = orderComputer.constructLimitOrderString(2323451.541234, .05f);
        assertEquals("2323451.55", limit);

        limit = orderComputer.constructLimitOrderString(2323451.591234, .05f);
        assertEquals("2323451.55", limit);
    }

    @Test
    public void testPreProcessWalletCommand_negativeSend_expectNotSent() throws ExecutionException, RobinhoodException {
        final OrderStatus orderStatus = orderComputer.preProcessWalletCommand("twitch:michrob", new WalletCommand(WalletAction.SEND, 1, "twitch:player2", -100d));

        assertEquals(OrderStatus.INVALID_COMMAND, orderStatus);
    }

    @Test
    public void testPreProcessWalletCommand_samePlayer_expectNotSent() throws ExecutionException, RobinhoodException {
        when(walletRegistry.getWallet(any())).thenReturn(new Wallet("twitch:michrob", 1000d, 0d, 0d));

        final OrderStatus orderStatus = orderComputer.preProcessWalletCommand("twitch:michrob", new WalletCommand(WalletAction.SEND, 1, "twitch:michrob", 10d));

        assertEquals(OrderStatus.INVALID_COMMAND, orderStatus);
    }

    private RobinhoodOrder createRobinhoodOrder(final String side, final String symbol, final String id, final String created_at) {
        final RobinhoodOrder robinhoodOrder = new RobinhoodOrder(id, "filled", created_at, "100.55", "101.25", side, "1", symbol, "");
        robinhoodOrder.setExecutions(ImmutableList.of(new Execution("1", created_at)));
        return robinhoodOrder;
    }

}
