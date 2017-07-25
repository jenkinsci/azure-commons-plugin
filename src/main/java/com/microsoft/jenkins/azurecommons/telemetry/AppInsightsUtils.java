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

public class AppInsightsUtils {
    /**
     * For security reason, hash the sensitive information before sent to AI.
     *
     * @param original the original string before hashing.
     * @return the hashed string.
     */
    public static String hash(final String original) {
        if (StringUtils.isBlank(original))
            return original;

        String ret;
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = original.getBytes("UTF-8");
            digest.update(bytes);
            byte[] bytesAfterDigest = digest.digest();
            final StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < bytesAfterDigest.length; i++) {
                buffer.append(Integer.toString((bytesAfterDigest[i] & 0xff) + 0x100, 16).substring(1));
            }
            ret = buffer.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        } catch (UnsupportedEncodingException ex) {
            return null;
        }

        return ret;
    }
}
