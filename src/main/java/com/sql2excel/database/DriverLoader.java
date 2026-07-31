package com.sql2excel.database;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 사용자가 지정한 경로의 JDBC jar 파일을 URLClassLoader로 로드한 뒤,
 * DriverManager에 DriverShim을 등록한다.
 */
public class DriverLoader {

    private static final Set<String> REGISTERED_DRIVERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private DriverLoader() {
    }

    public static void loadDriver(String jarPath, String driverClass) throws Exception {
        if (driverClass == null || driverClass.isEmpty()) {
            return;
        }

        if (jarPath == null || jarPath.isEmpty()) {
            loadDriverFromClasspath(driverClass);
            return;
        }

        Path path = Paths.get(jarPath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            Path fallback = findJarInM2(path);
            if (fallback == null) {
                throw new IllegalArgumentException("JDBC driver jar not found: " + path);
            }
            path = fallback;
        }

        String key = path.toString() + "|" + driverClass;
        if (!REGISTERED_DRIVERS.add(key)) {
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
            REGISTERED_DRIVERS.remove(key);
            classLoader.close();
            throw e;
        }
    }

    private static void loadDriverFromClasspath(String driverClass) throws ClassNotFoundException {
        if (REGISTERED_DRIVERS.add("classpath|" + driverClass)) {
            Class.forName(driverClass);
        }
    }

    private static Path findJarInM2(Path requested) {
        Path m2 = Paths.get(System.getProperty("user.home"), ".m2", "repository");
        if (!Files.isDirectory(m2)) {
            return null;
        }
        String base = requested.getFileName() != null ? requested.getFileName().toString() : null;
        if (base == null || !base.endsWith(".jar")) {
            return null;
        }
        String prefix = jarNamePrefix(base);
        try (Stream<Path> stream = Files.walk(m2)) {
            List<Path> candidates = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName() != null
                            && p.getFileName().toString().endsWith(".jar")
                            && !p.getFileName().toString().endsWith("-sources.jar")
                            && !p.getFileName().toString().endsWith("-javadoc.jar")
                            && !p.getFileName().toString().endsWith("-tests.jar")
                            && p.getFileName().toString().startsWith(prefix))
                    .collect(Collectors.toList());
            if (candidates.isEmpty()) {
                return null;
            }
            candidates.sort((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
            return candidates.get(0);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String jarNamePrefix(String jarName) {
        int dot = jarName.lastIndexOf('.');
        String name = dot > 0 ? jarName.substring(0, dot) : jarName;
        int dash = name.lastIndexOf('-');
        while (dash > 0) {
            String maybeVersion = name.substring(dash + 1);
            if (maybeVersion.matches("[0-9].*") || maybeVersion.isEmpty()) {
                name = name.substring(0, dash);
                dash = name.lastIndexOf('-');
            } else {
                break;
            }
        }
        return name;
    }
}
