/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.core.credentials;

import com.microsoft.azure.AzureEnvironment;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.HashMap;

@Deprecated
public class MsiTokenCredentials extends AbstractTokenCredentials {
    private int msiPort;

    /**
     * Initializes a new instance of the AzureTokenCredentials.
     *
     * @param msiPort     the MSI port to use
     * @param environment the Azure environment to use
     */
    public MsiTokenCredentials(final int msiPort, AzureEnvironment environment) {
        super(environment, null);
        setTokens(new HashMap<String, Token>());
        this.msiPort = msiPort;
    }

    int getMsiPort() {
        return msiPort;
    }

    @Override
    protected Token acquireAccessToken(final String resource) throws IOException {
        return parseToken(requestLocalMsiEndpoint(resource, msiPort));
    }

    protected static String requestLocalMsiEndpoint(final String resource, final int msiPort) throws IOException {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        RequestBody body = new FormBody.Builder()
                .add("resource", resource)
                .build();
        Request request = new Request.Builder()
                .addHeader("Metadata", "true")
                .url("http://localhost:" + msiPort + "/oauth2/token")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RuntimeException("http response: " + response.code() + " " + response.message());
        } else {
            return response.body().string();
        }
    }

}
