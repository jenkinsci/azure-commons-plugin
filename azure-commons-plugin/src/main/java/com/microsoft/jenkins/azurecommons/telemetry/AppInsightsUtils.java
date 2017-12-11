/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.azurecommons.telemetry;

import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class AppInsightsUtils {
    /**
     * For security reason, hash the sensitive information before sent to AI.
     *
     * @param original the original string before hashing.
     * @return the hashed string.
     */
    public static String hash(String original) {
        if (StringUtils.isBlank(original)) {
            return original;
        }

        final int byteMask = 0xFF;
        final int bytePadding = 0x100;
        final int radix = 16;
        String ret;
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = original.getBytes("UTF-8");
            digest.update(bytes);
            byte[] bytesAfterDigest = digest.digest();
            final StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < bytesAfterDigest.length; i++) {
                buffer.append(Integer.toString((bytesAfterDigest[i] & byteMask) + bytePadding, radix).substring(1));
            }
            ret = buffer.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            return null;
        }

        return ret;
    }

    private AppInsightsUtils() {
        // hide constructor.
    }
}
