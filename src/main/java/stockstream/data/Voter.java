package stockstream.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang.builder.HashCodeBuilder;

@Data
@AllArgsConstructor
public class Voter {

    private String username;
    private String platform;
    private String channel;
    private boolean subscriber;

    public String getPlayerId() {
        return platform + ":" + username;
    }

    /**
     * Note that for these methods must only check player name and platform for equality.
     * Otherwise players voting in multiple channels will be double counted.
     */
    @Override
    public int hashCode() {
        int hashcode = new HashCodeBuilder().append(username).append(platform).toHashCode();
        return hashcode;
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof Voter)) {
            return false;
        }
        final Voter otherVoter = (Voter) object;

        return this.hashCode() == otherVoter.hashCode();
    }
}
