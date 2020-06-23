package com.playtika.test.tidb;


import org.testcontainers.containers.JdbcDatabaseContainer;

public class TiDBContainer<SELF extends TiDBContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "pingcap";
    public static final String IMAGE = "tidb";
    public static final String DEFAULT_TAG = "v4.0.1";

    static final String DEFAULT_USER = "test";

    static final String DEFAULT_PASSWORD = "";

    static final Integer TIDB_PORT = 4000;
    private String databaseName = "test";
    private String username = DEFAULT_USER;
    private String password = DEFAULT_PASSWORD;
    private static final String TIDB_ROOT_USER = "root";
    private static final String MY_CNF_CONFIG_OVERRIDE_PARAM_NAME = "TC_MY_CNF";

    public TiDBContainer() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public TiDBContainer(String dockerImageName) {
        super(dockerImageName);
        addExposedPort(TIDB_PORT);
    }


    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(TIDB_PORT);
    }

    @Override
    protected void configure() {
        optionallyMapResourceParameterAsVolume(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, "/config/", "tidb-default-conf/config.toml");
        setStartupAttempts(3);
    }

    @Override
    public String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return "jdbc:mysql://" + getHost() + ":" + getMappedPort(TIDB_PORT) +
                "/" + databaseName + additionalUrlParams;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1";
    }

    public SELF withConfigurationOverride(String s) {
        parameters.put(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, s);
        return self();
    }

    @Override
    public SELF withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public SELF withUsername(final String username) {
        this.username = username;
        return self();
    }

    @Override
    public SELF withPassword(final String password) {
        this.password = password;
        return self();
    }
}
