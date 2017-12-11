/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.core.command;

import com.microsoft.jenkins.azurecommons.core.JobContext;
import hudson.EnvVars;

public interface IBaseCommandData {
    void logError(String message);

    void logStatus(String status);

    void logError(Exception ex);

    void logError(String prefix, Exception ex);

    /**
     * Set the command state for the associated running command.
     *
     * @param state the result command state.
     */
    void setCommandState(CommandState state);

    /**
     * Get the command state for the associated completed command.
     *
     * @return the result command state.
     */
    CommandState getCommandState();

    JobContext getJobContext();

    /**
     * To get the environment variable bindings, this method should be called in prefer to
     * {@code getJobContext().envVars()}. In the pipeline environment, the environment variables returned from
     * {@code getJobContext().envVars()} is not complete. The default implementation of this method in
     * {@link BaseCommandContext} handles this.
     */
    EnvVars getEnvVars();
}
