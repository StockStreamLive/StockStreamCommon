package stockstream.config;

public class Config {
    public static final float MAX_INFLUENCED_BUY = 3000;

    public static final String RH_UN = System.getenv("ROBINHOOD_USERNAME");
    public static final String RH_PW = System.getenv("ROBINHOOD_PASSWORD");
}
