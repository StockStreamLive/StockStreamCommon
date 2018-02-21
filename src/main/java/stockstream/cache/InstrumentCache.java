package stockstream.cache;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.database.InstrumentRegistry;
import stockstream.database.InstrumentStub;
import stockstream.logic.Scheduler;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class InstrumentCache {
    private static final Set<String> RESTRICTED_SYMBOLS = ImmutableSet.of();

    private final Set<String> validSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, InstrumentStub> urlToInstrument = new ConcurrentHashMap<>();
    private final Map<String, InstrumentStub> symbolToInstrument = new ConcurrentHashMap<>();

    @Autowired
    private InstrumentRegistry instrumentRegistry;

    @Autowired
    private Scheduler scheduler;

    @PostConstruct
    public void init() {
        scheduler.scheduleJob(this::loadRemoteInstruments, 0, 1, TimeUnit.HOURS);
    }

    private void loadRemoteInstruments() {
        final List<InstrumentStub> instruments = this.instrumentRegistry.getAllInstrumentStubs();

        updateSymbolMaps(instruments);

        log.info("InstrumentsCache constructed.");
    }

    private synchronized void updateSymbolMaps(final Collection<InstrumentStub> withInstruments) {
        validSymbols.clear();
        urlToInstrument.clear();
        symbolToInstrument.clear();

        withInstruments.forEach(instrument -> {
            if (RESTRICTED_SYMBOLS.contains(instrument.getSymbol())) {
                log.debug("Ignoring instrument {} because it's restricted.", instrument);
                return;
            }
            if (instrument.getDay_trade_ratio() > .25f) {
                log.debug("Ignoring instrument {} because it's too risky.", instrument);
                return;
            }
            if (!instrument.isTradeable()) {
                log.debug("Ignoring instrument {} because it's not tradeable.", instrument);
                return;
            }
            validSymbols.add(instrument.getSymbol());
            urlToInstrument.put(instrument.getUrl(), instrument);
            symbolToInstrument.put(instrument.getSymbol(), instrument);
        });
    }

    public Set<String> getValidSymbols() {
        return Collections.unmodifiableSet(validSymbols);
    }

    public Map<String, InstrumentStub> getUrlToInstrument() {
        return Collections.unmodifiableMap(urlToInstrument);
    }

    public Map<String, InstrumentStub> getSymbolToInstrument() {
        return Collections.unmodifiableMap(symbolToInstrument);
    }

}
