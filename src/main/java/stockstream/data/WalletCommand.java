package stockstream.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import stockstream.logic.elections.Candidate;

@Data
@AllArgsConstructor
public class WalletCommand implements Candidate {
    private WalletAction action;
    private int quantity;
    private String parameter;
    private double limit;

    @Override
    public int hashCode() {
        int hashcode = new HashCodeBuilder().append(action).append(parameter).append(limit).toHashCode();
        return hashcode;
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof WalletCommand)) {
            return false;
        }
        final WalletCommand otherTradeCommand = (WalletCommand) object;

        return this.hashCode() == otherTradeCommand.hashCode();
    }

    @Override
    public String toString() {
        return (action + " " + parameter + " " + String.valueOf(limit)).trim();
    }

    @Override
    public String getLabel() {
        return this.toString();
    }
}
