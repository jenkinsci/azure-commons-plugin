/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.remote;

/**
 * SSH authentication credentials with username and password.
 */
@Deprecated
class UsernamePasswordAuth extends UsernameAuth {
    private final String password;

    UsernamePasswordAuth(String username, String password) {
        super(username);
        this.password = password;
    }

    String getPassword() {
        return password;
    }
}
