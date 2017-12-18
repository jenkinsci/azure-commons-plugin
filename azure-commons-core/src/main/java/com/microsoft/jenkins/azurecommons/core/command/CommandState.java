/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.core.command;

public enum CommandState {
    Unknown,
    Done,
    HasError,
    Success;

    public boolean isFinished() {
        return this == HasError || this == Done;
    }

    public boolean isError() {
        return this == HasError;
    }
}
