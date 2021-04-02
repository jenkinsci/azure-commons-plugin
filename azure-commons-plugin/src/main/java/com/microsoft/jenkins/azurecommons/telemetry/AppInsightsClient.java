/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;


import hudson.Main;
import hudson.Plugin;
import hudson.model.Computer;
import hudson.node_monitors.ArchitectureMonitor;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Client for App Insights.
 * To enable AI in local env(mvn hpi:run), you need set env variable first: export APPLICATION_INSIGHTS_IKEY=`your key`.
 */
public class AppInsightsClient {
    private static final Logger LOGGER = Logger.getLogger(AppInsightsClient.class.getName());
    private static final String INSTRUMENTATION_KEY_NAME = "InstrumentationKey";

    private String instrumentationKey = null;
    private String eventNamePrefix = AppInsightsConstants.DEFAULT_EVENT_PREFIX;

    private final Plugin plugin;
    private JenkinsTelemetryClient telemetryClient;

    public AppInsightsClient(Plugin plugin) {
        this.plugin = plugin;

        // If in development mode, use the test AI instrumentation key.
        //
        // This works when we develop other plugins that depend on azure-commons. In that case azure-commons is
        // fetched as a dependency from jenkins-ci with the production instrumentation key, so if without this
        // injection the telemetry data generated in development mode will go to the production sink.
        //
        // http://jenkins-ci.361315.n4.nabble.com/How-to-check-if-plugin-is-running-in-development-mode-td4739906.html
        if (Main.isDevelopmentMode || Boolean.getBoolean("hudson.hpi.run")) {
            // Ensure that the instrumentation key is not overwritten explicitly.
            //
            // https://docs.microsoft.com/en-us/azure/application-insights/app-insights-java-get-started
            // #alternative-ways-to-set-the-instrumentation-key
            if (System.getProperty("APPLICATION_INSIGHTS_IKEY") == null
                    && System.getenv("APPLICATION_INSIGHTS_IKEY") == null) {
                // Inject the test AI instrumentation key.
                this.instrumentationKey = "712adcab-2593-48c6-8367-8a940f483bc1";
                LOGGER.fine("Use test AI instrumentation key for " + plugin.getClass().getName());
            }
        } else {
            String ikey = System.getProperty("APPLICATION_INSIGHTS_IKEY");
            if (ikey == null) {
                ikey = System.getenv("APPLICATION_INSIGHTS_IKEY");
            }
            if (ikey != null) {
                this.instrumentationKey = ikey;
                LOGGER.fine("Use AI instrumentation key from system properties or environment variables.");
            } else {
                this.instrumentationKey = AiProperties.getInstrumentationKey();
            }
        }
        telemetryClient = new JenkinsTelemetryClient(this.instrumentationKey);
    }

    /**
     * No-OP, first step of retiring analytics.
     */
    public void sendEvent(String item, String action, Map<String, String> properties, boolean force) {
    }

    /*
       Override instrumentationKey() if you are connecting to another AI.
    */
    public AppInsightsClient withInstrumentationKey(String key) {
        checkNotNull(key, "Parameter instrumentation key is null.");
        this.instrumentationKey = key;
        this.telemetryClient.setInstrumentKey(key);
        return this;
    }

    public AppInsightsClient withEventNamePrefix(String prefix) {
        checkNotNull(prefix, "Parameter event name prefix is null.");
        this.eventNamePrefix = prefix;
        return this;
    }

    private JenkinsTelemetryClient getTelemetryClient() {
        return telemetryClient;
    }

    private String buildEventName(String item, String action) {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(eventNamePrefix)
                .append(AppInsightsConstants.EVENT_NAME_SEPARATOR).append(plugin.getWrapper().getShortName());

        if (StringUtils.isNotBlank(item)) {
            stringBuilder.append(AppInsightsConstants.EVENT_NAME_SEPARATOR).append(item);
        }

        if (StringUtils.isNotBlank(action)) {
            stringBuilder.append(AppInsightsConstants.EVENT_NAME_SEPARATOR).append(action);
        }

        return stringBuilder.toString();
    }

    private Map<String, String> formalizeProperties(Map<String, String> properties) {
        if (properties == null) {
            properties = new HashMap<>();
        }

        putJenkinsInfo(properties);
        properties.put(AppInsightsConstants.PROP_PLUGIN_NAME, plugin.getWrapper().getDisplayName());
        properties.put(AppInsightsConstants.PROP_PLUGIN_VERSION, plugin.getWrapper().getVersion());

        // Telemetry client doesn't accept null value for ConcurrentHashMap doesn't accept null key or null value.
        for (Iterator<Map.Entry<String, String>> iter = properties.entrySet().iterator(); iter.hasNext(); ) {
            final Map.Entry<String, String> entry = iter.next();
            if (StringUtils.isBlank(entry.getKey()) || StringUtils.isBlank(entry.getValue())) {
                iter.remove();
            }
        }

        return properties;
    }

    private void putJenkinsInfo(final Map<String, String> properties) {
        final Jenkins j = Jenkins.getInstanceOrNull();
        if (j == null) {
            properties.put(AppInsightsConstants.PROP_JENKINS_INSTANCE_ID, "local");
            properties.put(AppInsightsConstants.PROP_JENKINS_VERSION, "local");
            return;
        }

        properties.put(AppInsightsConstants.PROP_JENKINS_INSTANCE_ID, j.getLegacyInstanceId());
        properties.put(AppInsightsConstants.PROP_JENKINS_VERSION, j.VERSION);
        for (Computer c : j.getComputers()) {
            if (c.getNode() == j) {
                properties.put("master", "true");
                properties.put("jvm-vendor", System.getProperty("java.vm.vendor"));
                properties.put("jvm-name", System.getProperty("java.vm.name"));
                properties.put("jvm-version", System.getProperty("java.version"));
            }
            ArchitectureMonitor.DescriptorImpl descriptor =
                    j.getDescriptorByType(ArchitectureMonitor.DescriptorImpl.class);
            properties.put("os", descriptor.get(c));
        }
    }
}
