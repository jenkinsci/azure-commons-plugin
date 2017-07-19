/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;


import com.microsoft.applicationinsights.TelemetryClient;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AppInsightsClient {
    private AppInsightsClientConfiguration configuration;
    private TelemetryClient telemetryClient;

    public AppInsightsClient(final AppInsightsClientConfiguration configuration) {
        if (configuration == null)
            throw new NullArgumentException("AppInsights configuration is null.");

        this.configuration = configuration;
    }

    public void createEvent(final String item, final String action, final Map<String, String> properties) {
        createEvent(item, action, properties, false);
    }

    public void createEvent(final String item, final String action, final Map<String, String> properties, final boolean force) {
        // TODO check the global configuration whether AI is disabled

        final String eventName = buildEventName(item, action);
        final Map<String, String> formalizedProperties = formalizeProperties(properties);
        final TelemetryClient telemetryClient = getTelemetryClient();
        telemetryClient.trackEvent(eventName, formalizedProperties, null);
        telemetryClient.flush();
    }

    private TelemetryClient getTelemetryClient() {
        if (telemetryClient == null) {
            telemetryClient = new TelemetryClient();
            if (StringUtils.isNotBlank(configuration.instrumentationKey())) {
                telemetryClient.getContext().setInstrumentationKey(configuration.instrumentationKey());
            }
        }

        return telemetryClient;
    }

    private String buildEventName(final String item, final String action) {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(configuration.eventNamePrefix())
                .append(AppInsightsConstants.EVENT_NAME_SEPARATOR).append(configuration.pluginName());

        if (StringUtils.isNotBlank(item))
            stringBuilder.append(AppInsightsConstants.EVENT_NAME_SEPARATOR).append(item);

        if (StringUtils.isNotBlank(action))
            stringBuilder.append(AppInsightsConstants.EVENT_NAME_SEPARATOR).append(action);

        return stringBuilder.toString();
    }

    private Map<String, String> formalizeProperties(final Map<String, String> properties) {
        final Map<String, String> props = properties == null ? new HashMap<String, String>() : properties;

        properties.put(AppInsightsConstants.PROP_JENKINS_INSTAMCE_ID, configuration.jenkinsInstanceId());
        properties.put(AppInsightsConstants.PROP_JENKINS_VERSION, configuration.jenkinsVersion());
        properties.put(AppInsightsConstants.PROP_PLUGIN_NAME, configuration.pluginName());
        properties.put(AppInsightsConstants.PROP_PLUGIN_VERSION, configuration.pluginVersion());

        // Telemetry client doesn't accept null value for ConcurrentHashMap doesn't accept null key or null value.
        for (final Iterator<Map.Entry<String, String>> iter = properties.entrySet().iterator(); iter.hasNext(); ) {
            final Map.Entry<String, String> entry = iter.next();
            if (StringUtils.isBlank(entry.getKey()) || StringUtils.isBlank(entry.getValue())) {
                iter.remove();
            }
        }

        return props;
    }
}
