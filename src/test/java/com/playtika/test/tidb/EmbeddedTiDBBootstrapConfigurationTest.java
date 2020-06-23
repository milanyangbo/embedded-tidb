/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.tidb;

import com.playtika.test.common.operations.NetworkTestOperations;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.Callable;

import static com.playtika.test.tidb.TiDBProperties.BEAN_NAME_EMBEDDED_TIDB;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = EmbeddedTiDBBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
                "spring.profiles.active=enabled",
                "embedded.tidb.init-script-path=initScript.sql"
        })
public class EmbeddedTiDBBootstrapConfigurationTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    NetworkTestOperations tidbNetworkTestOperations;

    @Test
    public void shouldConnectToTiDB() throws Exception {
        assertThat(jdbcTemplate.queryForObject("select version()", String.class)).contains("TiDB");
    }

    @Test
    public void shouldSaveAndGetUnicode() throws Exception {
        jdbcTemplate.execute("CREATE TABLE employee(id INT, name VARCHAR(64));");
        jdbcTemplate.execute("insert into employee (id, name) values (1, 'some data');");

        assertThat(jdbcTemplate.queryForObject("select name from employee where id = 1", String.class)).isEqualTo("some data");
    }

    @Test
    public void shouldEmulateLatency() throws Exception {
        jdbcTemplate.execute("CREATE TABLE operator(id INT, name VARCHAR(64));");
        jdbcTemplate.execute("insert into operator (id, name) values (1, 'test');");

/*        tidbNetworkTestOperations.withNetworkLatency(ofMillis(1000),
                () -> assertThat(durationOf(() -> jdbcTemplate.queryForList("select name from operator", String.class)))
                        .isCloseTo(1000L, Offset.offset(100L))
        );*/

        assertThat(durationOf(() -> jdbcTemplate.queryForList("select name from operator", String.class)))
                .isLessThan(100L);
    }

    @Test
    public void shouldInitDBForTiDB() throws Exception {
        assertThat(jdbcTemplate.queryForObject("select count(first_name) from users where first_name = 'Sam' ", Integer.class)).isEqualTo(1);
    }


    @Test
    public void shouldSetupDependsOnForAllDataSources() throws Exception {
        String[] beanNamesForType = beanFactory.getBeanNamesForType(DataSource.class);
        assertThat(beanNamesForType)
                .as("Custom datasource should be present")
                .hasSize(1)
                .contains("customDatasource");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_TIDB);
    }

    private static long durationOf(Callable<?> op) throws Exception {
        long start = System.currentTimeMillis();
        op.call();
        return System.currentTimeMillis() - start;
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

        @Value("${spring.datasource.url}")
        String jdbcUrl;
        @Value("${spring.datasource.username}")
        String user;
        @Value("${spring.datasource.password}")
        String password;

        @Bean(destroyMethod = "close")
        public DataSource customDatasource() {
            PoolConfiguration poolConfiguration = new PoolProperties();
            poolConfiguration.setUrl(jdbcUrl);
            poolConfiguration.setDriverClassName("com.mysql.jdbc.Driver");
            poolConfiguration.setUsername(user);
            poolConfiguration.setPassword(password);
            poolConfiguration.setTestOnBorrow(true);
            poolConfiguration.setTestOnReturn(true);
            return new org.apache.tomcat.jdbc.pool.DataSource(poolConfiguration);
        }
    }
}
