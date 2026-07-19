package com.sql2excel.database;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자가 지정한 경로의 JDBC jar 파일을 URLClassLoader로 로드한 뒤,
 * DriverManager에 DriverShim을 등록한다.
 */
public class DriverLoader {

    private static final Set<String> REGISTERED_JARS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private DriverLoader() {
    }

    public static void registerDriver(String jarPath, String driverClass) throws Exception {
        if (jarPath == null || jarPath.isEmpty()) {
            throw new IllegalArgumentException("jar path must not be empty");
        }

        Path path = Paths.get(jarPath).toAbsolutePath().normalize();
        if (!java.nio.file.Files.exists(path)) {
            throw new IllegalArgumentException("JDBC driver jar not found: " + path);
        }

        String absolutePath = path.toString();
        if (!REGISTERED_JARS.add(absolutePath)) {
            return;
        }

        URL jarUrl = path.toUri().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, DriverLoader.class.getClassLoader());

        try {
            Driver driver = (Driver) Class.forName(driverClass, true, classLoader)
                    .getDeclaredConstructor()
                    .newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (Exception e) {
            REGISTERED_JARS.remove(absolutePath);
            classLoader.close();
            throw e;
        }
    }
}
