package com.playtika.test.tidb;

import com.github.dockerjava.api.model.Capability;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.common.utils.ContainerUtils.startAndLogTime;
import static com.playtika.test.tidb.TiDBProperties.BEAN_NAME_EMBEDDED_TIDB;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;


@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.tidb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(TiDBProperties.class)
public class EmbeddedTiDBBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_TIDB, destroyMethod = "stop")
    public GenericContainer tidb(ConfigurableEnvironment environment, TiDBProperties properties)
            throws Exception {
        log.info("Starting tidb server. Docker image: {}", properties.dockerImage);

        TiDBContainer tidb =
                new TiDBContainer<>(properties.dockerImage)
                        .withUsername(properties.getUser())
                        .withPassword(properties.getPassword())
                        .withDatabaseName(properties.getDatabase())
//                        .withCommand(
//                                "--config=/config/config.toml")
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.port)
                        .withCreateContainerCmdModifier(cmd -> cmd.withCapAdd(Capability.NET_ADMIN))
                        .withStartupTimeout(properties.getTimeoutDuration())
                        .withInitScript(properties.initScriptPath);
        startAndLogTime(tidb);
        registerTidbEnvironment(tidb, environment, properties);
        return tidb;
    }

    private void registerTidbEnvironment(
            GenericContainer tidb, ConfigurableEnvironment environment, TiDBProperties properties) {
        Integer mappedPort = tidb.getMappedPort(properties.port);
        String host = tidb.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.tidb.port", mappedPort);
        map.put("embedded.tidb.host", host);
        map.put("embedded.tidb.schema", properties.getDatabase());
        map.put("embedded.tidb.user", properties.getUser());
        map.put("embedded.tidb.password", properties.getPassword());

        String jdbcURL = "jdbc:mysql://{}:{}/{}";
        log.info(
                "Started tidb server. Connection details: {}, " + "JDBC connection url: " + jdbcURL,
                map,
                host,
                mappedPort,
                properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedTiDBInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
