package com.owera.xaps.monitor;

import com.owera.xaps.monitor.task.ModuleMonitorTask;
import com.owera.xaps.monitor.task.MonitorInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MonitorController {

    // Handles http://localhost:8090/monitor and http://localhost:8090/monitor/
    @GetMapping({"/", ""})
    public String monitorWeb(
            @RequestParam(value = "async", required = false) String async,
            @RequestParam(value = "html", required = false) String html,
            Model model) {

        model.addAttribute("java8TimeFormatter", java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        if (async != null) {
            model.addAttribute("async", async);
        }

        // Fetch events from the static set in ModuleMonitorTask
        List<MonitorInfo> events = ModuleMonitorTask.getMonitorInfoSet().stream()
                .filter(info -> !"monitor".equals(info.module()))
                .collect(Collectors.toList());

        model.addAttribute("events", events);

        if ("no".equalsIgnoreCase(html)) {
            return "main"; // returns main.ftl fragment
        } else {
            model.addAttribute("main", "main.ftl");
            return "index"; // returns index.ftl wrapper
        }
    }
}
