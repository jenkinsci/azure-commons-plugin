/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

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
    public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
        req.bindJSON(this, formData);
        save();
        return true;
    }

    public boolean isAppInsightsEnabled() {
        return appInsightsEnabled;
    }

    public void setAppInsightsEnabled(final boolean appInsightsEnabled) {
        this.appInsightsEnabled = appInsightsEnabled;
    }

    public static AppInsightsGlobalConfig get() {
        return GlobalConfiguration.all().get(AppInsightsGlobalConfig.class);
    }
}
