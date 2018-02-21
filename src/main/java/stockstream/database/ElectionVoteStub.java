package stockstream.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import stockstream.data.Voter;
import stockstream.util.JSONUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ElectionVotes")
public class ElectionVoteStub {

    @Id
    @Column(name = "voteId")
    private String voteId;

    @Column(name = "playerId")
    private String playerId;

    @Column(name = "electionId")
    private String electionId;

    @Column(name = "voteObject")
    private String voteObject;

    @Column(name = "voterObject")
    private String voterObject;

    public ElectionVoteStub(final Voter voter, final Object vote, final String electionId) {
        this(voter, JSONUtil.serializeObject(vote).orElse("{}"), electionId);
    }

    public ElectionVoteStub(final Voter voter, final String serializedVote, final String electionId) {
        this.voteId = String.format("%s:%s", electionId, voter.getPlayerId());
        this.playerId = voter.getPlayerId();
        this.electionId = electionId;
        this.voteObject = serializedVote;
        this.voterObject = JSONUtil.serializeObject(voter).orElse("{}");
    }

}
