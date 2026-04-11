package com.github.freeacs.core;

import com.github.freeacs.cache.HazelcastConfig;
import com.github.freeacs.common.scheduler.ExecutorWrapper;
import com.github.freeacs.common.scheduler.ExecutorWrapperFactory;
import com.github.freeacs.dbi.*;
import com.hazelcast.core.HazelcastInstance;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration(proxyBeanMethods = false)
public class AppConfiguration {

    @Bean
    @Primary
    public HazelcastInstance hazelcastInstance() {
        return HazelcastConfig.getHazelcastInstance();
    }

    @Bean
    public DBI dbi(DataSource dataSource) throws SQLException {
        Users users = new Users(dataSource);
        User adminUser = users.getUnprotected(Users.USER_ADMIN);
        Identity id = new Identity(SyslogConstants.FACILITY_CORE, "latest", adminUser);
        Syslog syslog = new Syslog(dataSource, id);

        return DBI.createAndInitialize(Integer.MAX_VALUE, dataSource, syslog);
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorWrapper executorWrapper(){
        return ExecutorWrapperFactory.create(3);
    }


    @Bean
    @Primary
    @ConfigurationProperties("main.datasource")
    public DataSource dataSource() {
        return new HikariDataSource();
    }
}
