package com.microsoft.jenkins.azurecommons.telemetry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;

@SuppressFBWarnings
public class TelemetryEventData {
    private final String name = "Microsoft.ApplicationInsights.Event";

    @JsonProperty("iKey")
    private final String instrumentationKey;

    private final Tags tags = new Tags();

    private final EventData data = new EventData();

    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
        sdf.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        return sdf.format(System.currentTimeMillis());
    }

    private final String time = getCurrentTime();

    @JsonCreator
    public TelemetryEventData(String eventName, Map<String, String> properties, String instrumentationKey) {
        Assert.hasText(eventName, "Event name should contain text.");
        Assert.hasText(instrumentationKey, "Instrumentation key should contain text.");

        this.instrumentationKey = instrumentationKey;
        this.data.getBaseData().setName(eventName);
        this.data.getBaseData().setProperties(properties);
    }

    private static class Tags {
        @JsonProperty("ai.cloud.roleInstance")
        private final String aiCloudRoleInstance = "Jenkins-on-azure";

        /**
         * Just make compatible with Application Insights SDK, this value is const in the version this plugin used.
         */
        @JsonProperty("ai.internal.sdkVersion")
        private final String aiInternalSdkVersion = "Java 1.0.3";
    }

    public String getName() {
        return name;
    }

    public String getInstrumentationKey() {
        return instrumentationKey;
    }

    public Tags getTags() {
        return tags;
    }

    public EventData getData() {
        return data;
    }

    public String getTime() {
        return time;
    }

    private static class EventData {

        private final String baseType = "EventData";

        private final CustomData baseData = new CustomData();

        private static class CustomData {

            private final Integer ver = 2;

            private String name;

            public void setName(String name) {
                this.name = name;
            }

            public void setProperties(Map<String, String> properties) {
                this.properties = properties;
            }

            private Map<String, String> properties;

            public Integer getVer() {
                return ver;
            }

            public String getName() {
                return name;
            }

            public Map<String, String> getProperties() {
                return properties;
            }
        }

        public CustomData getBaseData() {
            return baseData;
        }

        public String getBaseType() {
            return baseType;
        }
    }
}
