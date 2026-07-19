package com.sql2excel.database;

import com.sql2excel.config.DatabaseConfig;

public enum DatabaseType {
    MSSQL("com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    MYSQL("com.mysql.cj.jdbc.Driver"),
    MARIADB("org.mariadb.jdbc.Driver"),
    POSTGRESQL("org.postgresql.Driver"),
    POSTGRES("org.postgresql.Driver"),
    PG("org.postgresql.Driver"),
    SQLITE("org.sqlite.JDBC"),
    ORACLE("oracle.jdbc.OracleDriver"),
    ORACLEDB("oracle.jdbc.OracleDriver"),
    OCI("oracle.jdbc.OracleDriver"),
    TIBERO("com.tmax.tibero.jdbc.TiberoDriver"),
    TIBERO6("com.tmax.tibero.jdbc.TiberoDriver"),
    TIBERO7("com.tmax.tibero.jdbc.TiberoDriver");

    private final String defaultDriverClass;

    DatabaseType(String defaultDriverClass) {
        this.defaultDriverClass = defaultDriverClass;
    }

    public String getDefaultDriverClass() {
        return defaultDriverClass;
    }

    public static DatabaseType fromString(String type) {
        if (type == null || type.isEmpty()) {
            return MSSQL;
        }
        for (DatabaseType t : values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unsupported database type: " + type);
    }

    public String buildJdbcUrl(DatabaseConfig config) {
        if (config.getConnectString() != null && !config.getConnectString().isEmpty()) {
            String url = config.getConnectString();
            if (url.startsWith("jdbc:")) {
                return url;
            }
        }

        switch (this) {
            case MSSQL:
            case SQLSERVER:
                return buildMssqlUrl(config);
            case MYSQL:
            case MARIADB:
                return buildMysqlUrl(config);
            case POSTGRESQL:
            case POSTGRES:
            case PG:
                return buildPostgresUrl(config);
            case SQLITE:
                return buildSqliteUrl(config);
            case ORACLE:
            case ORACLEDB:
            case OCI:
                return buildOracleUrl(config);
            case TIBERO:
            case TIBERO6:
            case TIBERO7:
                return buildTiberoUrl(config);
            default:
                throw new IllegalStateException("No URL builder for " + this);
        }
    }

    private String buildMssqlUrl(DatabaseConfig config) {
        String host = config.getServer() != null ? config.getServer() : "localhost";
        int port = config.getPort() > 0 ? config.getPort() : 1433;
        StringBuilder sb = new StringBuilder("jdbc:sqlserver://")
                .append(host).append(":").append(port).append(";");
        if (config.getDatabase() != null && !config.getDatabase().isEmpty()) {
            sb.append("databaseName=").append(config.getDatabase()).append(";");
        }
        sb.append("encrypt=false;");
        if (config.option("trustServerCertificate") != null) {
            sb.append("trustServerCertificate=").append(config.option("trustServerCertificate")).append(";");
        }
        if (config.option("loginTimeout") != null) {
            sb.append("loginTimeout=").append(config.option("loginTimeout")).append(";");
        } else if (config.option("connectionTimeout") != null) {
            sb.append("loginTimeout=").append(asSeconds(config.option("connectionTimeout"))).append(";");
        }
        return sb.toString();
    }

    private String buildMysqlUrl(DatabaseConfig config) {
        String host = config.getServer() != null ? config.getServer() : "localhost";
        int port = config.getPort() > 0 ? config.getPort() : 3306;
        String db = config.getDatabase() != null ? config.getDatabase() : "";
        StringBuilder sb = new StringBuilder("jdbc:mysql://")
                .append(host).append(":").append(port).append("/").append(db);
        sb.append("?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        if (config.option("connectionTimeout") != null) {
            sb.append("&connectTimeout=").append(config.option("connectionTimeout"));
        }
        return sb.toString();
    }

    private String buildPostgresUrl(DatabaseConfig config) {
        String host = config.getServer() != null ? config.getServer() : "localhost";
        int port = config.getPort() > 0 ? config.getPort() : 5432;
        String db = config.getDatabase() != null ? config.getDatabase() : "postgres";
        StringBuilder sb = new StringBuilder("jdbc:postgresql://")
                .append(host).append(":").append(port).append("/").append(db);
        if (config.option("ssl") != null) {
            sb.append("?ssl=").append(config.option("ssl"));
        }
        return sb.toString();
    }

    private String buildSqliteUrl(DatabaseConfig config) {
        String path = config.getDatabase() != null ? config.getDatabase() : config.getServer();
        if (path == null || path.isEmpty()) {
            path = ":memory:";
        }
        return "jdbc:sqlite:" + path;
    }

    private String buildOracleUrl(DatabaseConfig config) {
        String host = config.getServer() != null ? config.getServer() : "localhost";
        int port = config.getPort() > 0 ? config.getPort() : 1521;
        String serviceName = config.getServiceName();
        String sid = config.getSid();
        if (serviceName != null && !serviceName.isEmpty()) {
            return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + serviceName;
        } else if (sid != null && !sid.isEmpty()) {
            return "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
        } else if (config.getDatabase() != null && !config.getDatabase().isEmpty()) {
            return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + config.getDatabase();
        } else {
            throw new IllegalArgumentException("Oracle requires serviceName, sid, or database");
        }
    }

    private String buildTiberoUrl(DatabaseConfig config) {
        String host = config.getServer() != null ? config.getServer() : "localhost";
        int port = config.getPort() > 0 ? config.getPort() : 8629;
        String db = config.getDatabase() != null ? config.getDatabase()
                : (config.getServiceName() != null ? config.getServiceName() : config.getSid());
        if (db == null || db.isEmpty()) {
            throw new IllegalArgumentException("Tibero requires database, serviceName, or sid");
        }
        return "jdbc:tibero:thin:@" + host + ":" + port + ":" + db;
    }

    private int asSeconds(Object msOrSeconds) {
        if (msOrSeconds == null) {
            return 30;
        }
        int value = msOrSeconds instanceof Number ? ((Number) msOrSeconds).intValue()
                : Integer.parseInt(msOrSeconds.toString());
        return value > 1000 ? value / 1000 : value;
    }

    public String getTestQuery() {
        switch (this) {
            case ORACLE:
            case ORACLEDB:
            case OCI:
            case TIBERO:
            case TIBERO6:
            case TIBERO7:
                return "SELECT 1 FROM dual";
            default:
                return "SELECT 1";
        }
    }

    public String applyLimit(String sql, int maxRows) {
        if (maxRows <= 0) {
            return sql;
        }
        String upper = sql.trim().toUpperCase();
        if (upper.contains("LIMIT") || upper.contains("FETCH FIRST") || upper.contains("TOP") || upper.contains("ROWNUM")) {
            return sql;
        }
        switch (this) {
            case MSSQL:
            case SQLSERVER:
                return addTopClause(sql, maxRows);
            case ORACLE:
            case ORACLEDB:
            case OCI:
            case TIBERO:
            case TIBERO6:
            case TIBERO7:
                return sql.trim() + " FETCH FIRST " + maxRows + " ROWS ONLY";
            case MYSQL:
            case MARIADB:
            case POSTGRESQL:
            case POSTGRES:
            case PG:
            case SQLITE:
            default:
                return sql.trim() + " LIMIT " + maxRows;
        }
    }

    private String addTopClause(String sql, int maxRows) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        int selectIndex = upper.indexOf("SELECT");
        if (selectIndex < 0) {
            return trimmed;
        }
        int afterSelect = selectIndex + 6;
        // Skip whitespace
        int i = afterSelect;
        while (i < trimmed.length() && Character.isWhitespace(trimmed.charAt(i))) {
            i++;
        }
        // Handle DISTINCT/ALL modifier
        StringBuilder prefix = new StringBuilder(trimmed.substring(0, i));
        prefix.append(" TOP (").append(maxRows).append(") ");
        prefix.append(trimmed.substring(i));
        return prefix.toString();
    }

    public String replaceGetDate(String sql) {
        if (sql == null) {
            return null;
        }
        String replacement;
        switch (this) {
            case ORACLE:
            case ORACLEDB:
            case OCI:
            case TIBERO:
            case TIBERO6:
            case TIBERO7:
                replacement = "SYSTIMESTAMP";
                break;
            case POSTGRESQL:
            case POSTGRES:
            case PG:
                replacement = "NOW()";
                break;
            case MYSQL:
            case MARIADB:
                replacement = "NOW()";
                break;
            case SQLITE:
                replacement = "datetime('now')";
                break;
            case MSSQL:
            case SQLSERVER:
            default:
                replacement = "GETDATE()";
                break;
        }
        return sql.replaceAll("(?i)\\bGETDATE\\(\\)\\b", replacement);
    }
}
