/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

public class AppInsightsConstants {
    // AI event prefix
    public static final String DEFAULT_EVENT_PREFIX = "AzureJenkinsPlugin";
    public static final String EVENT_NAME_SEPARATOR = ".";

    // common properties for AI events
    public static final String PROP_JENKINS_INSTAMCE_ID = "JenkinsInstanceId";
    public static final String PROP_JENKINS_VERSION = "JenkinsVersion";
    public static final String PROP_PLUGIN_NAME = "PluginName";
    public static final String PROP_PLUGIN_VERSION = "PluginVersion";
    public static final String JENKINS = "Jenkins";
    public static final String PLUGIN = "Plugin";
    public static final String RESTART = "Restart";
    public static final String LOAD= "Load";
    public static final String PING= "Ping";

    // items or properties related to Azure
    public static final String AZURE_SUBSCRIPTION_ID = "SubscriptionId";
    public static final String AZURE_APP_INSIGHTS = "AppInsights";
    public static final String AZURE_BLOB_STORAGE = "BlobStorage";
    public static final String AZURE_FILE_STORAGE = "FileStorage";
    public static final String AZURE_REST = "AzureRestAPI";
    public static final String AZURE_LOCATION = "Location";
    public static final String AZURE_CONTAINER_SERVICE = "AzureContainerService";

    public static final String DOCKER = "Docker";
}
