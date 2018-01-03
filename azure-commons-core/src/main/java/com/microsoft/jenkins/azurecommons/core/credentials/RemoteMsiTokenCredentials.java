/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.microsoft.jenkins.azurecommons.core.credentials;

import com.microsoft.azure.AzureEnvironment;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.slaves.SlaveComputer;
import org.jenkinsci.remoting.RoleChecker;

import java.io.IOException;

/**
 * Enable retrieving the msi token from Jenkins agents.
 */
public class RemoteMsiTokenCredentials extends MsiTokenCredentials {
    /**
     * Initializes a new instance of the AzureTokenCredentials.
     *
     * @param msiPort
     * @param environment the Azure environment to use
     */
    public RemoteMsiTokenCredentials(int msiPort, AzureEnvironment environment) {
        super(msiPort, environment);
    }

    @Override
    protected Token acquireAccessToken(final String resource) throws IOException {
        VirtualChannel channel = SlaveComputer.getChannelToMaster();
        if (channel == null) {
            throw new RuntimeException("Failed to get the channel to master. Please check the running environment.");
        }
        String responseBody;
        try {
            responseBody = channel.call(new RequestMsiTokenTask(resource, getMsiPort()));
        } catch (InterruptedException e) {
            throw new RuntimeException("Execution on the master is Interrupted");
        }
        return parseToken(responseBody);
    }

    public static class RequestMsiTokenTask implements Callable<String, IOException> {
        private final String resource;
        private final int msiPort;

        public RequestMsiTokenTask(String resource, int msiPort) {
            this.resource = resource;
            this.msiPort = msiPort;
        }

        @Override
        public String call() throws IOException {
            return requestLocalMsiEndpoint(resource, msiPort);
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {
            // Do nothing
        }
    }
}
