package com.github.freeacs.core;

import com.github.freeacs.common.scheduler.ScheduleType;
import com.github.freeacs.common.scheduler.Task;
import com.github.freeacs.core.task.*;
import com.github.freeacs.dbi.DBI;
import com.github.freeacs.dbi.util.ACSVersionCheck;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Component
public class AppScheduledTasks {

    private final DeleteOldJobs deleteOldJobs;
    private final JobRuleEnforcer jobRuleEnforcer;
    private final ScriptExecutor scriptExecutor;
    private final TriggerReleaser triggerReleaserTask;
    private final DeleteOldScripts deleteOldScriptsTask;
    private final HeartbeatDetection heartbeatDetectionTask;
    private final ReportGenerator reportGeneratorHourlyTask;
    private final ReportGenerator reportGeneratorDailyTask;
    private final DeleteOldSyslog deleteOldSyslogTask;
    private final DataSource dataSource;

    public AppScheduledTasks(DBI dbi, Properties properties, DataSource dataSource) {

        this.deleteOldJobs = new DeleteOldJobs("DeleteOldJobs", dbi, properties);
        this.jobRuleEnforcer = new JobRuleEnforcer("JobRuleEnforcer", dbi, properties);
        this.scriptExecutor = new ScriptExecutor("ScriptExecutor", dbi, properties);
        this.triggerReleaserTask = new TriggerReleaser("TriggerReleaser", dbi);
        this.deleteOldScriptsTask = new DeleteOldScripts("DeleteOldScripts", dbi, properties);
        this.heartbeatDetectionTask = new HeartbeatDetection("HeartbeatDetection", dbi);
        this.reportGeneratorHourlyTask = new ReportGenerator("ReportGeneratorHourly", ScheduleType.HOURLY, dbi, properties);
        this.reportGeneratorDailyTask = new ReportGenerator("ReportGeneratorDaily", ScheduleType.DAILY, dbi, properties);
        this.deleteOldSyslogTask = new DeleteOldSyslog("DeleteOldSyslogEntries", dbi, properties);
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() throws SQLException {
        ACSVersionCheck.versionCheck(dataSource);
    }

    // Run at 30(sec) every minute - light task
    @Scheduled(cron = "30 * * ? * *")
    public void runTriggerReleaserTask(){
        if (ACSVersionCheck.triggerSupported) {
            runTask(triggerReleaserTask);
        }
    }

    // Run every second - light task
    @Scheduled(cron = "* * * * * ?")
    public void runJobRuleEnforcer() {
        runTask(jobRuleEnforcer);
    }

    // Run at 05:30 every day - light task
    @Scheduled(cron = "0 30 5 * * ?")
    public void runDeleteOldJobs() {
        runTask(deleteOldJobs);
    }

    // Run every second - light task
    @Scheduled(cron = "* * * * * ?")
    public void runScriptExecutor() {
        if (ACSVersionCheck.scriptExecutionSupported) {
            runTask(scriptExecutor);
        }
    }

    // Run at 45 every hour - light task
    @Scheduled(cron = "0 45 * ? * *")
    public void runDeleteOldScripts() {
        if (ACSVersionCheck.scriptExecutionSupported) {
            runTask(deleteOldScriptsTask);
        }
    }

    // Run every 5 minute - moderate task
    @Scheduled(cron = "0 0/5 * ? * *")
    public void runHeartbeatDetectionTask() {
        if (ACSVersionCheck.heartbeatSupported) {
            runTask(heartbeatDetectionTask);
        }
    }


    @Scheduled(cron = "0 0 * ? * *")
    public void runReportGeneratorHourly() {
        runTask(reportGeneratorHourlyTask);
    }

    @Scheduled(cron = "0 15 0 ? * *")
    public void runReportGeneratorDaily() {
        runTask(reportGeneratorDailyTask);
    }

    @Scheduled(cron = "0 0 5 ? * *")
    public void runDeleteOldSyslogTask() {
        runTask(deleteOldSyslogTask);
    }

    private void runTask(Task task) {
        if (!task.isRunning()) {
            task.setThisLaunchTms(System.currentTimeMillis());
            task.run();
        }
    }
}
