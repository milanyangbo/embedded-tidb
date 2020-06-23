package com.playtika.test.tidb;

import com.playtika.test.common.operations.DefaultNetworkTestOperations;
import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.common.properties.InstallPackageProperties;
import com.playtika.test.common.utils.AptGetPackageInstaller;
import com.playtika.test.common.utils.PackageInstaller;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;

import java.util.Collections;

import static com.playtika.test.tidb.TiDBProperties.BEAN_NAME_EMBEDDED_TIDB;


@Configuration
@ConditionalOnBean({TiDBProperties.class})
@ConditionalOnProperty(value = "embedded.tidb.enabled", matchIfMissing = true)
public class EmbeddedTiDBTestOperationsAutoConfiguration {

    @Bean
    @ConfigurationProperties("embedded.tidb.install")
    public InstallPackageProperties tidbPackageProperties() {
        InstallPackageProperties properties = new InstallPackageProperties();
        properties.setPackages(
                Collections.singleton("iproute2")); // we need iproute2 for tc command to work
        return properties;
    }

    @Bean
    public PackageInstaller tidbPackageInstaller(
            InstallPackageProperties tidbPackageProperties,
            @Qualifier(BEAN_NAME_EMBEDDED_TIDB) GenericContainer tidb) {
        return new AptGetPackageInstaller(tidbPackageProperties, tidb);
    }

    @Bean
    @ConditionalOnMissingBean(name = "tidbNetworkTestOperations")
    public NetworkTestOperations tidbNetworkTestOperations(
            @Qualifier(BEAN_NAME_EMBEDDED_TIDB) GenericContainer tidb) {
        return new DefaultNetworkTestOperations(tidb);
    }
}
