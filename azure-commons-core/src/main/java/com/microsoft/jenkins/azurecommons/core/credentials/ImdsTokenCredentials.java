/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.core.credentials;

import com.microsoft.azure.AzureEnvironment;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ImdsTokenCredentials extends AbstractTokenCredentials {

    /**
     * Initializes a new instance of the AzureTokenCredentials.
     *
     * @param environment the Azure environment to use
     */
    public ImdsTokenCredentials(AzureEnvironment environment) {
        super(environment, null);
        setTokens(new HashMap<String, Token>());
    }

    protected Token acquireAccessToken(final String resource) throws IOException {
        return parseToken(requestIMDSEndpoint(resource));
    }

    protected static String requestIMDSEndpoint(final String resource) throws IOException {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        String parameters = "https://management.azure.com/";
        Request request = new Request.Builder()
                .addHeader("Metadata", "true")
                .url("http://169.254.169.254/metadata/identity/oauth2/token?"
                        + "api-version=2018-02-01&resource="
                        + URLEncoder.encode(parameters, StandardCharsets.UTF_8.toString()))
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RuntimeException("http response: " + response.code() + " " + response.message());
        } else {
            return response.body().string();
        }
    }
}
