package stockstream.logic.elections;

import lombok.Data;
import stockstream.data.Voter;
import stockstream.util.RandomUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class VoteComparator<T> implements Comparator<T> {

    private Map<T, Set<Voter>> candidateToVoters = new ConcurrentHashMap<>();

    public VoteComparator(final Map<T, Set<Voter>> candidateToVoters) {
        this.candidateToVoters = candidateToVoters;
    }

    private VoteComparator() { }

    @Override
    public int compare(final T o1, final T o2) {
        final Integer o1Votes = candidateToVoters.getOrDefault(o1, Collections.emptySet()).size();
        final Integer o2Votes = candidateToVoters.getOrDefault(o2, Collections.emptySet()).size();

        if (o2Votes.equals(o1Votes)) {
            return RandomUtil.choice(-1, 1);
        }

        return o2Votes.compareTo(o1Votes);
    }

}
