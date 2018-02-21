package stockstream.database;

import com.cheddar.robinhood.data.Instrument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RobinhoodInstruments")
public class InstrumentStub {

    @Id
    @Column(name = "url")
    private String url;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "name")
    private String name;

    @Column(name = "day_trade_ratio")
    private float day_trade_ratio;

    @Column(name = "tradeable")
    private boolean tradeable;

    @Column(name = "min_tick_size")
    private float min_tick_size;

    public InstrumentStub(final Instrument instrument) {
        this.url = instrument.getUrl();
        this.symbol = instrument.getSymbol();
        this.name = instrument.getName();
        this.day_trade_ratio = instrument.getDay_trade_ratio();
        this.tradeable = instrument.isTradeable();
        this.min_tick_size = instrument.getMin_tick_size();
    }

}
