/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.remote;

import hudson.util.Secret;

/**
 * SSH authentication credentials with username and password.
 */
public class UsernamePasswordAuth extends UsernameAuth {
    private final Secret password;

    public UsernamePasswordAuth(String username, String password) {
        super(username);
        this.password = Secret.fromString(password);
    }

    public UsernamePasswordAuth(String username,Secret password){
        super(username);
        this.password = password;
    }

    public Secret getPassword() {
        return password;
    }
}
