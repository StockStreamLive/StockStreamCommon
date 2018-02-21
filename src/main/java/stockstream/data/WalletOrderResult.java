package stockstream.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletOrderResult {
    private WalletCommand walletCommand;
    private int executedShares;
    private OrderStatus orderStatus;

    @Override
    public String toString() {
        return String.format("%s %s %s %.2f -> %s",
                             walletCommand.getAction(),
                             walletCommand.getQuantity(),
                             walletCommand.getParameter(),
                             walletCommand.getLimit(),
                             orderStatus);
    }
}
