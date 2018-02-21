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
@Table(name = "Contests")
public class ContestEntry {

    @Id
    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "username")
    private String username;

    @Column(name = "platform")
    private String platform;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "contest_name")
    private String contestName;

    public String getPlayerId() {
        return String.format("%s:%s", platform, username);
    }
}
