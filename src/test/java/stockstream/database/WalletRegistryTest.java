package stockstream.database;


import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import stockstream.TestDataUtils;
import stockstream.spring.DatabaseBeans;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class WalletRegistryTest {

    private static WalletRegistry walletRegistry;
    private static WalletOrderRegistry walletOrderRegistry;

    private static RobinhoodOrderRegistry robinhoodOrderRegistry;

    @BeforeClass
    public static void setupTest() throws Exception {
        final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

        context.register(DatabaseBeans.class);
        context.refresh();

        walletRegistry = context.getBean(WalletRegistry.class);
        walletOrderRegistry = context.getBean(WalletOrderRegistry.class);
        robinhoodOrderRegistry = context.getBean(RobinhoodOrderRegistry.class);
    }

    @Before
    public void resetDatabaseTables() throws Exception {
        walletOrderRegistry.eraseTables();
        robinhoodOrderRegistry.eraseTables();
    }

    @Test
    public void testGetWallet_walletDoesNotExist_expectEmptyWalletReturned() {
        final Wallet databaseWallet = walletRegistry.getWallet("does_not_exist");

        assertEquals(0, databaseWallet.getRealizedReturn(), .001);
        assertEquals(0, databaseWallet.getRealizedDecimalReturn(), .001);
        assertEquals(0, databaseWallet.getUnrealizedDollarsSpent(), .001);
    }

    @Test
    public void testGetWallet_walletExists_expectWalletReturned() {
        final double realizedReturn = new Random().nextDouble();
        final Wallet expectedWallet = new Wallet("test", realizedReturn, realizedReturn*2, realizedReturn*3);

        walletRegistry.updateWallets(ImmutableSet.of(expectedWallet));

        final Wallet databaseWallet = walletRegistry.getWallet("test");

        assertEquals(expectedWallet.getRealizedReturn(), databaseWallet.getRealizedReturn(), .001);
        assertEquals(expectedWallet.getRealizedDecimalReturn(), databaseWallet.getRealizedDecimalReturn(), .001);
        assertEquals(expectedWallet.getUnrealizedDollarsSpent(), databaseWallet.getUnrealizedDollarsSpent(), .001);
    }

    @Test
    public void testFindNextSellableBuyOrder_orderExists_expectOrderReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("test", "buy", "AMZN", "123", null, "2017-05-30");
        final RobinhoodOrder robinhoodOrder = TestDataUtils.createRobinhoodOrder("buy", "AMZN", "123", "2017-05-30");

        walletOrderRegistry.saveWalletOrder(walletOrder);
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);

        final Optional<WalletOrder> databaseWalletOrder = walletOrderRegistry.findNextSellableBuyOrder("test", "AMZN");

        assertEquals(walletOrder.getId(), databaseWalletOrder.get().getId());
        assertEquals(walletOrder.getSymbol(), databaseWalletOrder.get().getSymbol());
    }

    @Test
    public void testFindNextSellableBuyOrder_noOrder_expectEmptyReturn() {
        final Optional<WalletOrder> databaseWalletOrder = walletOrderRegistry.findNextSellableBuyOrder("test", "AAPL");

        assertEquals(false, databaseWalletOrder.isPresent());
    }

    @Test
    public void testFindUnmatchedFilledBuyOrdersForSymbol_orderExists_expectOrderReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("test", "buy", "GOOG", "123", null, "2017-05-30");
        final RobinhoodOrder robinhoodOrder = TestDataUtils.createRobinhoodOrder("buy", "GOOG", "123", "2017-05-30");

        walletOrderRegistry.saveWalletOrder(walletOrder);
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnmatchedFilledBuyOrdersForSymbol("GOOG");

        assertEquals(1, databaseWalletOrders.size());
    }

    @Test
    public void testFindUnmatchedFilledBuyOrdersForSymbol_orderExistsStillPending_expectNotReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("test", "buy", "GOOG", "123", null, "2017-05-30");
        final RobinhoodOrder robinhoodOrder = TestDataUtils.createRobinhoodOrder("buy", "GOOG", "123", "2017-05-30");
        robinhoodOrder.setState("confirmed");

        walletOrderRegistry.saveWalletOrder(walletOrder);
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnmatchedFilledBuyOrdersForSymbol("GOOG");

        assertEquals(0, databaseWalletOrders.size());
    }

    @Test
    public void testFindUnmatchedFilledBuyOrdersForSymbol_orderExistsSold_expectNotReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("test", "buy", "GOOG", "123", "321", "2017-05-30");
        final RobinhoodOrder robinhoodOrder = TestDataUtils.createRobinhoodOrder("buy", "GOOG", "123", "2017-05-30");

        walletOrderRegistry.saveWalletOrder(walletOrder);
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnmatchedFilledBuyOrdersForSymbol("GOOG");

        assertEquals(0, databaseWalletOrders.size());
    }

    @Test
    public void testFindUnmatchedFilledBuyOrders_orderExists_expectOrderReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("test", "buy", "GOOG", "123", null, "2017-05-30");
        final RobinhoodOrder robinhoodOrder = TestDataUtils.createRobinhoodOrder("buy", "GOOG", "123", "2017-05-30");

        walletOrderRegistry.saveWalletOrder(walletOrder);
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnmatchedFilledBuyOrders("test", "GOOG");

        assertEquals(1, databaseWalletOrders.size());
    }

    @Test
    public void testFindUnmatchedFilledBuyOrders_orderExistsDifferentPlayer_expectOrderNotReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("player1", "buy", "GOOG", "123", null, "2017-05-30");
        final RobinhoodOrder robinhoodOrder = TestDataUtils.createRobinhoodOrder("buy", "GOOG", "123", "2017-05-30");

        walletOrderRegistry.saveWalletOrder(walletOrder);
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnmatchedFilledBuyOrders("player2", "GOOG");

        assertEquals(0, databaseWalletOrders.size());
    }

    @Test
    public void testFindUnmatchedFilledBuyOrders_orderExistsSamePlayerSold_expectOrderNotReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("player1", "buy", "GOOG", "123", "321", "2017-05-30");
        final RobinhoodOrder robinhoodOrder = TestDataUtils.createRobinhoodOrder("buy", "GOOG", "123", "2017-05-30");

        walletOrderRegistry.saveWalletOrder(walletOrder);
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnmatchedFilledBuyOrders("player1", "GOOG");

        assertEquals(0, databaseWalletOrders.size());
    }

    @Test
    public void testFindUnsoldBuyOrders_orderExistsUnsold_expectReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("player1", "buy", "GOOG", "123", null, "2017-05-30");
        final RobinhoodOrder robinhoodOrder = TestDataUtils.createRobinhoodOrder("buy", "GOOG", "123", "2017-05-30");

        walletOrderRegistry.saveWalletOrder(walletOrder);
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnsoldOrPendingBuyOrders("player1");

        assertEquals(1, databaseWalletOrders.size());
    }

    @Test
    public void testFindUnsoldBuyOrders_orderExistsUnSoldFailed_expectNotReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("player1", "buy", "GOOG", "123", null, "2017-05-30");
        final RobinhoodOrder robinhoodOrder = TestDataUtils.createRobinhoodOrder("buy", "GOOG", "123", "2017-05-30");
        robinhoodOrder.setState("failed");

        walletOrderRegistry.saveWalletOrder(walletOrder);
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnsoldOrPendingBuyOrders("player1");

        assertEquals(0, databaseWalletOrders.size());
    }

    @Test
    public void testFindUnsoldBuyOrders_orderExistsUnSoldCancelled_expectNotReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("player1", "buy", "GOOG", "123", null, "2017-05-30");
        final RobinhoodOrder robinhoodOrder = TestDataUtils.createRobinhoodOrder("buy", "GOOG", "123", "2017-05-30");
        robinhoodOrder.setState("cancelled");

        walletOrderRegistry.saveWalletOrder(walletOrder);
        robinhoodOrderRegistry.saveRobinhoodOrder(robinhoodOrder);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnsoldOrPendingBuyOrders("player1");

        assertEquals(0, databaseWalletOrders.size());
    }

    @Test
    public void testFindUnmatchedBuyOrders_orderExistsUnmatched_expectReturned() {
        final WalletOrder walletOrder = TestDataUtils.createWalletOrder("player1", "buy", "GOOG", "123", null, "2017-05-30");

        walletOrderRegistry.saveWalletOrder(walletOrder);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnmatchedBuyOrders("player1");

        assertEquals(1, databaseWalletOrders.size());
    }

    @Test
    public void testFindUnmatchedBuyOrders_orderExistsOneMatchedOneUnmatched_expectOneReturned() {
        final WalletOrder walletOrder1 = TestDataUtils.createWalletOrder("player1", "buy", "GOOG", "123", "321", "2017-05-30");
        final WalletOrder walletOrder2 = TestDataUtils.createWalletOrder("player1", "buy", "GOOG", "124", null, "2017-05-30");

        walletOrderRegistry.saveWalletOrder(walletOrder1);
        walletOrderRegistry.saveWalletOrder(walletOrder2);

        final List<WalletOrder> databaseWalletOrders = walletOrderRegistry.findUnmatchedBuyOrders("player1");

        assertEquals(1, databaseWalletOrders.size());
    }
}
