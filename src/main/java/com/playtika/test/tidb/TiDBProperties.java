package com.playtika.test.tidb;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.tidb")
public class TiDBProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_TIDB = "embeddedTiDB";
    String dockerImage = "pingcap/tidb:v4.0.1";

    String user = "root";
    String password = "";
    String database = "test";
    String schema = "test";
    String host = "localhost";
    int port = 4000;
    int port1 = 10080;
    String initScriptPath;
}
