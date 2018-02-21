package stockstream.twitch;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import stockstream.http.HTTPClient;
import stockstream.http.HTTPQuery;
import stockstream.http.HTTPResult;

import java.util.*;

@Slf4j
public class TwitchAPI {
    private static final String TWITCH_CLIENT_ID = System.getenv("TWITCH_CLIENT_ID");

    private static final String CHANNEL_ROOM_URL = "http://tmi.twitch.tv/group/user/stockstream/chatters";

    private final HTTPClient httpClient = new HTTPClient();

    public List<String> getViewers() {
        try {
            final Optional<HTTPResult> httpResultOptional = this.httpClient.executeHTTPGetRequest(new HTTPQuery(CHANNEL_ROOM_URL, ImmutableMap.of(), ImmutableMap.of()));

            if (!httpResultOptional.isPresent()) {
                log.error("Exception getting list of viewers from {}", CHANNEL_ROOM_URL);
                return ImmutableList.of();
            }

            final JSONObject jsonObject = new JSONObject(httpResultOptional.get().getBody());
            final JSONArray viewersArray = jsonObject.getJSONObject("chatters").getJSONArray("viewers");

            final List<String> viewers = new ArrayList<>();

            for (final Object viewerObj : viewersArray) {
                if (!(viewerObj instanceof String)) {
                    continue;
                }

                viewers.add((String)viewerObj);
            }

            return viewers;
        } catch (final Exception ex) {
            log.warn(ex.getMessage(), ex);
        }

        return Collections.emptyList();
    }

    public Optional<String> isChannelStreaming(final String username) {
        try {
            final String endpoint = String.format("https://api.twitch.tv/kraken/streams/%s?client_id=%s", username, TWITCH_CLIENT_ID);

            final Optional<HTTPResult> httpResult = this.httpClient.executeHTTPGetRequest(new HTTPQuery(endpoint, ImmutableMap.of(), ImmutableMap.of()));

            if (!httpResult.isPresent()) {
                log.warn("Bad response from TwitchAPI");
                return Optional.empty();
            }

            final JSONObject twitchChannelObject = new JSONObject(httpResult.get().getBody());

            String streamURL = null;

            final Object streamRawObject = twitchChannelObject.get("stream");

            if (streamRawObject instanceof JSONObject) {
                final JSONObject streamObject = (JSONObject) streamRawObject;

                final Long streamID = streamObject.getLong("_id");
                final Long channelID = streamObject.getJSONObject("channel").getLong("_id");

                streamURL = String.format("https://player.twitch.tv/?stream=%s&channelId=%s&autoplay=true", streamID, channelID);
            }

            return Optional.ofNullable(streamURL);
        } catch (final Exception exception) {
            log.warn(exception.getMessage(), exception);
        }

        return Optional.empty();
    }

    public Map<String, String> getStreamURLsByUsername(final List<String> usernames) {
        final Map<String, String> usernameToStreamURL = new HashMap<>();

        final List<List<String>> batches = Lists.partition(usernames, 50);
        for (final List<String> batch : batches) {

            final String userList = StringUtils.join(batch, ",");

            try {
                populateStreamURLSFromBatch(userList, usernameToStreamURL);
            } catch (final Exception exception) {
                log.warn(exception.getMessage(), exception);
            }
        }

        return usernameToStreamURL;
    }

    private void populateStreamURLSFromBatch(final String userList, final Map<String, String> usernameToStreamURL) {
        final String endpoint = String.format("https://api.twitch.tv/kraken/streams?channel=%s&client_id=%s", userList, TWITCH_CLIENT_ID);

        final Optional<HTTPResult> httpResult = this.httpClient.executeHTTPGetRequest(new HTTPQuery(endpoint, ImmutableMap.of(), ImmutableMap.of()));

        if (!httpResult.isPresent()) {
            log.warn("Bad response from TwitchAPI");
            return;
        }

        final JSONObject twitchChannelObject = new JSONObject(httpResult.get().getBody());

        final JSONArray streams = twitchChannelObject.getJSONArray("streams");

        for (final Object obj : streams) {
            if (!(obj instanceof JSONObject)) {
                continue;
            }

            final JSONObject streamObject = (JSONObject) obj;

            final Pair<String, String> usernameStream = extractUsernameAndStream(streamObject);
            usernameToStreamURL.put(usernameStream.getLeft(), usernameStream.getRight());
        }
    }


    private Pair<String, String> extractUsernameAndStream(final JSONObject jsonObject) {
        final Long streamID = jsonObject.getLong("_id");
        final Long channelID = jsonObject.getJSONObject("channel").getLong("_id");
        final String channelName = jsonObject.getJSONObject("channel").getString("name");

        final String streamURL = String.format("https://player.twitch.tv/?stream=%s&channelId=%s&autoplay=true", streamID, channelID);

        return ImmutablePair.of(channelName, streamURL);
    }
}
