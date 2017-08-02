/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;


import com.microsoft.applicationinsights.TelemetryClient;
import hudson.Plugin;
import hudson.model.Computer;
import hudson.node_monitors.ArchitectureMonitor;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
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

    private String instrumentationKey = null;
    private String eventNamePrefix = AppInsightsConstants.DEFAULT_EVENT_PREFIX;

    private final Plugin plugin;
    private TelemetryClient telemetryClient;

    public AppInsightsClient(final Plugin plugin) {
        this.plugin = plugin;
    }

    public void sendEvent(final String item, final String action, final Map<String, String> properties, final boolean force) {
        try {
            if (this.plugin != null && (AppInsightsGlobalConfig.get().isAppInsightsEnabled() || force)) {
                final String eventName = buildEventName(item, action);
                final Map<String, String> formalizedProperties = formalizeProperties(properties);

                final TelemetryClient telemetryClient = getTelemetryClient();
                telemetryClient.trackEvent(eventName, formalizedProperties, null);
                telemetryClient.flush();
                LOGGER.info("AI: " + eventName);
            }
        } catch (Exception e) {
            LOGGER.warning("Fail to send trace to App Insights due to:" + e.getMessage());
        }
    }

    /*
       Override instrumentationKey() if you are connecting to another AI.
    */
    public AppInsightsClient withInstrumentationKey(final String instrumentationKey) {
        checkNotNull(instrumentationKey, "Parameter instrumentationKey is null.");
        this.instrumentationKey = instrumentationKey;
        if (telemetryClient != null) {
            telemetryClient.getContext().setInstrumentationKey(instrumentationKey);
        }
        return this;
    }

    public AppInsightsClient withEventNamePrefix(final String eventNamePrefix) {
        checkNotNull(eventNamePrefix, "Parameter event name is null.");
        this.eventNamePrefix = eventNamePrefix;
        return this;
    }

    private TelemetryClient getTelemetryClient() {
        if (telemetryClient == null) {
            telemetryClient = new TelemetryClient();
            if (StringUtils.isNotBlank(instrumentationKey)) {
                telemetryClient.getContext().setInstrumentationKey(instrumentationKey);
            }
        }

        return telemetryClient;
    }

    private String buildEventName(final String item, final String action) {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(eventNamePrefix)
                .append(AppInsightsConstants.EVENT_NAME_SEPARATOR).append(plugin.getWrapper().getShortName());

        if (StringUtils.isNotBlank(item))
            stringBuilder.append(AppInsightsConstants.EVENT_NAME_SEPARATOR).append(item);

        if (StringUtils.isNotBlank(action))
            stringBuilder.append(AppInsightsConstants.EVENT_NAME_SEPARATOR).append(action);

        return stringBuilder.toString();
    }

    private Map<String, String> formalizeProperties(Map<String, String> properties) {
        if (properties == null)
            properties = new HashMap<>();

        putJenkinsInfo(properties);
        properties.put(AppInsightsConstants.PROP_PLUGIN_NAME, plugin.getWrapper().getDisplayName());
        properties.put(AppInsightsConstants.PROP_PLUGIN_VERSION, plugin.getWrapper().getVersion());

        // Telemetry client doesn't accept null value for ConcurrentHashMap doesn't accept null key or null value.
        for (final Iterator<Map.Entry<String, String>> iter = properties.entrySet().iterator(); iter.hasNext(); ) {
            final Map.Entry<String, String> entry = iter.next();
            if (StringUtils.isBlank(entry.getKey()) || StringUtils.isBlank(entry.getValue())) {
                iter.remove();
            }
        }

        return properties;
    }

    private void putJenkinsInfo(final Map<String, String> properties) {
        final Jenkins j = Jenkins.getInstance();
        if (j == null) {
            properties.put(AppInsightsConstants.PROP_JENKINS_INSTAMCE_ID, "local");
            properties.put(AppInsightsConstants.PROP_JENKINS_VERSION, "local");
            return;
        }

        properties.put(AppInsightsConstants.PROP_JENKINS_INSTAMCE_ID, j.getLegacyInstanceId());
        properties.put(AppInsightsConstants.PROP_JENKINS_VERSION, j.VERSION);
        for (Computer c : j.getComputers()) {
            if (c.getNode() == j) {
                properties.put("master", "true");
                properties.put("jvm-vendor", System.getProperty("java.vm.vendor"));
                properties.put("jvm-name", System.getProperty("java.vm.name"));
                properties.put("jvm-version", System.getProperty("java.version"));
            }
            ArchitectureMonitor.DescriptorImpl descriptor = j.getDescriptorByType(ArchitectureMonitor.DescriptorImpl.class);
            properties.put("os", descriptor.get(c));
        }
    }
}
