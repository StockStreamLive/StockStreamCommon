package stockstream.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Account {
    private double cashBalance;
    private double spentMargin;
    private List<AssetNode> assets;
    private List<Object> orders;
}