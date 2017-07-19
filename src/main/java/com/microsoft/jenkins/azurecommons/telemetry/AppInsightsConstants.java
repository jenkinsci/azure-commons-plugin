/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

public class AppInsightsConstants {
    public static String DEFAULT_EVENT_PREFIX = "AzureJenkinsPlugin";
    public static String EVENT_NAME_SEPARATOR = ".";

    public static String PROP_JENKINS_INSTAMCE_ID = "JenkinsInstanceId";
    public static String PROP_JENKINS_VERSION = "JenkinsVersion";
    public static String PROP_PLUGIN_NAME = "PluginName";
    public static String PROP_PLUGIN_VERSION = "PluginVersion";
}
