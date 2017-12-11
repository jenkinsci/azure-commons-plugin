/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

import hudson.Plugin;
import jenkins.model.Jenkins;

import java.util.HashMap;
import java.util.Map;

public final class AppInsightsClientFactory {
    private static Map<Class<? extends Plugin>, AppInsightsClient> appInsightsClientMap = new HashMap<>();
    private static Object lock = new Object();

    public static AppInsightsClient getInstance(Class<? extends Plugin> clazz) {
        if (!appInsightsClientMap.containsKey(clazz)) {
            synchronized (lock) {
                if (!appInsightsClientMap.containsKey(clazz)) {
                    Plugin plugin = null;
                    final Jenkins jenkins = Jenkins.getInstance();
                    if (jenkins != null) {
                        plugin = jenkins.getPlugin(clazz);
                    }
                    if (plugin == null) {
                        plugin = new Plugin.DummyImpl();
                    }
                    appInsightsClientMap.put(clazz, new AppInsightsClient(plugin));
                }
            }
        }

        return appInsightsClientMap.get(clazz);
    }

    private AppInsightsClientFactory() {
        // hide constructor
    }
}
