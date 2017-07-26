/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

import okhttp3.HttpUrl;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class AzureHttpRecorder {
    private static final Logger LOGGER = Logger.getLogger(AzureHttpRecorder.class.getName());

    static final String API_VERSION = "api-version";
    static final String SUBSCRIPTIONS = "subscriptions";
    static final String MS_REQUEST_ID = "x-ms-request-id";
    static final String PROVIDERS = "providers";
    static final String RESOURCE_PATH = "resource-path";
    static final String RESPONSE_CODE = "httpCode";
    static final String RESPONSE_MESSAGE = "httpMessage";

    final AppInsightsClient appInsightsClient;

    public AzureHttpRecorder(final AppInsightsClient client) {
        this.appInsightsClient = client;
    }

    public void record(final HttpRecordable recordable) throws IOException {
        if (appInsightsClient != null) {
            try {
                sendTelemetry(recordable);
            } catch (Exception e) {
                LOGGER.warning("Fails in recording http metrics:" + e.getMessage());
            }
        }
    }

    private void sendTelemetry(final HttpRecordable recordable) {
        final Map<String, String> properties = new HashMap<>();
        final HttpUrl httpUrl = HttpUrl.get(recordable.getRequestUri());
        final String objectName = parseProvider(httpUrl, properties);
        if (objectName == null) {
            // Might not be a standard API path. Ignore
            return;
        }
        properties.put(API_VERSION, httpUrl.queryParameter(API_VERSION));
        properties.put(RESPONSE_CODE, String.valueOf(recordable.getHttpCode()));
        properties.put(RESPONSE_MESSAGE, recordable.getHttpMessage());
        properties.put(MS_REQUEST_ID, recordable.getRequestId());
        properties.putAll(recordable.getExtraProperties());
        parseSubscriptionId(httpUrl, properties);
        appInsightsClient.sendEvent(AppInsightsConstants.AZURE_REST, recordable.getHttpMethod(), properties, false);
    }

    private String parseProvider(final HttpUrl httpUrl, final Map<String, String> properties) {
        if (httpUrl.pathSegments().contains(PROVIDERS)) {
            int index = httpUrl.pathSegments().indexOf(PROVIDERS);
            if (index + 1 < httpUrl.pathSegments().size()) {
                String path = httpUrl.encodedPath();
                properties.put(RESOURCE_PATH, path.substring(path.indexOf(PROVIDERS)));
                return httpUrl.pathSegments().get(index + 1);
            }
        }
        return null;
    }

    private void parseSubscriptionId(final HttpUrl httpUrl, final Map<String, String> properties) {
        if (httpUrl.pathSegments().contains(SUBSCRIPTIONS)) {
            int index = httpUrl.pathSegments().indexOf(SUBSCRIPTIONS);
            if (index + 1 < httpUrl.pathSegments().size())
                properties.put(AppInsightsConstants.AZURE_SUBSCRIPTION_ID, httpUrl.pathSegments().get(index + 1));
        }
    }

    public static class HttpRecordable {
        // request
        private URI requestUri;
        private String httpMethod;

        // response
        private int httpCode;
        private String httpMessage;
        private String requestId; // 'x-ms-request-id' from Azure

        // any other properties
        private Map<String, String> extraProperties = new HashMap<>();

        public URI getRequestUri() {
            return requestUri;
        }

        public HttpRecordable withRequestUri(final URI requestUri) {
            checkNotNull(requestUri);
            this.requestUri = requestUri;
            return this;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public HttpRecordable withHttpMethod(final String httpMethod) {
            checkNotNull(httpMethod);
            this.httpMethod = httpMethod;
            return this;
        }

        public int getHttpCode() {
            return httpCode;
        }

        public HttpRecordable withHttpCode(final int httpCode) {
            this.httpCode = httpCode;
            return this;
        }

        public String getHttpMessage() {
            return httpMessage;
        }

        public HttpRecordable withHttpMessage(final String httpMessage) {
            this.httpMessage = httpMessage;
            return this;
        }

        public String getRequestId() {
            return requestId;
        }

        public HttpRecordable withRequestId(final String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Map<String, String> getExtraProperties() {
            return extraProperties;
        }

        public HttpRecordable withExtraProperty(final String name, final String value) {
            this.extraProperties.put(name, value);
            return this;
        }
    }

}
