/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

import com.microsoft.jenkins.azurecommons.AzureCommonsPlugin;
import hudson.Extension;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.RestartListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.logging.Logger;

@Extension
public class AppInsightsPluginLoadListener extends RestartListener {
    private static final Logger LOGGER = Logger.getLogger(AppInsightsPluginLoadListener.class.getName());

    @Override
    public boolean isReadyToRestart() throws IOException, InterruptedException {
        if (AiProperties.enableRestartTrace()) {
            AzureCommonsPlugin.sendEvent(AppInsightsConstants.JENKINS, AppInsightsConstants.RESTART);
        }
        return true;
    }

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void onPluginsLoaded(final Jenkins jenkins) {
        Thread.UncaughtExceptionHandler exceptionHandler = (th, ex) ->
                LOGGER.severe("Exception in sending load events: " + ex);
        Thread thread = new Thread(() -> traceAzurePlugins(jenkins, AppInsightsConstants.LOAD));
        thread.setUncaughtExceptionHandler(exceptionHandler);
        thread.start();
    }

    public static void traceAzurePlugins(final Jenkins jenkins, final String action) {
        for (PluginWrapper wrapper : jenkins.getPluginManager().getPlugins()) {
            Plugin plugin = wrapper.getPlugin();
            if (plugin == null) {
                continue;
            }

            if (plugin instanceof AppInsightsRecordable || isMicrosoftPlugin(wrapper)) {
                AppInsightsClient client = AppInsightsClientFactory.getInstance(plugin.getClass());
                client.sendEvent(AppInsightsConstants.PLUGIN, action, null, false);
            }
        }
    }

    private static boolean isMicrosoftPlugin(PluginWrapper wrapper) {
        final String pluginClass = wrapper.getPluginClass();
        // might includes plugins owned by other MS teams
        return pluginClass != null && pluginClass.toLowerCase().contains("com.microsoft");
    }

    public interface AppInsightsRecordable {

    }
}
