package com.github.freeacs.stun;

import com.github.freeacs.cache.HazelcastConfig;
import com.github.freeacs.common.scheduler.ExecutorWrapper;
import com.github.freeacs.common.scheduler.ExecutorWrapperFactory;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;

@SpringBootApplication
public class App {

    private final ExecutorWrapper executorWrapper;

    public App() {
        // Mantém a criação do pool de threads original
        this.executorWrapper = ExecutorWrapperFactory.create(2);
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public HazelcastInstance hazelcastInstance() {
        return HazelcastConfig.getHazelcastInstance();
    }

    @Bean
    public StunServlet stunServlet(DataSource datasource, Properties properties) {
        StunServlet servlet = new StunServlet(datasource, properties, executorWrapper);
        servlet.init();
        return servlet;
    }

    @PreDestroy
    public void onShutdown() {
        System.out.println("Shutdown Hook is running (Spring PreDestroy)!");
        StunServlet.destroy();
        executorWrapper.shutdown();
    }
}
