package com.microsoft.jenkins.azurecommons.telemetry;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class AiProperties {
    private static final Logger LOGGER = Logger.getLogger(AiProperties.class.getName());
    private static final String INSTRUMENTATION_KEY_NAME = "InstrumentationKey";
    private static final String ENABLE_AZURE_API_TRACE_NAME = "EnableAzureApiTrace";
    private static final String ENABLE_PAGE_DECORATOR_NAME = "EnablePageDecorator";
    private static final String FILTERED_EVENTS_NAME = "FilteredEvents";

    private static final Properties prop = new Properties();

    static {
        try {
            prop.load(AiProperties.class.getClassLoader().getResourceAsStream("ai.properties"));
        } catch (IOException e) {
            LOGGER.severe("Failed to load Ai property file.");
        }
    }

    public static String getInstrumentationKey() {
        return prop.getProperty(INSTRUMENTATION_KEY_NAME);
    }

    public static boolean enableAzureApiTrace() {
        String property = prop.getProperty(ENABLE_AZURE_API_TRACE_NAME);
        return Boolean.parseBoolean(property);
    }

    public static boolean enablePageDecorator() {
        String property = prop.getProperty(ENABLE_PAGE_DECORATOR_NAME);
        return Boolean.parseBoolean(property);
    }

    public static String getFilteredEvents() {
        return prop.getProperty(FILTERED_EVENTS_NAME);
    }
}
