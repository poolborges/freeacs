package com.github.freeacs.stun.endpoint;

import de.javawi.jstun.StunServer;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Component
@Endpoint(id = "stun-clients")
public class StunClientsEndpoint {

    @ReadOperation
    public Map<String, Object> getStunInfo(@org.springframework.lang.Nullable Boolean onlyStats) {
        Map<String, Long> rawClients = StunServer.getActiveStunClients().getMap();
        long now = System.currentTimeMillis();

        Map<String, Object> response = new LinkedHashMap<>();

        // Stats
        response.put("totalActive", rawClients.size());
        response.put("serverTime", now);

        if (Boolean.TRUE.equals(onlyStats)) {
            return response;
        }

        // Detail
        Map<String, Object> clients = rawClients.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Map.of(
                                "lastSeenMs", e.getValue(),
                                "secondsAgo", (now - e.getValue()) / 1000
                        )
                ));

        response.put("clients", clients);

        return response;
    }
}
