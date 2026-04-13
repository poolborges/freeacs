package com.github.freeacs.stun;

import com.github.freeacs.common.scheduler.TaskDefaultImpl;
import com.github.freeacs.common.util.TimestampMap;
import com.github.freeacs.dbi.ACSUnit;
import com.github.freeacs.dbi.DBI;
import com.github.freeacs.dbi.Heartbeat;
import com.github.freeacs.dbi.Syslog;
import com.github.freeacs.dbi.SyslogConstants;
import com.github.freeacs.dbi.SyslogEntry;
import com.github.freeacs.dbi.SyslogFilter;
import com.github.freeacs.dbi.Unit;
import com.github.freeacs.dbi.util.SyslogClient;
import de.javawi.jstun.StunServer;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveDeviceDetection extends TaskDefaultImpl {
  private static final Logger logger = LoggerFactory.getLogger(ActiveDeviceDetection.class);
  private final DataSource xapsCp;
  private final DBI dbi;
  private final TimestampMap activeDevicesLogged = new TimestampMap();
  private static final int TIME_MS_ONE_HOUR = 60 * 60000;
  private static final int TIME_MS_FIVE_MINUTE = 5 * 60000;

  public ActiveDeviceDetection(DataSource xapsCp, DBI dbi, String taskName) {
    super(taskName);
    this.xapsCp = xapsCp;
    this.dbi = dbi;
  }

  private void logActiveDevices(TimestampMap activeDevices, TimestampMap sentSyslogMap)
      throws SQLException {
    // Process 1/60 of all active devices each time this method is called
    // Note that process 1/60 of the activeDevices map will be too much, since
    // some of the units will be more than 5 minutes old - and we cannot log
    // as "actually active" if the device timestamp is more than 5 minutes old.
    // Hence a portion of the activeDevices map (perhaps 1/10?) will be older
    // than 5 minutes, and then those 1/60 will actually process a larger share
    // than strictly required. This is not a problem - the most important
    // thing is to process all devices within an hour and to distribute the
    // load over time. Worst case scenario is that no units will be processed
    // at the end of a 60-minute cycle.
    int unitsToProcess = activeDevices.size() / 60;
    long fiveMinAgo = getThisLaunchTms() - TIME_MS_FIVE_MINUTE;
    long oneHourAgo = getThisLaunchTms() - TIME_MS_ONE_HOUR;
    ACSUnit acsUnit = new ACSUnit(xapsCp, dbi.getAcs(), dbi.getSyslog());

    // this will force units which haven't been processed the last hour to
    // be processed again.
    Map<String, Long> oldDevices = sentSyslogMap.removeOld(oneHourAgo);
    logger.info("Have removed {} devices from sentSyslog map(should be approx 1/60 of {} activeDevices)",  oldDevices.size(), activeDevices.size());

    int processCount = 0;
    int loggedCount = 0;
    for (Entry<String, Long> entry : activeDevices.getMap().entrySet()) {
      String address = entry.getKey();
      if (processCount > unitsToProcess) {
        break;
      }
      if (entry.getValue() > fiveMinAgo && sentSyslogMap.get(address) == null) {
        processCount++;
        sentSyslogMap.put(address, getThisLaunchTms());
        Unit unit = acsUnit.getUnitByValue(address, null, null);
        if (unit != null) {
          loggedCount++;
          SyslogClient.info(
              unit.getId(),
              "StunMsg/TR-111: Requests from " + address + " within last 5 minutes",
              16,
              null,
              null);
        } else {
          logger.info("Stun request from {}, but the address was not recorded (consider adding A-flag to UDPConnectionRequestAddress in all unittypes)",
                  address);
        }
      }
    }
    logger.info("Processed {} syslog messages. Sent {} syslog messages. SentSyslogMap: {} , ActiveDevices: {}",
            processCount, loggedCount, sentSyslogMap.size(), activeDevices.size());
    
    //meterRegistry.gauge("stun.active.devices", activeDevices.size());
    //meterRegistry.counter("stun.syslog.sent").increment(loggedCount);
  }

  private void logInactiveDevices(TimestampMap activeDevices) throws SQLException {
    long tooOldTms = getThisLaunchTms() - TIME_MS_ONE_HOUR;
    logger.info( "Will check for inactive STUN clients (map size before check: {})", activeDevices.size());
    Map<String, Long> tooOldMap = activeDevices.removeOldSync(tooOldTms);
    for (Entry<String, Long> entry : tooOldMap.entrySet()) {
      String address = entry.getKey();
      ACSUnit acsUnit = new ACSUnit(xapsCp, dbi.getAcs(), dbi.getSyslog());
      Unit unit = acsUnit.getUnitByValue(address, null, null);
      if (unit != null) {
        Syslog syslog = dbi.getSyslog();
        SyslogFilter sf = new SyslogFilter();
        sf.setCollectorTmsStart(new Date(tooOldTms)); // look for syslog newer than 1 hour
        sf.setUnitId(unit.getId());
        boolean active = false;
        List<SyslogEntry> entries = syslog.read(sf, dbi.getAcs());
        for (SyslogEntry sentry : entries) {
          String content = sentry.getContent();
          if (sentry.getFacility() < SyslogConstants.FACILITY_SHELL
              && !content.contains(Heartbeat.MISSING_HEARTBEAT_ID)
              && !content.startsWith("StunMsg/TR-111")) {
            logger.info("Found syslog activity for unit {} at {} : {}",
                    unit.getId(),  sentry.getCollectorTimestamp(), content);
            active = true;
            break;
          }
        }
        if (active) {
          logger.info("No STUN request from {} (unit: {}) since {} ",
                  address, unit.getId(), toDatetime(tooOldTms));
          SyslogClient.info(
              unit.getId(),
              "StunMsg/TR-111: No request from " + address + " since " + toDatetime(tooOldTms) + " - but device has been active since then",
              dbi.getSyslog());
        } else {
          logger.info("No STUN request from {} (unit: {}) since {}. Device may not be active",
                  address, unit.getId(), toDatetime(tooOldTms));
        }
      } else {
        logger.info("No STUN request from {} for more than 60 minutes.", address);
      }
    }
    logger.info("Have removed {} devices from active devices map", tooOldMap.size());
  }

  @Override
  public void runImpl() throws Throwable {
    TimestampMap activeDevices = StunServer.getActiveStunClients();
    // will clean out old and inactive devices from activeDevices map (and log new inactive devices)
    logInactiveDevices(activeDevices);
    // will update list of devices logged to syslog (and log new ones)
    logActiveDevices(activeDevices,activeDevicesLogged);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  private String toDatetime(long millis){
    return new Date(millis).toString();
  }
}
