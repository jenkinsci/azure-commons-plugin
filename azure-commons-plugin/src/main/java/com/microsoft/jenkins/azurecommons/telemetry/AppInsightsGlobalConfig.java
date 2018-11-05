/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

import com.microsoft.jenkins.azurecommons.AzureCommonsPlugin;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public final class AppInsightsGlobalConfig extends GlobalConfiguration {
    private static final String APP_INSIGHTS_CONFIG_ID = "app-insights-plugin-configuration";

    private boolean appInsightsEnabled = true;

    public AppInsightsGlobalConfig() {
        load();
    }

    @DataBoundConstructor
    public AppInsightsGlobalConfig(boolean appInsightsEnabled) {
        this.appInsightsEnabled = appInsightsEnabled;
    }

    @Override
    public String getDisplayName() {
        return "Azure";
    }

    /*
    To avoid long class name as id in xml tag name and config file
     */
    @Override
    public String getId() {
        return APP_INSIGHTS_CONFIG_ID;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        // send events to AppInsights in case config updated.
        // it's mandatory since we want to know the AI is explicitly enabled/disabled by user.
        boolean newValue = formData.getBoolean("appInsightsEnabled");
        if (newValue != this.appInsightsEnabled) {
            final String action = newValue ? "Enable" : "Disable";
            AzureCommonsPlugin.sendEvent(AppInsightsConstants.AZURE_APP_INSIGHTS, action, null, true);
        }

        // update and persist config
        req.bindJSON(this, formData);
        save();

        return true;
    }

    public boolean isAppInsightsEnabled() {
        return appInsightsEnabled;
    }

    public void setAppInsightsEnabled(boolean appInsightsEnabled) {
        this.appInsightsEnabled = appInsightsEnabled;
    }

    public static AppInsightsGlobalConfig get() {
        return GlobalConfiguration.all().get(AppInsightsGlobalConfig.class);
    }
}
