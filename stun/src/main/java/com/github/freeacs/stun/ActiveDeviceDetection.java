package com.github.freeacs.stun;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.freeacs.common.scheduler.TaskDefaultImpl;
import com.github.freeacs.common.util.TimestampMap;
import com.github.freeacs.dbi.*;
import com.github.freeacs.dbi.util.SyslogClient;
import de.javawi.jstun.StunServer;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveDeviceDetection extends TaskDefaultImpl {
  private static final Logger logger = LoggerFactory.getLogger(ActiveDeviceDetection.class);

  private final DataSource dataSource;
  private final DBI dbi;
  private final TimestampMap activeDevicesLogged = new TimestampMap();

  private static final int TIME_MS_ONE_HOUR = 60 * 60000;
  private static final int TIME_MS_FIVE_MINUTE = 5 * 60000;

  // Caffeine cache to store Unit lookups for 5 minutes.
  // This prevents hitting the database repeatedly for the same address.
  private final Cache<String, Unit> unitCache = Caffeine.newBuilder()
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .maximumSize(10000)
          .build();

  public ActiveDeviceDetection(DataSource dataSource, DBI dbi, String taskName) {
    super(taskName);
    this.dataSource = dataSource;
    this.dbi = dbi;
  }

  private void logActiveDevices(TimestampMap activeDevices, TimestampMap sentSyslogMap, ACSUnit acsUnit)
          throws SQLException {
    int unitsToProcess = activeDevices.size() / 60;
    long fiveMinAgo = getThisLaunchTms() - TIME_MS_FIVE_MINUTE;
    long oneHourAgo = getThisLaunchTms() - TIME_MS_ONE_HOUR;

    sentSyslogMap.removeOld(oneHourAgo);
    logger.info("Removed old devices from sentSyslogMap. Active map size: {}", activeDevices.size());

    int processCount = 0;
    int loggedCount = 0;

    for (Entry<String, Long> entry : activeDevices.getMap().entrySet()) {
      if (processCount > unitsToProcess) {
        break;
      }
      String address = entry.getKey();
      if (entry.getValue() > fiveMinAgo && sentSyslogMap.get(address) == null) {
        processCount++;
        sentSyslogMap.put(address, getThisLaunchTms());

        Unit unit = getUnitFromCache(acsUnit, address);
        if (unit != null) {
          loggedCount++;
          SyslogClient.info(
                  unit.getId(),
                  "StunMsg/TR-111: Requests from " + address + " within last 5 minutes",
                  16, null, null);
        }
      }
    }
    logger.info("Processed {} syslog messages. Sent {}. ActiveDevices: {}",
            processCount, loggedCount, activeDevices.size());
  }

  private void logInactiveDevices(TimestampMap activeDevices, ACSUnit acsUnit) throws SQLException {
    long tooOldTms = getThisLaunchTms() - TIME_MS_ONE_HOUR;
    logger.info("Checking for inactive STUN clients. Current size: {}", activeDevices.size());

    Map<String, Long> tooOldMap = activeDevices.removeOldSync(tooOldTms);
    for (Entry<String, Long> entry : tooOldMap.entrySet()) {
      String address = entry.getKey();
      Unit unit = getUnitFromCache(acsUnit, address);

      if (unit != null) {
        Syslog syslog = dbi.getSyslog();
        SyslogFilter sf = new SyslogFilter();
        sf.setCollectorTmsStart(new Date(tooOldTms));
        sf.setUnitId(unit.getId());

        boolean active = syslog.read(sf, dbi.getAcs()).stream()
                .anyMatch(sentry -> {
                  String content = sentry.getContent();
                  return sentry.getFacility() < SyslogConstants.FACILITY_SHELL
                          && !content.contains(Heartbeat.MISSING_HEARTBEAT_ID)
                          && !content.startsWith("StunMsg/TR-111");
                });

        if (active) {
          SyslogClient.info(unit.getId(),
                  "StunMsg/TR-111: No request from " + address + " since " + toDatetime(tooOldTms) + " - device active elsewhere",
                  dbi.getSyslog());
        }
      }
    }
    logger.info("Removed {} inactive devices from map", tooOldMap.size());
  }

  /**
   * Helper to retrieve Unit from Caffeine cache or database.
   */
  private Unit getUnitFromCache(ACSUnit acsUnit, String address) {
    return unitCache.get(address, addr -> {
      try {
        return acsUnit.getUnitByValue(addr, null, null);
      } catch (SQLException e) {
        logger.error("Database error fetching unit for address: {}", addr, e);
        return null;
      }
    });
  }

  @Override
  public void runImpl() throws Throwable {
    TimestampMap activeDevices = StunServer.getActiveStunClients();
    // Instantiate ACSUnit once per run to avoid overhead in loops
    ACSUnit acsUnit = new ACSUnit(dataSource, dbi.getAcs(), dbi.getSyslog());

    logInactiveDevices(activeDevices, acsUnit);
    logActiveDevices(activeDevices, activeDevicesLogged, acsUnit);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  private String toDatetime(long millis) {
    return new Date(millis).toString();
  }
}
