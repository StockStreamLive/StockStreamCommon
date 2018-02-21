package stockstream.database;

import com.cheddar.robinhood.data.Execution;
import com.cheddar.robinhood.data.Order;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;
import stockstream.util.JSONUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Collections;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RobinhoodOrders")
public class RobinhoodOrder {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "state")
    private String state;

    @Column(name = "created_at")
    private String created_at;

    @Column(name = "average_price")
    private String average_price;

    @Column(name = "price")
    private String price;

    @Column(name = "side")
    private String side;

    @Column(name = "quantity")
    private String quantity;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "executions")
    private String executions;

    public List<Execution> getExecutions() {
        return JSONUtil.deserializeString(executions, new TypeReference<List<Execution>>() { }).orElse(Collections.emptyList());
    }

    public void setExecutions(final List<Execution> executions){
        this.executions = JSONUtil.serializeObject(executions).orElse("[]");
    }

    public RobinhoodOrder(final JSONObject jsonOrder) {
        this.id = jsonOrder.getString("id");
        this.state = jsonOrder.getString("state");
        this.created_at = jsonOrder.getString("created_at");
        this.average_price = jsonOrder.getString("average_price");
        this.price = jsonOrder.getString("price");
        this.side = jsonOrder.getString("side");
        this.quantity = jsonOrder.getString("quantity");
        this.symbol = jsonOrder.getString("symbol");
        this.executions = jsonOrder.getJSONArray("executions").toString();
    }

    public RobinhoodOrder(final String symbol, final Order order) {
        this.id = DigestUtils.sha1Hex(order.getId());
        this.state = order.getState();
        this.created_at = order.getCreated_at();
        this.average_price = order.getAverage_price();
        this.price = order.getPrice();
        this.side = order.getSide();
        this.quantity = order.getQuantity();
        this.symbol = symbol;
        setExecutions(order.getExecutions());
    }

    public Double computeCost() {
        if (this.getAverage_price() == null) {
            return Double.parseDouble(getPrice());
        } else {
            return Double.parseDouble(getAverage_price());
        }
    }

    public String computeRankedTimestamp() {
        if ("filled".equalsIgnoreCase(state)) {
            return getExecutions().iterator().next().getTimestamp();
        }
        return getCreated_at();
    }


    @Override
    public String toString() {
        return id;
    }

}
