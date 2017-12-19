/*
 Copyright 2016 Microsoft, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.microsoft.jenkins.azurecommons.core;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.jenkins.azurecommons.core.credentials.MsiTokenCredentials;
import com.microsoft.jenkins.azurecommons.core.credentials.TokenCredentialData;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Logger;

public final class AzureClientFactory {

    private static final Logger LOGGER = Logger.getLogger(AzureClientFactory.class.getName());

    public static String getUserAgent(String pluginName) {
        String version = null;
        String instanceId = null;
        try {
            version = AzureClientFactory.class.getPackage().getImplementationVersion();
            instanceId = Jenkins.getActiveInstance().getLegacyInstanceId();
        } catch (Exception e) {
        }

        if (version == null) {
            version = "local";
        }
        if (instanceId == null) {
            instanceId = "local";
        }

        return pluginName + "/" + version + "/" + instanceId;
    }

    private static AzureEnvironment createAzureEnvironment(TokenCredentialData token) {
        String envName = token.getAzureEnvironmentName();
        AzureEnvironment azureEnvironment = AzureEnvUtil.resolveAzureEnv(envName);

        AzureEnvUtil.resolveOverride(azureEnvironment,
                AzureEnvironment.Endpoint.MANAGEMENT, token.getManagementEndpoint());
        AzureEnvUtil.resolveOverride(azureEnvironment,
                AzureEnvironment.Endpoint.ACTIVE_DIRECTORY, token.getActiveDirectoryEndpoint());
        AzureEnvUtil.resolveOverride(azureEnvironment,
                AzureEnvironment.Endpoint.RESOURCE_MANAGER, token.getResourceManagerEndpoint());
        AzureEnvUtil.resolveOverride(azureEnvironment,
                AzureEnvironment.Endpoint.GRAPH, token.getGraphEndpoint());
        return azureEnvironment;
    }

    @Nonnull
    public static Azure getClient(TokenCredentialData data) throws IOException {
        AzureEnvironment env = createAzureEnvironment(data);
        if (data.getType() == TokenCredentialData.TYPE_SP) {
            return getClient(data.getClientId(),
                    data.getClientSecret(),
                    data.getTenant(),
                    data.getSubscriptionId(),
                    env);
        } else if (data.getType() == TokenCredentialData.TYPE_MSI) {
            return getClient(data.getMsiPort(), env);
        } else {
            throw new UnsupportedOperationException("Unknown data type: " + data.getType());
        }
    }

    @Nonnull
    public static Azure getClient(final String clientId,
                                  final String secret,
                                  final String tenantId,
                                  final String subId,
                                  final AzureEnvironment env) {

        ApplicationTokenCredentials token = new ApplicationTokenCredentials(
                clientId,
                tenantId,
                secret,
                env);
        return configure(Azure.configure())
                .authenticate(token)
                .withSubscription(subId);

    }

    @Nonnull
    public static Azure getClient(final int msiPort, final AzureEnvironment env) throws IOException {
        MsiTokenCredentials msiToken = new MsiTokenCredentials(msiPort, env);
        return configure(Azure.configure())
                .authenticate(msiToken)
                .withDefaultSubscription();
    }

    protected static Azure.Configurable configure(Azure.Configurable azure) {
        return azure;
    }

    private AzureClientFactory() {
    }
}
