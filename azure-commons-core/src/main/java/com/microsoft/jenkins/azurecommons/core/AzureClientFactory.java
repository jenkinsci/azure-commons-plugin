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
import com.microsoft.jenkins.azurecommons.core.credentials.RemoteMsiTokenCredentials;
import com.microsoft.jenkins.azurecommons.core.credentials.TokenCredentialData;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.Proxy;
import java.util.logging.Logger;

public final class AzureClientFactory {

    private static final Logger LOGGER = Logger.getLogger(AzureClientFactory.class.getName());

    public static String getUserAgent(String pluginName, String version) {
        String instanceId = null;
        try {
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
    public static Azure getClient(TokenCredentialData data) {
        return getClient(data, null);
    }


    @Nonnull
    public static Azure getClient(TokenCredentialData data, Configurer configurer) {
        AzureEnvironment env = createAzureEnvironment(data);
        if (data.getType() == TokenCredentialData.TYPE_SP) {
            byte[] certificateBytes = data.getCertificateBytes();
            if (certificateBytes == null || certificateBytes.length == 0) {
                return getClient(data.getClientId(),
                        data.getClientSecret(),
                        data.getTenant(),
                        data.getSubscriptionId(),
                        env,
                        configurer);
            } else {
                return getClient(
                        data.getClientId(),
                        certificateBytes,
                        data.getCertificatePassword(),
                        data.getTenant(),
                        data.getSubscriptionId(),
                        env,
                        configurer);
            }
        } else if (data.getType() == TokenCredentialData.TYPE_MSI) {
            return getClient(data.getMsiPort(), env, configurer);
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
        return getClient(clientId, secret, tenantId, subId, env, null);
    }

    @Nonnull
    public static Azure getClient(final String clientId,
                                  final byte[] certificateBytes,
                                  final String cerficiatePassword,
                                  final String tenantId,
                                  final String subscriptionId,
                                  final AzureEnvironment env,
                                  final Configurer configurer) {
        ApplicationTokenCredentials token = new ApplicationTokenCredentials(
                clientId,
                tenantId,
                certificateBytes,
                cerficiatePassword,
                env);
        return azure(configurer)
                .authenticate(token)
                .withSubscription(subscriptionId);
    }

    @Nonnull
    public static Azure getClient(final String clientId,
                                  final String secret,
                                  final String tenantId,
                                  final String subId,
                                  final AzureEnvironment env,
                                  final Configurer configurer) {

        ApplicationTokenCredentials token = new ApplicationTokenCredentials(
                clientId,
                tenantId,
                secret,
                env);
        return azure(configurer)
                .authenticate(token)
                .withSubscription(subId);

    }

    @Nonnull
    public static Azure getClient(final int msiPort, final AzureEnvironment env) {
        return getClient(msiPort, env, null);
    }

    @Nonnull
    public static Azure getClient(final int msiPort, final AzureEnvironment env, final Configurer configurer) {
        MsiTokenCredentials msiToken = new RemoteMsiTokenCredentials(msiPort, env);
        try {
            return azure(configurer)
                    .authenticate(msiToken)
                    .withDefaultSubscription();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Azure.Configurable azure(Configurer configurer) {
        Azure.Configurable azure = Azure.configure();

        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            ProxyConfiguration proxyConfig = jenkins.proxy;
            if (proxyConfig != null) {
                Proxy proxy = proxyConfig.createProxy();
                azure = azure.withProxy(proxy);

                // TODO: Proxy auth
                // Unfortunately the auth doesn't actually work due to a bug in Azure SDK:
                // https://github.com/Azure/azure-sdk-for-java/issues/2030
                // Let's keep an eye on their state.
                final String userName = proxyConfig.getUserName();
                final String password = proxyConfig.getPassword();
                if (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password)) {
                    azure = azure.withProxyAuthenticator(new Authenticator() {
                        @Override
                        public Request authenticate(Route route, Response response) throws IOException {
                            String credential = Credentials.basic(userName, password);
                            return response.request().newBuilder().header("Proxy-Authorization", credential)
                                    .build();
                        }
                    });
                }
            }
        }

        if (configurer != null) {
            azure = configurer.configure(azure);
        }

        return azure;
    }

    private AzureClientFactory() {
    }

    public interface Configurer {
        Azure.Configurable configure(Azure.Configurable configurable);
    }
}
