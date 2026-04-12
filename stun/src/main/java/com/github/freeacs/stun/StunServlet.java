package com.github.freeacs.stun;

import com.github.freeacs.common.scheduler.ExecutorWrapper;
import com.github.freeacs.common.util.Sleep;
import com.github.freeacs.dbi.DBI;
import de.javawi.jstun.StunServer;
import java.net.InetAddress;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StunServlet {
  private StunServer server;

  private static final Logger logger = LoggerFactory.getLogger(StunServlet.class);

  private final DBI dbi;
  private final DataSource mainDs;
  private final Properties properties;
  private final ExecutorWrapper executorWrapper;

  public StunServlet(DBI dbi, DataSource mainDs, Properties properties, ExecutorWrapper executorWrapper) {
    this.dbi = dbi;
    this.mainDs = mainDs;
    this.properties = properties;
    this.executorWrapper = executorWrapper;
  }

  public void destroy() {
    Sleep.terminateApplication();
    server.shutdown();
  }

  public void init() {
    trigger();
  }


  private synchronized void trigger() {
    try {

      if (properties.isRunWithStun()) {
        if (server == null) {
          int pPort = properties.getPrimaryPort();
          String pIp = properties.getPrimaryIp();
          int sPort = properties.getSecondaryPort();
          String sIp = properties.getSecondaryIp();
          server =
              new StunServer(pPort, InetAddress.getByName(pIp), sPort, InetAddress.getByName(sIp));
        }
        if (!StunServer.isStarted()) {
          logger.info("Server startup...");
          server.start();
        }
      }

      ActiveDeviceDetection activeDeviceDetection =
          new ActiveDeviceDetection(mainDs, dbi, "ActiveDeviceDetection");
      executorWrapper.scheduleCron(
          "15 * * ? * * *",
          (tms) ->
              () -> {
                activeDeviceDetection.setThisLaunchTms(tms);
                activeDeviceDetection.run();
              });

      Thread singleKickThread = new Thread(new SingleKickThread(mainDs, dbi, properties));
      singleKickThread.setName("Scheduler STUN");
      singleKickThread.start();

      Thread jobKickThread = new Thread(new JobKickThread(mainDs, dbi, properties));
      jobKickThread.setName("STUN Job Kick Thread");
      jobKickThread.start();

    } catch (Exception t) {
      logger.error("An error occurred while starting Stun Server", t);
    }
  }
}
