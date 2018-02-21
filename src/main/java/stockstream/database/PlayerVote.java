package stockstream.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "PlayerVotes")
public class PlayerVote {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name="platform_username")
    private String platform_username;

    @Column(name = "date")
    private String date;

    @Column(name = "action")
    private String action;

    @Column(name = "parameter")
    private String parameter;

    @Column(name = "timestamp")
    private Long timestamp;

    @Column(name = "order_id")
    private String order_id;

}
