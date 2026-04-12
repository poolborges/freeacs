package com.github.freeacs.stun;

import com.github.freeacs.common.util.Sleep;
import com.github.freeacs.dbi.DBI;
import de.javawi.jstun.StunServer;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StunServlet {
  private static final Logger logger = LoggerFactory.getLogger(StunServlet.class);

  private StunServer server;
  private ExecutorService executor;

  private final DBI dbi;
  private final DataSource mainDs;
  private final Properties properties;

  public StunServlet(DBI dbi, DataSource mainDs, Properties properties) {
    this.dbi = dbi;
    this.mainDs = mainDs;
    this.properties = properties;
  }

  public synchronized void init() {
    logger.info("Init StunServlet and background threads...");

    try {
      if (properties.isRunWithStun()) {
        startStunServer();
      }

      startTriggers();
    } catch (Exception t) {
      logger.error("Critical error while starting Stun Server or Schedulers", t);
    }

  }

  public synchronized void destroy() {
    logger.info("Shutting down StunServlet and stopping background threads...");
    Sleep.terminateApplication();

    if (server != null) {
      server.shutdown();
    }

    if (executor != null) {
      executor.shutdownNow();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          logger.warn("Background threads did not terminate within the expected time.");
        }
      } catch (InterruptedException e) {
        logger.error("Interrupted during executor shutdown", e);
        Thread.currentThread().interrupt();
      }
    }
  }

  private void startTriggers() {

      if (executor == null || executor.isShutdown()) {
        executor = Executors.newFixedThreadPool(2, new ThreadFactory() {
          private final AtomicInteger count = new AtomicInteger(1);
          @Override
          public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("StunBackgroundPool-" + count.getAndIncrement());
            return t;
          }
        });
      }

      executor.submit(() -> {
        Thread.currentThread().setName("STUN-SingleKickThread");
        new SingleKickThread(mainDs, dbi, properties).run();
      });

      executor.submit(() -> {
        Thread.currentThread().setName("STUN-JobKickThread");
        new JobKickThread(mainDs, dbi, properties).run();
      });

      logger.info("Background kick threads submitted to the pool.");
  }

  private void startStunServer() throws UnknownHostException, SocketException {
    if (server == null) {
      int pPort = properties.getPrimaryPort();
      String pIp = properties.getPrimaryIp();
      int sPort = properties.getSecondaryPort();
      String sIp = properties.getSecondaryIp();

      server = new StunServer(pPort, InetAddress.getByName(pIp), sPort, InetAddress.getByName(sIp));
    }

    if (!StunServer.isStarted()) {
      logger.info("Starting STUN Server on {}:{}", properties.getPrimaryIp(), properties.getPrimaryPort());
      server.start();
    }
  }
}
