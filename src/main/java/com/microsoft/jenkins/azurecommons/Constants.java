/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons;

import java.nio.charset.Charset;

/**
 * Common constants.
 */
public final class Constants {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static final int READ_BUFFER_SIZE = 4096;

    private Constants() {
        // hide constructor
    }
}
