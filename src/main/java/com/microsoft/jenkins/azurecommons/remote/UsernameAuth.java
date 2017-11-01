/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.remote;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

/**
 * Abstract SSH authentication credentials with username.
 */
public abstract class UsernameAuth {
    private final String username;

    public UsernameAuth(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public static UsernameAuth fromCredentials(StandardUsernameCredentials credentials) {
        if (credentials instanceof StandardUsernamePasswordCredentials) {
            StandardUsernamePasswordCredentials userPass = (StandardUsernamePasswordCredentials) credentials;
            return new UsernamePasswordAuth(userPass.getUsername(), userPass.getPassword());
        } else if (credentials instanceof SSHUserPrivateKey) {
            SSHUserPrivateKey userKey = (SSHUserPrivateKey) credentials;
            return new UsernamePrivateKeyAuth(userKey.getUsername(), userKey.getPassphrase(), userKey.getPrivateKeys());
        } else {
            throw new IllegalArgumentException("Unsupported credentials type " + credentials.getClass().getName());
        }
    }
}
