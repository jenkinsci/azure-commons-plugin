/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

import hudson.util.VersionNumber;
import jenkins.model.Jenkins;

public abstract class AppInsightsClientConfiguration {
    public String jenkinsInstanceId() {
        return Jenkins.getActiveInstance().getLegacyInstanceId();
    }

    public String jenkinsVersion() {
        final VersionNumber version = Jenkins.getVersion();
        return version == null ? null : version.toString();
    }

    /*
    Override instrumentationKey() if you are connecting to another AI.
     */
    public String instrumentationKey() {
        return null;
    }

    public abstract String pluginName();

    public abstract String pluginVersion();

    public String eventNamePrefix() {
        return AppInsightsConstants.DEFAULT_EVENT_PREFIX;
    }
}
