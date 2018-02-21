package stockstream.data;

public enum OrderStatus {
    OK,
    CANT_AFFORD,
    BALANCE_TOO_LOW,
    BAD_LIMIT,
    BAD_TICK_SIZE,
    BAD_TICKER,
    BAD_AUTH,
    NET_WORTH_TOO_LOW,
    NO_SHARES,
    NOT_ENOUGH_VOTES,
    NOT_ENOUGH_BUYING_POWER,
    EXCESS_CASH_AVAILABLE,
    MARKET_CLOSED,
    BROKER_EXCEPTION,
    SERVER_EXCEPTION,
    INVALID_COMMAND,
    UNKNOWN
}