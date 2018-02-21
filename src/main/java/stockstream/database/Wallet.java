package stockstream.database;


import com.google.common.math.DoubleMath;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@NoArgsConstructor
@Table(name = "Wallets")
public class Wallet {

    @Id
    @Column(name = "platform_username")
    private String platform_username;

    @Column(name = "realized_return")
    private double realizedReturn;

    @Column(name = "realized_decimal_return")
    private double realizedDecimalReturn;

    @Column(name = "unrealized_dollars_spent")
    private double unrealizedDollarsSpent;

    @Column(name = "sent_dollars")
    private double sentDollars;

    @Column(name = "received_dollars")
    private double receivedDollars;

    @Column(name = "referral_code")
    private String referralCode;

    @Column(name = "referral_clicks")
    private int referralClicks;

    public Wallet(final String playerId, final double realizedReturn, final double realizedDecimalReturn, final double unrealizedDollarsSpent) {
        this.platform_username = playerId;
        this.realizedReturn = realizedReturn;
        this.realizedDecimalReturn = realizedDecimalReturn;
        this.unrealizedDollarsSpent = unrealizedDollarsSpent;
    }

    public boolean indirectValuesChanged(final Wallet otherWallet) {
        final boolean objectsEqual =
                DoubleMath.fuzzyEquals(realizedReturn, otherWallet.getRealizedReturn(), .001) &&
                DoubleMath.fuzzyEquals(realizedDecimalReturn, otherWallet.getRealizedDecimalReturn(), .001) &&
                DoubleMath.fuzzyEquals(unrealizedDollarsSpent, otherWallet.getUnrealizedDollarsSpent(), .001);
        return !objectsEqual;
    }

    public boolean walletValuesChanged(final Wallet otherWallet) {
        final boolean objectsEqual =
                DoubleMath.fuzzyEquals(realizedReturn, otherWallet.getRealizedReturn(), .001) &&
                DoubleMath.fuzzyEquals(sentDollars, otherWallet.getSentDollars(), .001) &&
                DoubleMath.fuzzyEquals(receivedDollars, otherWallet.getReceivedDollars(), .001) &&
                DoubleMath.fuzzyEquals(realizedDecimalReturn, otherWallet.getRealizedDecimalReturn(), .001) &&
                DoubleMath.fuzzyEquals(unrealizedDollarsSpent, otherWallet.getUnrealizedDollarsSpent(), .001);
        return !objectsEqual;
    }

}
