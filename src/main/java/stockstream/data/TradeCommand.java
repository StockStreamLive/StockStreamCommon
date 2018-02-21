package stockstream.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import stockstream.logic.elections.Candidate;

@Data
@AllArgsConstructor
public class TradeCommand implements Candidate {
    private TradeAction action;
    private String parameter;

    public TradeCommand(final TradeCommand otherTradeCommand) {
        this.action = otherTradeCommand.action;
        this.parameter = otherTradeCommand.parameter;
    }

    public boolean isValid() {
        return action != null && parameter != null;
    }

    @Override
    public int hashCode() {
        int hashcode = new HashCodeBuilder().append(action).append(parameter).toHashCode();
        return hashcode;
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof TradeCommand)) {
            return false;
        }
        final TradeCommand otherTradeCommand = (TradeCommand) object;

        return this.hashCode() == otherTradeCommand.hashCode();
    }

    @Override
    public String toString() {
        return (action + " " + parameter).trim();
    }

    @Override
    public String getLabel() {
        return this.toString();
    }
}
