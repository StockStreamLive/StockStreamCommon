package stockstream.logic;

import lombok.AllArgsConstructor;
import lombok.Data;
import stockstream.logic.elections.Candidate;

@Data
@AllArgsConstructor
public class TestCandidate implements Candidate {

    public String candidate;

    @Override
    public String getLabel() {
        return candidate;
    }
}
