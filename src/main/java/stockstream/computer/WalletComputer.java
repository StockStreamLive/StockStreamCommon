package stockstream.computer;

import org.springframework.beans.factory.annotation.Autowired;
import stockstream.config.Config;
import stockstream.data.Constants;
import stockstream.database.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WalletComputer {

    @Autowired
    private WalletRegistry walletRegistry;

    @Autowired
    private WalletOrderRegistry walletOrderRegistry;

    @Autowired
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    public double computeSpendingBalance(final Wallet wallet) {
        final Collection<WalletOrder> walletOrders = walletOrderRegistry.findUnsoldOrPendingBuyOrders(wallet.getPlatform_username());

        final Set<String> orderIds = walletOrders.stream().map(WalletOrder::getId).collect(Collectors.toSet());
        final Collection<RobinhoodOrder> orders = robinhoodOrderRegistry.retrieveRobinhoodOrdersById(orderIds);

        double spentAmount = 0d;
        for (final RobinhoodOrder robinhoodOrder : orders) {
            if ("filled".equalsIgnoreCase(robinhoodOrder.getState())) {
                spentAmount += Double.valueOf(robinhoodOrder.getAverage_price());
            } else if (Constants.PENDING_ORDER_STATES.contains(robinhoodOrder.getState().toLowerCase())) {
                spentAmount += Double.valueOf(robinhoodOrder.getPrice());
            }
        }

        double spendingBalance = wallet.getRealizedReturn() - spentAmount;

        if (wallet.getUnrealizedDollarsSpent() > Config.MAX_INFLUENCED_BUY) {
            final double buyLimitOverage = wallet.getUnrealizedDollarsSpent() - Config.MAX_INFLUENCED_BUY;
            spendingBalance -= buyLimitOverage;
        }

        spendingBalance += wallet.getReceivedDollars() - wallet.getSentDollars();

        return spendingBalance;
    }

    public double computeBuyingPower(final Set<String> players) {
        final List<Wallet> wallets = walletRegistry.getWallets(players);

        double totalBuyingPower = 0;

        final Set<String> playersWithEmptyWallets = new HashSet<>(players);

        for (final Wallet wallet : wallets) {
            playersWithEmptyWallets.remove(wallet.getPlatform_username());
            final double buyingPower = computeBuyingPower(wallet);
            if (buyingPower > 0) {
                totalBuyingPower += buyingPower;
            }
        }

        totalBuyingPower += (playersWithEmptyWallets.size() * Config.MAX_INFLUENCED_BUY);

        return totalBuyingPower;
    }

    public double computeBuyingPower(final Wallet wallet) {
        final double buyingPower = Config.MAX_INFLUENCED_BUY - wallet.getUnrealizedDollarsSpent() - wallet.getRealizedReturn();
        return buyingPower;
    }
}
