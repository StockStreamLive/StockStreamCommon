package stockstream.database;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stockstream.logic.elections.Election;
import stockstream.util.JSONUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Elections")
public class ElectionStub {

    @Id
    @Column(name = "topic")
    private String topic;

    @Column(name = "expirationDate")
    private Long expirationDate;

    @Column(name = "rank")
    private Long rank;

    @Column(name = "polls")
    private String polls;

    @Column(name = "candidates")
    private String candidates;

    public Map<String, Integer> getPolls() {
        return JSONUtil.deserializeString(polls, new TypeReference<Map<String, Integer>>() {}).orElse(Collections.emptyMap());
    }

    public void setPolls(final Map<String, Integer> polls){
        this.polls = JSONUtil.serializeObject(polls).orElse("[]");
    }

    public Set<String> getCandidates() {
        return JSONUtil.deserializeString(candidates, new TypeReference<Set<String>>() {}).orElse(Collections.emptySet());
    }

    public void setCandidates(final Set<String> candidates){
        this.candidates = JSONUtil.serializeObject(candidates).orElse("[]");
    }

    public String getElectionId() {
        return String.format("%s:%s", topic, expirationDate);
    }

    public ElectionStub(final Election<?> election) {
        this.topic = election.getTopic();
        this.expirationDate = election.getExpirationDate();
        this.rank = election.getRank();

        final Map<String, Integer> polls = new HashMap<>();

        election.getCandidateToVoters().forEach((key, value) -> {
            final String objectStr = JSONUtil.serializeObject(key).get();
            polls.computeIfAbsent(objectStr, val -> value.size());
        });

        final Set<String> candidates = new HashSet<>();
        election.getCandidateToVoters().keySet().forEach(key -> candidates.add(JSONUtil.serializeObject(key).orElse("{}")));
        election.getCandidateToRunnable().keySet().forEach(key -> candidates.add(JSONUtil.serializeObject(key).orElse("{}")));

        setPolls(polls);
        setCandidates(candidates);
    }
}
