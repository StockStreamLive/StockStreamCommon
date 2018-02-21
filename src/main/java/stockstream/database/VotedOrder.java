package stockstream.database;

import com.cheddar.robinhood.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;

import javax.persistence.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "VotedOrders")
public class VotedOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "n")
    private Integer n;

    @Column(name = "id")
    private String id;

    @Column(name = "sell_order_id")
    private String sell_order_id;

    @Column(name = "side")
    private String side;

    @Column(name = "symbol")
    private String symbol;

    public VotedOrder(final RobinhoodOrder robinhoodOrder) {
        this.id = robinhoodOrder.getId();
        this.side = robinhoodOrder.getSide();
        this.symbol = robinhoodOrder.getSymbol();
    }

    public VotedOrder(final String symbol, final Order order) {
        this.id = DigestUtils.sha1Hex(order.getId());
        this.side = order.getSide();
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return id;
    }

}
