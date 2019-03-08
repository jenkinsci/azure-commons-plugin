package com.microsoft.jenkins.azurecommons.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Map;

public class JenkinsTelemetryClient {
    private static final String INSTRUMENT_KEY_ASSERT_MESSAGE = "Instrument key should have text.";
    private static final String TELEMETRY_TARGET_URL = "https://dc.services.visualstudio.com/v2/track";
    private static final int REQUEST_TIMEOUT = 5000;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final CloseableHttpClient CLIENT = HttpClients.custom()
            .setRetryHandler(new DefaultHttpRequestRetryHandler())
            .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(REQUEST_TIMEOUT).build())
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .build();

    private String instrumentKey;

    public JenkinsTelemetryClient(String instrumentKey) {
        Assert.hasText(instrumentKey, INSTRUMENT_KEY_ASSERT_MESSAGE);
        this.instrumentKey = instrumentKey;
    }

    public void setInstrumentKey(String instrumentKey) {
        Assert.hasText(instrumentKey, INSTRUMENT_KEY_ASSERT_MESSAGE);
        this.instrumentKey = instrumentKey;
    }

    public void send(String eventName, Map<String, String> properties) throws IOException {
        Assert.hasText(eventName, "Event name should have text.");

        HttpPost post = new HttpPost(TELEMETRY_TARGET_URL);
        try {
            TelemetryEventData telemetryEventData = new TelemetryEventData(eventName, properties, instrumentKey);
            String content = MAPPER.writeValueAsString(telemetryEventData);
            HttpEntity entity = new StringEntity(content, ContentType.APPLICATION_JSON);
            post.setEntity(entity);
            CloseableHttpResponse execute = CLIENT.execute(post);
            int statusCode = execute.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new IOException(String.format("Failed to send telemetry event %s, status code is %d, details: %s",
                        eventName, statusCode, execute.getStatusLine().getReasonPhrase()));
            }
        } finally {
            post.releaseConnection();
        }
    }

}
