package stockstream.data;

import com.cheddar.robinhood.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResult {
    private String action;
    private String symbol = "";
    private OrderStatus orderStatus;
    private Order order;

    @Override
    public String toString() {
        String orderInfo = "";
        if (order != null) {
            orderInfo = String.format("%s @ %.2f", order.getState(), Double.valueOf(order.getPrice()));
        }
        return String.format("%s %s %s", action, symbol, orderInfo);
    }
}
