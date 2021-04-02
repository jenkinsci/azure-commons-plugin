package com.microsoft.jenkins.azurecommons.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.util.Map;

public class JenkinsTelemetryClient {
    private static final int REQUEST_TIMEOUT = 5000;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final CloseableHttpClient CLIENT = HttpClients.custom()
            .setRetryHandler(new DefaultHttpRequestRetryHandler())
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                    .setConnectTimeout(CONNECT_TIMEOUT).build())
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .build();

    public JenkinsTelemetryClient(String instrumentKey) {
    }

    public void setInstrumentKey(String instrumentKey) {
    }

    /**
     * No-OP, first step of retiring analytics.
     */
    public void send(String eventName, Map<String, String> properties) throws IOException {
    }

}
