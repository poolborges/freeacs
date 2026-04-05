package com.github.freeacs.syslogserver;

import com.github.freeacs.common.scheduler.ExecutorWrapper;
import com.github.freeacs.common.scheduler.ExecutorWrapperFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;

@SpringBootApplication
public class App {

    private final ExecutorWrapper executorWrapper;

    public App() {
        // Inicializa o executor como no código original
        this.executorWrapper = ExecutorWrapperFactory.create(3);
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public SyslogServlet syslogServlet(DataSource datasource, Properties properties) {
        SyslogServlet servlet = new SyslogServlet(datasource, properties, executorWrapper);
        servlet.init();
        return servlet;
    }

    @PreDestroy
    public void onShutdown() {
        System.out.println("Shutdown Hook is running (Spring PreDestroy)!");
        SyslogServlet.destroy();
        executorWrapper.shutdown();
    }
}
