package com.microsoft.jenkins.azurecommons.core.credentials;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public class TokenCredentialData implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int TYPE_SP = 0;
    public static final int TYPE_MSI = 1;

    private int type;
    private String azureEnvironmentName;
    private String managementEndpoint;
    private String activeDirectoryEndpoint;
    private String resourceManagerEndpoint;
    private String graphEndpoint;
    private String subscriptionId;
    private String clientId;
    private String clientSecret;
    private String tenant;
    private int msiPort;

    public String getAzureEnvironmentName() {
        return azureEnvironmentName;
    }

    public void setAzureEnvironmentName(String azureEnvironmentName) {
        this.azureEnvironmentName = azureEnvironmentName;
    }

    public String getManagementEndpoint() {
        return managementEndpoint;
    }

    public void setManagementEndpoint(String managementEndpoint) {
        this.managementEndpoint = managementEndpoint;
    }

    public String getActiveDirectoryEndpoint() {
        return activeDirectoryEndpoint;
    }

    public void setActiveDirectoryEndpoint(String activeDirectoryEndpoint) {
        this.activeDirectoryEndpoint = activeDirectoryEndpoint;
    }

    public String getResourceManagerEndpoint() {
        return resourceManagerEndpoint;
    }

    public void setResourceManagerEndpoint(String resourceManagerEndpoint) {
        this.resourceManagerEndpoint = resourceManagerEndpoint;
    }

    public String getGraphEndpoint() {
        return graphEndpoint;
    }

    public void setGraphEndpoint(String graphEndpoint) {
        this.graphEndpoint = graphEndpoint;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public int getMsiPort() {
        return msiPort;
    }

    public void setMsiPort(int msiPort) {
        this.msiPort = msiPort;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public static byte[] serialize(TokenCredentialData obj) {
        return SerializationUtils.serialize(obj);
    }

    public static TokenCredentialData deserialize(byte[] data) {
        return SerializationUtils.deserialize(data);
    }
}
