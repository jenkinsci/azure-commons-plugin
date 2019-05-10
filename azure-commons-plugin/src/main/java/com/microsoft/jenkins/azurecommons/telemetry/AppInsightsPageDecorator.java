/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

import hudson.Extension;
import hudson.model.PageDecorator;
import jenkins.model.Jenkins;

import static java.util.concurrent.TimeUnit.DAYS;

@Extension
public class AppInsightsPageDecorator extends PageDecorator {
    private static final long DAY = DAYS.toMillis(1);

    /**
     * When was the last time we asked a browser to send the AI event for us.
     */
    private transient volatile long lastAttempt = -1;

    /**
     * Returns true if it's time for us to check for new version.
     */
    public boolean isDue() {
        // user opted out. no data collection.
        if (!AppInsightsGlobalConfig.get().isAppInsightsEnabled()
                || !AiProperties.enablePageDecorator()) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastAttempt > DAY) {
            lastAttempt = now;
            return true;
        }
        return false;
    }

    public void trace() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            AppInsightsPluginLoadListener.traceAzurePlugins(jenkins, AppInsightsConstants.PING);
        }
    }
}
