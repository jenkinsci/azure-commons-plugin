/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons;

import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsClientFactory;
import hudson.Plugin;

import java.util.Map;

public class AzureCommonsPlugin extends Plugin {
    public static void sendEvent(final String item, final String action, final Map<String, String> properties) {
        sendEvent(item, action, properties, false);
    }

    public static void sendEvent(final String item, final String action, final Map<String, String> properties, final boolean force) {
        AppInsightsClientFactory.getInstance(AzureCommonsPlugin.class)
                .sendEvent(item, action, properties, force);
    }
}
