package com.erp.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigReader {

    private static final Properties PROPERTIES = new Properties();

    static {
        loadProperties();
    }

    private ConfigReader() {
        // Prevent object creation
    }

    private static void loadProperties() {

        try (InputStream input =
                     ConfigReader.class
                             .getClassLoader()
                             .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new RuntimeException(
                        "config.properties file was not found."
                );
            }

            PROPERTIES.load(input);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to load config.properties", e
            );
        }
    }

    public static String get(String key) {

        String value = PROPERTIES.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new RuntimeException(
                    "Configuration key not found: " + key
            );
        }

        return value.trim();
    }

    public static boolean getBoolean(String key) {

        return Boolean.parseBoolean(get(key));
    }

    public static int getInt(String key) {

        return Integer.parseInt(get(key));
    }
}