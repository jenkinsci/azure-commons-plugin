/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.remote;

import com.google.common.collect.ImmutableList;
import com.microsoft.jenkins.azurecommons.Constants;
import hudson.util.Secret;

import java.util.Arrays;

/**
 * SSH authentication credentials with username and private keys.
 */
public class UsernamePrivateKeyAuth extends UsernameAuth {
    private final Secret passPhrase;
    private final ImmutableList<String> privateKeys;

    public UsernamePrivateKeyAuth(final String username, final Secret passPhrase, String... privateKeys) {
        this(username, passPhrase, Arrays.asList(privateKeys));
    }

    public UsernamePrivateKeyAuth(final String username, final Secret passPhrase, Iterable privateKeys) {
        super(username);
        this.passPhrase = passPhrase;
        //noinspection unchecked
        this.privateKeys = ImmutableList.<String>copyOf(privateKeys);
    }

    public byte[] getPassPhraseBytes() {
        if (passPhrase == null) {
            return null;
        }
        return passPhrase.getPlainText().getBytes(Constants.DEFAULT_CHARSET);
    }

    public ImmutableList<String> getPrivateKeys() {
        return privateKeys;
    }
}
