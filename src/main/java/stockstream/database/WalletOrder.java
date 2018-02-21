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
@Table(name = "WalletOrders")
public class WalletOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "n")
    private Integer n;

    @Column(name = "id")
    private String id;

    @Column(name = "created_at")
    private String created_at;

    @Column(name = "side")
    private String side;

    @Column(name = "quantity")
    private String quantity;

    @Column(name = "platform_username")
    private String platform_username;

    @Column(name = "sell_order_id")
    private String sell_order_id;

    @Column(name = "symbol")
    private String symbol;

    public WalletOrder(final String symbol, final Order order, final String platform_username) {
        this.id = DigestUtils.sha1Hex(order.getId());
        this.side = order.getSide();
        this.quantity = order.getQuantity();
        this.created_at = order.getCreated_at();
        this.platform_username = platform_username;
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return id;
    }

}
