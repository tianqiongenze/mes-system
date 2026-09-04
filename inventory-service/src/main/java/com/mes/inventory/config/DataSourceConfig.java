package com.mes.inventory.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Read-write separation DataSource configuration for CRDB.
 *
 * Two Hikari pools are created:
 *   - writeDataSource -> CRDB write node (port 26257)
 *   - readDataSource  -> CRDB read  node (port 26267)
 *
 * A RoutingDataSource picks the pool per-transaction based on readOnly flag,
 * wrapped in LazyConnectionDataSourceProxy so connections are only acquired
 * when actually needed (lets readOnly routing work even on write methods).
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.write.hikari")
    public HikariDataSource writeDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("CRDB-Write-Pool");
        return ds;
    }

    @Bean
    @ConfigurationProperties("spring.datasource.read.hikari")
    public HikariDataSource readDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("CRDB-Read-Pool");
        ds.setReadOnly(true);
        return ds;
    }

    @Bean
    @Primary
    public DataSource routingDataSource() {
        RoutingDataSource routing = new RoutingDataSource();
        Map<Object, Object> targets = new HashMap<>();
        targets.put(RoutingDataSource.WRITE, writeDataSource());
        targets.put(RoutingDataSource.READ, readDataSource());
        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(writeDataSource());
        routing.afterPropertiesSet();
        return new LazyConnectionDataSourceProxy(routing);
    }
}
