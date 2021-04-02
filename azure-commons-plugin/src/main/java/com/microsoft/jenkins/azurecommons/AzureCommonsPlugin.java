/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons;

import hudson.Plugin;

import java.util.Map;

public class AzureCommonsPlugin extends Plugin {

    /**
     * No-OP, first step of retiring analytics.
     */
    public static void sendEvent(String item, String action, String... properties) {
    }

    /**
     * No-OP, first step of retiring analytics.
     */
    public static void sendEvent(String item, String action, Map<String, String> properties, boolean force) {
    }
}
