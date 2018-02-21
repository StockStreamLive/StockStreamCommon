package stockstream.computer;

import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.data.Quote;
import com.cheddar.robinhood.exception.RobinhoodException;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import spark.utils.StringUtils;
import stockstream.api.Position;
import stockstream.cache.BrokerCache;
import stockstream.cache.InstrumentCache;
import stockstream.database.*;
import stockstream.util.MathUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class AssetComputer {

    @Autowired
    private RobinhoodAPI broker;

    @Autowired
    private InstrumentCache instrumentCache;

    @Autowired
    private BrokerCache brokerCache;

    @Autowired
    private WalletOrderRegistry walletOrderRegistry;

    @Autowired
    private RobinhoodOrderRegistry robinhoodOrderRegistry;

    @Autowired
    private QuoteComputer quoteComputer;

    public Collection<Asset> getAssetsOwnedByPlayer(final String platform_username) throws RobinhoodException {
        log.info("Getting owned assets.");

        final List<WalletOrder> walletOrders = walletOrderRegistry.findUnmatchedBuyOrders(platform_username);
        final Set<String> orderIds = walletOrders.stream().map(WalletOrder::getId).collect(Collectors.toSet());
        final List<RobinhoodOrder> robinhoodOrders = robinhoodOrderRegistry.retrieveRobinhoodOrdersById(orderIds);

        Collection<String> symbols = new ArrayList<>();
        for (final WalletOrder walletOrder : walletOrders) {
            final InstrumentStub instrument = this.instrumentCache.getSymbolToInstrument().get(walletOrder.getSymbol());
            if (instrument == null || StringUtils.isEmpty(instrument.getSymbol())) {
                continue;
            }
            symbols.add(instrument.getSymbol());
        }

        final List<Quote> quotes = broker.getQuotes(symbols);

        final Map<String, Quote> instrumentURLToQuote = new HashMap<>();
        quotes.forEach(quote -> instrumentURLToQuote.put(quote.getInstrument(), quote));

        final Map<String, Asset> ownedAssets = new HashMap<>();

        for (final RobinhoodOrder robinhoodOrder : robinhoodOrders) {
            if (!"filled".equalsIgnoreCase(robinhoodOrder.getState())) {
                continue;
            }

            final double shares = Double.parseDouble(robinhoodOrder.getQuantity());
            final double avgBuyPrice = Double.parseDouble(robinhoodOrder.getAverage_price());

            if (shares <= 0) {
                continue;
            }

            final InstrumentStub instrument = this.instrumentCache.getSymbolToInstrument().get(robinhoodOrder.getSymbol());

            if (instrument == null) {
                continue;
            }

            final String instrumentURL = instrument.getUrl();

            if (!instrumentURLToQuote.containsKey(instrumentURL)) {
                continue;
            }

            final String symbol = instrument.getSymbol();

            final Quote quote = instrumentURLToQuote.get(instrumentURL);

            final Asset existingAsset = ownedAssets.computeIfAbsent(symbol, asset -> new Asset(symbol, 0, 0, quote));

            final double totalCost = (existingAsset.getAvgBuyPrice() * existingAsset.getShares()) + (avgBuyPrice * shares);

            existingAsset.setShares((int) (shares + existingAsset.getShares()));
            existingAsset.setAvgBuyPrice(totalCost/existingAsset.getShares());
        }

        log.info("Got {} assets from Robinhood.", ownedAssets.size());

        return new ArrayList<>(ownedAssets.values());
    }

    public boolean isSymbol(final String token) {
        String tokenToCheck = token;
        if (tokenToCheck.startsWith("$")) {
            tokenToCheck = tokenToCheck.substring(1);
        }

        return this.instrumentCache.getValidSymbols().contains(tokenToCheck);
    }

    public List<Position> getWalletOwnedPositions(final String symbol, final String playerId) {
        final List<Position> positionsForSymbol = brokerCache.getSymbolToPositions().getOrDefault(symbol, Collections.emptyList());

        final List<Position> playerOwnedPosition = positionsForSymbol.stream()
                                                                     .filter(Position::isWalletOrder)
                                                                     .filter(position -> position.getLiablePlayers().contains(playerId))
                                                                     .collect(Collectors.toList());

        return playerOwnedPosition;
    }

    public List<Position> getPublicOwnedPositions(final String symbol, final String playerId) {
        final List<Position> positionsForSymbol = brokerCache.getSymbolToPositions().getOrDefault(symbol, Collections.emptyList());

        final List<Position> playerOwnedPosition = positionsForSymbol.stream()
                                                                     .filter(position -> !position.isWalletOrder())
                                                                     .filter(position -> position.getLiablePlayers().contains(playerId))
                                                                     .collect(Collectors.toList());

        return playerOwnedPosition;
    }

    public boolean playersOwnPosition(final String symbol, final Set<String> playerIds) {
        final List<Position> positionsForSymbol = brokerCache.getSymbolToPositions().getOrDefault(symbol, Collections.emptyList());

        final Set<String> playersWhoOwn = new HashSet<>();
        positionsForSymbol.forEach(position -> playersWhoOwn.addAll(position.getLiablePlayers()));

        final Set<String> intersectingPlayers = Sets.intersection(playerIds, playersWhoOwn);

        return intersectingPlayers.size() > 0;
    }

    public Map<String, Quote> loadSymbolToQuote(final Set<String> symbols) throws RobinhoodException {
        final Map<String, Quote> symbolToQuote = new ConcurrentHashMap<>();

        final List<Quote> quotes = this.broker.getQuotes(symbols);
        quotes.forEach(entry -> symbolToQuote.put(entry.getSymbol(), entry));

        return symbolToQuote;
    }

    public double computePercentReturn(final Asset asset) {
        if (asset.getAvgBuyPrice() <= 0) {
            return 100;
        }

        final double mostRecentPrice = quoteComputer.computeMostRecentPrice(asset.getQuote());

        return MathUtil.computePercentChange(asset.getAvgBuyPrice(), mostRecentPrice);
    }

    public double computeAssetValue(final Asset asset) {
        final double mostRecentPrice = quoteComputer.computeMostRecentPrice(asset.getQuote());

        return asset.getShares() * mostRecentPrice;
    }

    public double computeAssetCost(final Asset asset) {
        return asset.getAvgBuyPrice() * asset.getShares();
    }

}
