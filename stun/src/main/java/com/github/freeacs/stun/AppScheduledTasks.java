package com.github.freeacs.stun;

import com.github.freeacs.common.scheduler.Task;
import com.github.freeacs.dbi.DBI;
import com.github.freeacs.dbi.util.ACSVersionCheck;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Component
public class AppScheduledTasks {

    private final ActiveDeviceDetection activeDeviceDetection;
    private final DataSource dataSource;

    public AppScheduledTasks(DBI dbi, Properties properties, DataSource dataSource) {

        this.activeDeviceDetection = new ActiveDeviceDetection(dataSource, dbi, "ActiveDeviceDetection");
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() throws SQLException {
        ACSVersionCheck.versionCheck(dataSource);
    }

    // Run at 15(sec) every minute - light task
    @Scheduled(cron = "15 * * ? * * ")
    public void runTriggerReleaserTask(){
        runTask(activeDeviceDetection);
    }

    private void runTask(Task task) {
        if (!task.isRunning()) {
            task.setThisLaunchTms(System.currentTimeMillis());
            task.run();
        }
    }
}
