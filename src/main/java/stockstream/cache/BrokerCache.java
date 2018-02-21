package stockstream.cache;

import com.cheddar.robinhood.RobinhoodAPI;
import com.cheddar.robinhood.data.MarginBalances;
import com.cheddar.robinhood.data.MarketState;
import com.cheddar.robinhood.data.Quote;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.api.Position;
import stockstream.api.StockStreamAPI;
import stockstream.computer.AssetComputer;
import stockstream.data.OrderResult;
import stockstream.database.Asset;
import stockstream.database.AssetRegistry;
import stockstream.database.RobinhoodAccountRegistry;
import stockstream.logic.PubSub;
import stockstream.logic.Scheduler;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BrokerCache {

    private class UpdateAccountRunnable implements Runnable {

        @Override
        public void run() {
            try {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        updateAssets();
                    }
                }).run();
            } catch (final Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    // Day of year -> DayObject
    private LoadingCache<Integer, MarketState> marketStateCache =
            CacheBuilder.newBuilder().build(new CacheLoader<Integer, MarketState>() {
                @Override
                public MarketState load(final Integer key) throws Exception {
                    DateTime thisDay = new DateTime(0).withYear(new DateTime().getYear()).withDayOfYear(key);
                    MarketState day = broker.getMarketStateForDate(thisDay);
                    return day;
                }
            });

    private LoadingCache<String, Quote> symbolQuoteCache =
            CacheBuilder.newBuilder()
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .build(new CacheLoader<String, Quote>() {
                            @Override
                            public Quote load(final String symbol) throws Exception {
                                return broker.getQuote(symbol);
                            }
                        });


    private LoadingCache<String, Map<String, List<Position>>> positionsCache =
            CacheBuilder.newBuilder()
                        .expireAfterWrite(15, TimeUnit.SECONDS)
                        .build(new CacheLoader<String, Map<String, List<Position>>>() {
                            @Override
                            public Map<String, List<Position>> load(final String key) throws Exception {
                                final List<Position> positions = stockStreamAPI.getOpenPositions();
                                final Map<String, List<Position>> symbolToPositions = new HashMap<>();

                                positions.forEach(position -> symbolToPositions.computeIfAbsent(position.getBuyOrder().getSymbol(), list -> new ArrayList<>()).add(position));

                                return symbolToPositions;
                            }
                        });


    private final UpdateAccountRunnable updateAccountRunnable = new UpdateAccountRunnable();

    private Collection<Asset> assets = ConcurrentHashMap.newKeySet();

    private Map<String, Asset> symbolToAsset = new ConcurrentHashMap<>();

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private PubSub pubSub;

    @Autowired
    private AssetComputer assetComputer;

    @Autowired
    private RobinhoodAPI broker;

    @Autowired
    private AssetRegistry assetRegistry;

    @Autowired
    private RobinhoodAccountRegistry robinhoodAccountRegistry;

    @Autowired
    private StockStreamAPI stockStreamAPI;

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(updateAccountRunnable, 5L, 10, TimeUnit.SECONDS);
        pubSub.subscribeRunnableToClassType(updateAccountRunnable, OrderResult.class);
    }

    public Quote getQuoteForSymbol(final String symbol) throws ExecutionException {
        return symbolQuoteCache.get(symbol);
    }

    public Collection<Asset> getAssets() {
        return Collections.unmodifiableCollection(assets);
    }

    public Map<String, List<Position>> getSymbolToPositions() {
        try {
            return positionsCache.get("positions");
        } catch (ExecutionException e) {
            log.warn(e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    public Map<String, Asset> getSymbolToAsset() {
        return Collections.unmodifiableMap(symbolToAsset);
    }

    public MarginBalances getAccountBalance() {
        return robinhoodAccountRegistry.getAccountInfo().getBalances();
    }

    public MarketState getMarketState(final DateTime forDate) {
        try {
            return marketStateCache.get(forDate.getDayOfYear());
        } catch (final Exception ex) {
            log.warn(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    public double getAccountTotalAssets() {
        return assets.stream().mapToDouble(asset -> assetComputer.computeAssetValue(asset)).sum();
    }

    public synchronized double getAccountNetWorth() {
        return robinhoodAccountRegistry.getAccountInfo().getPortfolio().getMarket_value();
    }

    private synchronized void updateAssets() {
        assets = this.assetRegistry.getAssets();

        this.symbolToAsset.clear();
        this.assets.forEach(asset -> symbolToAsset.put(asset.getSymbol(), asset));
    }

}
