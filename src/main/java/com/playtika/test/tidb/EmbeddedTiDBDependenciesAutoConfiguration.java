package com.playtika.test.tidb;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static com.playtika.test.tidb.TiDBProperties.BEAN_NAME_EMBEDDED_TIDB;


@Configuration
@AutoConfigureOrder
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(name = "embedded.tidb.enabled", matchIfMissing = true)
@AutoConfigureAfter(
        name = "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
public class EmbeddedTiDBDependenciesAutoConfiguration {

    @Configuration
    public static class EmbeddedTiDbDataSourceDependencyContext {
        @Bean
        public BeanFactoryPostProcessor datasourceDependencyPostProcessor() {
            return new DependsOnPostProcessor(DataSource.class, new String[]{BEAN_NAME_EMBEDDED_TIDB});
        }
    }
}
