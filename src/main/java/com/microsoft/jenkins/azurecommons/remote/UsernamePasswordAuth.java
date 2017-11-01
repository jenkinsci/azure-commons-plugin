/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.remote;

/**
 * SSH authentication credentials with username and password.
 */
public class UsernamePasswordAuth extends UsernameAuth {
    private final String password;

    public UsernamePasswordAuth(String username, String password) {
        super(username);
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
