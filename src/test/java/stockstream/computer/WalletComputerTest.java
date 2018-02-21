package stockstream.computer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import stockstream.config.Config;
import stockstream.database.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class WalletComputerTest {

    @Mock
    private WalletRegistry walletRegistry;

    @Mock
    private WalletOrderRegistry walletOrderRegistry;

    @Mock
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @InjectMocks
    private WalletComputer walletComputer;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testComputeSpendingBalance_noPendingOrders_expectBalance() {

        final double balance = walletComputer.computeSpendingBalance(new Wallet("twitch:michrob", 100, 0d, 0d));

        assertEquals(100.0, balance, .001);
    }

    @Test
    public void testComputeSpendingBalance_onePendingOrders_expectBalanceMinusCost() {
        final double playerCash = 500.0;

        final RobinhoodOrder pendingOrder = new RobinhoodOrder();
        pendingOrder.setState("confirmed");
        pendingOrder.setPrice("100");

        when(robinhoodOrderRegistry.retrieveRobinhoodOrdersById(any())).thenReturn(ImmutableList.of(pendingOrder));

        final double balance = walletComputer.computeSpendingBalance(new Wallet("twitch:michrob", playerCash, 0d, 0d));

        assertEquals(400.0, balance, .001);
    }

    @Test
    public void testComputeSpendingBalance_twoPendingOrders_expectBalanceMinusCost() {
        final double playerCash = 500.0;

        final RobinhoodOrder pendingOrder1 = new RobinhoodOrder();
        pendingOrder1.setState("confirmed");
        pendingOrder1.setPrice("100");

        final RobinhoodOrder pendingOrder2 = new RobinhoodOrder();
        pendingOrder2.setState("unconfirmed");
        pendingOrder2.setPrice("150");

        when(robinhoodOrderRegistry.retrieveRobinhoodOrdersById(any())).thenReturn(ImmutableList.of(pendingOrder1, pendingOrder2));

        final double balance = walletComputer.computeSpendingBalance(new Wallet("twitch:michrob", playerCash, 0d, 0d));

        assertEquals(250.0, balance, .001);
    }

    @Test
    public void testComputeSpendingBalance_threePendingOrders_expectBalanceMinusCost() {
        final double playerCash = 500.0;

        final RobinhoodOrder pendingOrder1 = new RobinhoodOrder();
        pendingOrder1.setState("confirmed");
        pendingOrder1.setPrice("100");

        final RobinhoodOrder pendingOrder2 = new RobinhoodOrder();
        pendingOrder2.setState("unconfirmed");
        pendingOrder2.setPrice("150");

        final RobinhoodOrder pendingOrder3 = new RobinhoodOrder();
        pendingOrder3.setState("queued");
        pendingOrder3.setPrice("50");

        when(robinhoodOrderRegistry.retrieveRobinhoodOrdersById(any())).thenReturn(ImmutableList.of(pendingOrder1, pendingOrder2, pendingOrder3));

        final double balance = walletComputer.computeSpendingBalance(new Wallet("twitch:michrob", playerCash, 0d, 0d));

        assertEquals(200.0, balance, .001);
    }

    @Test
    public void testComputeSpendingBalance_oneFilledOrder_expectBalanceMinusCost() {
        final double playerCash = 400.0;

        final RobinhoodOrder pendingOrder = new RobinhoodOrder();
        pendingOrder.setState("filled");
        pendingOrder.setAverage_price("100");

        when(robinhoodOrderRegistry.retrieveRobinhoodOrdersById(any())).thenReturn(ImmutableList.of(pendingOrder));

        final double balance = walletComputer.computeSpendingBalance(new Wallet("twitch:michrob", playerCash, 0d, 0d));

        assertEquals(300.0, balance, .001);
    }

    @Test
    public void testComputeBuyingPower_walletExistsWithBuyingPower_expectBuyingPowerReturned() {
        when(walletRegistry.getWallets(any())).thenReturn(ImmutableList.of(new Wallet("player1", 500, .05, 300)));

        final double buyingPower = walletComputer.computeBuyingPower(ImmutableSet.of("player1"));


        assertEquals(Config.MAX_INFLUENCED_BUY - 300.0 - 500, buyingPower, .001);
    }

    @Test
    public void testComputeBuyingPower_walletDoesNotExist_expectMaxBuyingPowerReturned() {
        final double buyingPower = walletComputer.computeBuyingPower(ImmutableSet.of("player1"));

        assertEquals(Config.MAX_INFLUENCED_BUY, buyingPower, .001);
    }

}
