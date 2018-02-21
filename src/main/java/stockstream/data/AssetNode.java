package stockstream.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AssetNode {
    private String symbol;
    private int shares;
    private double avgCost;
}
