package stockstream.data;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class Constants {

    public static final Set<String> PENDING_ORDER_STATES = ImmutableSet.of("confirmed", "unconfirmed", "queued");

}
