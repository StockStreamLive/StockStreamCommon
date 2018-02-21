package stockstream.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import stockstream.data.Account;
import stockstream.database.RobinhoodOrder;
import stockstream.http.HTTPClient;
import stockstream.http.HTTPQuery;
import stockstream.http.HTTPResult;
import stockstream.util.JSONUtil;

import java.util.*;

public class StockStreamAPI {

    @Autowired
    private HTTPClient httpClient;

    private static final String STOCKSTREAM_API_ENDPOINT = System.getenv("STOCKSTREAM_API_ENDPOINT");

    public static final String PORTFOLIO_API = "/v1/portfolio/current";
    public static final String POSITIONS_API = "/v1/positions/open";
    public static final String GAMESTATE_API = "/v1/game/state";
    public static final String ELECTIONS_API = "/v1/elections";
    public static final String ACCOUNT_API = "/v1/account";
    public static final String SCORES_API = "/v1/scores";

    public JSONObject queryEndpoint(final String apiMethod) {
        final HTTPQuery httpQuery = new HTTPQuery(STOCKSTREAM_API_ENDPOINT + apiMethod, ImmutableMap.of(), ImmutableMap.of());

        final Optional<HTTPResult> httpResult = httpClient.executeHTTPGetRequest(httpQuery);
        if (!httpResult.isPresent()) {
            return new JSONObject();
        }

        return new JSONObject(httpResult.get().getBody());
    }

    public Account getStockStreamAccount() {
        final HTTPQuery httpQuery = new HTTPQuery(STOCKSTREAM_API_ENDPOINT + PORTFOLIO_API, ImmutableMap.of(), ImmutableMap.of());

        final Optional<HTTPResult> httpResult = httpClient.executeHTTPGetRequest(httpQuery);
        if (!httpResult.isPresent()) {
            return new Account();
        }

        final Optional<Account> stockStreamAccount = JSONUtil.deserializeObject(httpResult.get().getBody(), Account.class);
        if (!stockStreamAccount.isPresent()) {
            return new Account();
        }

        return stockStreamAccount.get();
    }

    public JSONArray getElections() {
        final HTTPQuery httpQuery = new HTTPQuery(STOCKSTREAM_API_ENDPOINT + ELECTIONS_API, ImmutableMap.of(), ImmutableMap.of());

        final Optional<HTTPResult> httpResult = httpClient.executeHTTPGetRequest(httpQuery);
        if (!httpResult.isPresent()) {
            return new JSONArray();
        }

        final JSONArray elections = new JSONArray(httpResult.get().getBody());

        return elections;
    }

    public List<Position> getOpenPositions() {
        final HTTPQuery httpQuery = new HTTPQuery(STOCKSTREAM_API_ENDPOINT + POSITIONS_API, ImmutableMap.of(), ImmutableMap.of());

        final Optional<HTTPResult> httpResult = httpClient.executeHTTPGetRequest(httpQuery);
        if (!httpResult.isPresent()) {
            return ImmutableList.of();
        }

        final JSONArray jsonPositions = new JSONArray(httpResult.get().getBody());

        final List<Position> positions = new ArrayList<>();
        for (final Object object : jsonPositions) {
            if (!(object instanceof JSONObject)) {
                continue;
            }

            final JSONObject jsonObject = (JSONObject) object;

            final boolean isWalletOrder = jsonObject.getBoolean("walletOrder");
            final Double influence = jsonObject.getDouble("influence");
            final Optional<Set<String>> liablePlayers =  JSONUtil.deserializeString(jsonObject.getJSONArray("liablePlayers").toString(),
                                                                                    new TypeReference<Set<String>>() {});
            final JSONObject buyOrder = jsonObject.getJSONObject("buyOrder");
            final RobinhoodOrder robinhoodOrder = new RobinhoodOrder(buyOrder);

            final Position position = new Position(robinhoodOrder, null, influence, liablePlayers.orElse(Collections.emptySet()), isWalletOrder, false);
            positions.add(position);
        }

        return positions;
    }


    public List<Score> getScores() {
        final HTTPQuery httpQuery = new HTTPQuery(STOCKSTREAM_API_ENDPOINT + SCORES_API, ImmutableMap.of(), ImmutableMap.of());

        final Optional<HTTPResult> httpResult = httpClient.executeHTTPGetRequest(httpQuery);
        if (!httpResult.isPresent()) {
            return ImmutableList.of();
        }

        final Optional<List<Score>> playerScores = JSONUtil.deserializeString(httpResult.get().getBody(), new TypeReference<List<Score>>() {});
        if (!playerScores.isPresent()) {
            return ImmutableList.of();
        }

        return playerScores.get();
    }

}
