/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons;

import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsClientFactory;
import hudson.Plugin;

import java.util.HashMap;
import java.util.Map;

public class AzureCommonsPlugin extends Plugin {
    public static void sendEvent(String item, String action, String... properties) {
        final Map<String, String> props = new HashMap<>();
        for (int i = 1; i < properties.length; i += 2) {
            props.put(properties[i - 1], properties[i]);
        }
        sendEvent(item, action, props, false);
    }

    public static void sendEvent(String item, String action, Map<String, String> properties, boolean force) {
        AppInsightsClientFactory.getInstance(AzureCommonsPlugin.class)
                .sendEvent(item, action, properties, force);
    }
}
