package stockstream.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Score {

    private String playerId;

    private double decimalReturn;
    private double dollarReturn;

    private int qualifiedTrades;

    private double dollarsSpent;
    private double dollarsSold;

}
