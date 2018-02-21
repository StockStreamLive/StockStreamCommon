package stockstream.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "GameStates")
public class GameStateStub {

    private static final String ACCOUNT_KEY = "stockstream";

    @Id
    @Column(name = "accountName")
    private String accountName = ACCOUNT_KEY;

    @Column(name = "nextEvent")
    private Long nextEvent;

    @Column(name = "nextEventType")
    private String nextEventType;

    public GameStateStub(final Long nextEvent, final String nextEventType) {
        this.nextEvent = nextEvent;
        this.nextEventType = nextEventType;
    }

}
