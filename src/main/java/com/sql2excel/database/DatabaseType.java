package com.sql2excel.database;

import com.sql2excel.config.DatabaseConfig;

import java.util.Map;

public enum DatabaseType {
    MSSQL,
    MYSQL,
    MARIADB,
    POSTGRESQL,
    SQLITE,
    ORACLE,
    TIBERO;

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
                return buildMssqlUrl(config);
            case MYSQL:
                return buildMysqlUrl(config);
            case MARIADB:
                return buildMariadbUrl(config);
            case POSTGRESQL:
                return buildPostgresUrl(config);
            case SQLITE:
                return buildSqliteUrl(config);
            case ORACLE:
                return buildOracleUrl(config);
            case TIBERO:
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

        Map<String, Object> options = config.getOptions();
        if (options != null) {
            for (String key : options.keySet()) {
                Object value = options.get(key);
                if (value == null) {
                    continue;
                }
                if ("connectionTimeout".equalsIgnoreCase(key)) {
                    sb.append("loginTimeout=").append(asSeconds(value)).append(";");
                } else if ("loginTimeout".equalsIgnoreCase(key)) {
                    sb.append("loginTimeout=").append(value).append(";");
                } else {
                    sb.append(key).append("=").append(value).append(";");
                }
            }
        }

        if (config.option("encrypt") == null) {
            sb.append("encrypt=false;");
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

    private String buildMariadbUrl(DatabaseConfig config) {
        String host = config.getServer() != null ? config.getServer() : "localhost";
        int port = config.getPort() > 0 ? config.getPort() : 3306;
        String db = config.getDatabase() != null ? config.getDatabase() : "";
        StringBuilder sb = new StringBuilder("jdbc:mariadb://")
                .append(host).append(":").append(port).append("/").append(db);
        sb.append("?useSSL=false&serverTimezone=UTC");
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
            case TIBERO:
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
                return addTopClause(sql, maxRows);
            case ORACLE:
            case TIBERO:
                return sql.trim() + " FETCH FIRST " + maxRows + " ROWS ONLY";
            case MYSQL:
            case MARIADB:
            case POSTGRESQL:
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
            case TIBERO:
                replacement = "SYSTIMESTAMP";
                break;
            case POSTGRESQL:
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
            default:
                replacement = "GETDATE()";
                break;
        }
        return sql.replaceAll("(?i)\\bGETDATE\\(\\)\\b", replacement);
    }
}
