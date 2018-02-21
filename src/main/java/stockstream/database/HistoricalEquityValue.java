package stockstream.database;

import com.cheddar.robinhood.data.EquityHistorical;
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
@Table(name = "EquityValues")
public class HistoricalEquityValue {

    @Id
    @Column(name = "begins_at")
    private String begins_at;

    @Column(name = "adjusted_open_equity")
    private Double adjusted_open_equity;

    @Column(name = "adjusted_close_equity")
    private Double adjusted_close_equity;

    public HistoricalEquityValue(final EquityHistorical equityHistorical) {
        this.begins_at = equityHistorical.getBegins_at();
        this.adjusted_open_equity = equityHistorical.getAdjusted_open_equity();
        this.adjusted_close_equity = equityHistorical.getAdjusted_close_equity();
    }
}
