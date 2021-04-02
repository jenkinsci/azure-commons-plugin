/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.core.credentials;

import com.microsoft.azure.AzureEnvironment;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import jenkins.agents.AgentComputerUtil;
import org.jenkinsci.remoting.RoleChecker;

import java.io.IOException;

/**
 * Enable retrieving the IMDS token from Jenkins agents.
 */
public class RemoteImdsTokenCredentials extends ImdsTokenCredentials {
    /**
     * Initializes a new instance of the AzureTokenCredentials.
     *
     * @param environment the Azure environment to use
     */
    public RemoteImdsTokenCredentials(AzureEnvironment environment) {
        super(environment);
    }

    @Override
    protected Token acquireAccessToken(final String resource) throws IOException {
        VirtualChannel channel = AgentComputerUtil.getChannelToMaster();
        if (channel == null) {
            throw new RuntimeException("Failed to get the channel to master. Please check the running environment.");
        }
        String responseBody;
        try {
            responseBody = channel.call(new RequestImdsTokenTask(resource));
        } catch (InterruptedException e) {
            throw new RuntimeException("Execution on the master is Interrupted");
        }
        return parseToken(responseBody);
    }

    public static class RequestImdsTokenTask implements Callable<String, IOException> {
        private final String resource;

        public RequestImdsTokenTask(String resource) {
            this.resource = resource;
        }

        @Override
        public String call() throws IOException {
            return requestIMDSEndpoint(resource);
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {
            // Do nothing
        }
    }
}
