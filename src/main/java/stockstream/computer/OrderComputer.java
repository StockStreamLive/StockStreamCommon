package stockstream.computer;

import com.cheddar.robinhood.data.MarginBalances;
import com.cheddar.robinhood.data.Quote;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.cheddar.util.MathUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.cache.BrokerCache;
import stockstream.cache.InstrumentCache;
import stockstream.data.OrderStatus;
import stockstream.data.TradeCommand;
import stockstream.data.Voter;
import stockstream.data.WalletCommand;
import stockstream.database.*;
import stockstream.util.TimeUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class OrderComputer {

    @Autowired
    private TimeComputer timeComputer;

    @Autowired
    private AssetComputer assetComputer;

    @Autowired
    private QuoteComputer quoteComputer;

    @Autowired
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @Autowired
    private WalletOrderRegistry walletOrderRegistry;

    @Autowired
    private WalletRegistry walletRegistry;

    @Autowired
    private WalletComputer walletComputer;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private InstrumentCache instrumentCache;

    public OrderStatus preProcessTradeCommand(final TradeCommand tradeCommand, final Set<Voter> voters) throws RobinhoodException, ExecutionException {
        if (!instrumentCache.getValidSymbols().contains(tradeCommand.getParameter())) {
            return OrderStatus.BAD_TICKER;
        }

        final Quote quote = brokerCache.getQuoteForSymbol(tradeCommand.getParameter());

        switch (tradeCommand.getAction()) {
            case BUY: {

                final double buyLimit = calculateBuyOrderCeiling(quote);

                final MarginBalances marginBalances = brokerCache.getAccountBalance();

                if (buyLimit > marginBalances.getUnallocated_margin_cash()) {
                    return OrderStatus.CANT_AFFORD;
                }

                return OrderStatus.OK;

            } case SELL: {

                final String symbol = tradeCommand.getParameter();

                final Collection<Asset> allAssets = this.brokerCache.getAssets();
                final Map<String, Asset> assetMap = allAssets.stream().collect(Collectors.toMap(Asset::getSymbol, asset -> asset));

                if (!assetMap.containsKey(symbol)) {
                    return OrderStatus.NO_SHARES;
                }

                final double globalBuyingPower = brokerCache.getAccountBalance().getUnallocated_margin_cash();
                final boolean playersOwnPosition = assetComputer.playersOwnPosition(symbol, voters.stream().map(Voter::getPlayerId).collect(Collectors.toSet()));
                final double playerVotingPower = walletComputer.computeBuyingPower(voters.stream().map(Voter::getPlayerId).collect(Collectors.toSet()));
                if (!playersOwnPosition && playerVotingPower < globalBuyingPower) {
                    return OrderStatus.EXCESS_CASH_AVAILABLE;
                }

                final List<WalletOrder> openPlayerOrders = walletOrderRegistry.findUnmatchedFilledBuyOrdersForSymbol(symbol);

                final List<RobinhoodOrder> pendingSales = robinhoodOrderRegistry.retrievePendingRobinhoodOrders(TimeUtil.getStartOfToday());

                final int pendingSaleShares = pendingSales.stream().filter(order -> symbol.equalsIgnoreCase(order.getSymbol()))
                                                                   .filter(order -> "sell".equalsIgnoreCase(order.getSide()))
                                                                   .mapToInt(robinhoodOrder -> (int)Double.parseDouble(robinhoodOrder.getQuantity())).sum();
                final int playerOwnedShares = openPlayerOrders.stream().mapToInt(wallet -> (int)Double.parseDouble(wallet.getQuantity())).sum();
                final int totalOwnedShares = assetMap.get(symbol).getShares();

                log.info("For {} have {} pendingSales {} playerOwned and {} totalOwned", symbol, pendingSaleShares, playerOwnedShares, totalOwnedShares);

                if (totalOwnedShares - playerOwnedShares - pendingSaleShares <= 0) {
                    return OrderStatus.NO_SHARES;
                }

                return OrderStatus.OK;

            } case SKIP: {
                break;
            } default: {
                log.warn("Invalid action type: {}", tradeCommand);
                break;
            }
        }

        return OrderStatus.OK;
    }

    public OrderStatus preProcessWalletCommand(final String player, final WalletCommand walletCommand) throws RobinhoodException, ExecutionException {
        final Wallet wallet = walletRegistry.getWallet(player);

        OrderStatus orderStatus = OrderStatus.OK;

        switch (walletCommand.getAction()) {
            case BUY: {
                final Quote quote = brokerCache.getQuoteForSymbol(walletCommand.getParameter());
                final InstrumentStub instrument = instrumentCache.getSymbolToInstrument().get(walletCommand.getParameter());

                final double buyLimit = walletCommand.getLimit();
                final double purchaseTotal = walletCommand.getLimit() * walletCommand.getQuantity();

                final double playerSpendingBalance = walletComputer.computeSpendingBalance(wallet);

                if (purchaseTotal > playerSpendingBalance) {
                    return OrderStatus.BALANCE_TOO_LOW;
                }

                final MarginBalances marginBalances = brokerCache.getAccountBalance();

                if (purchaseTotal > marginBalances.getUnallocated_margin_cash()) {
                    return OrderStatus.CANT_AFFORD;
                }

                final double mostRecentPrice = quoteComputer.computeMostRecentPrice(quote);

                final double buyCeiling = mostRecentPrice + (mostRecentPrice * .01);
                final double buyFloor = mostRecentPrice - (mostRecentPrice * .1);

                if (buyLimit > buyCeiling || buyLimit < buyFloor) {
                    return OrderStatus.BAD_LIMIT;
                }

                if (instrument.getMin_tick_size() > 0) {
                    final int value = (int)(instrument.getMin_tick_size() * 100);
                    final int decimal = (int)Math.round(buyLimit * 100);
                    if (decimal % value != 0) {
                        return OrderStatus.BAD_TICK_SIZE;
                    }
                }

                return OrderStatus.OK;

            } case SELL: {
                final Quote quote = brokerCache.getQuoteForSymbol(walletCommand.getParameter());
                final InstrumentStub instrument = instrumentCache.getSymbolToInstrument().get(walletCommand.getParameter());

                final double sellLimit = walletCommand.getLimit();
                final double mostRecentPrice = quoteComputer.computeMostRecentPrice(quote);

                final double sellFloor = mostRecentPrice - (mostRecentPrice * .01);

                if (walletCommand.getLimit() < sellFloor) {
                    return OrderStatus.BAD_LIMIT;
                }

                if (instrument.getMin_tick_size() > 0) {
                    final int value = (int)(instrument.getMin_tick_size() * 100);
                    final int decimal = (int)(sellLimit * 100);
                    if (decimal % value != 0) {
                        return OrderStatus.BAD_TICK_SIZE;
                    }
                }

                final Collection<Asset> allAssets = this.brokerCache.getAssets();
                final Map<String, Asset> assetMap = allAssets.stream().collect(Collectors.toMap(Asset::getSymbol, asset -> asset));

                if (!assetMap.containsKey(walletCommand.getParameter())) {
                    return OrderStatus.NO_SHARES;
                }

                final List<WalletOrder> openWalletOrders = walletOrderRegistry.findUnmatchedFilledBuyOrders(wallet.getPlatform_username(), walletCommand.getParameter());

                if (openWalletOrders.size() < walletCommand.getQuantity()) {
                    return OrderStatus.NO_SHARES;
                }

                break;
            } case SEND: {
                final double sendLimit = walletCommand.getLimit();

                if (sendLimit < 0) {
                    return OrderStatus.INVALID_COMMAND;
                }

                if (wallet.getPlatform_username().equalsIgnoreCase(walletCommand.getParameter())) {
                    return OrderStatus.INVALID_COMMAND;
                }

                final double playerSpendingBalance = walletComputer.computeSpendingBalance(wallet);

                if (sendLimit > playerSpendingBalance) {
                    return OrderStatus.BALANCE_TOO_LOW;
                }

                break;
            } default: {
                log.warn("Invalid action type: {}", walletCommand);
                break;
            }
        }

        return orderStatus;
    }

    public double calculateBuyOrderCeiling(final Quote quote) throws RobinhoodException {
        // https://archive.fo/hf0rt
        final double mostRecentPrice = quoteComputer.computeMostRecentPrice(quote);

        double ceilingPercentage = .05d;

        if (timeComputer.isAfterHours()) {
            ceilingPercentage = .001d;
        }

        if (mostRecentPrice >= 250) {
            ceilingPercentage = 0d;
        }

        return mostRecentPrice + (mostRecentPrice * ceilingPercentage);
    }

    public double calculateSellOrderFloor(final Quote quote) throws RobinhoodException {
        final double mostRecentPrice = quoteComputer.computeMostRecentPrice(quote);

        double floorPercentage = .03d;

        if (timeComputer.isAfterHours()) {
            floorPercentage = .001d;
        }

        if (quoteComputer.computeMostRecentPrice(quote) >= 250) {
            floorPercentage = 0d;
        }

        return mostRecentPrice - (mostRecentPrice * floorPercentage);
    }

    // Convert limit double to a string with exactly 2 decimal places as required by Robinhood.
    // This also applies "tick size" rules mandated by the SEC.
    // https://www.sec.gov/oiea/investor-alerts-bulletins/ia_ticksize.html
    public String constructLimitOrderString(final double limit, final float minTickSize) {
        final String minTickSizeStr = String.format("%.2f", minTickSize);
        final double orderLimit = MathUtil.roundToTick(limit, minTickSize);

        final String maxPriceStr = String.format("%.2f", orderLimit);

        final String dollars = maxPriceStr.split("\\.")[0];
        final String cents = maxPriceStr.split("\\.")[1];

        final String minTickSizeCents = minTickSizeStr.split("\\.")[1];

        final StringBuilder centsBuilder = new StringBuilder(cents);
        centsBuilder.setCharAt(1, minTickSizeCents.charAt(1));

        final String limitOrderString = dollars + "." + centsBuilder.toString();

        log.info("{} {} {} {} {} -> {}", limit, minTickSize, minTickSizeStr, orderLimit, maxPriceStr, limitOrderString);

        return limitOrderString;
    }

}
