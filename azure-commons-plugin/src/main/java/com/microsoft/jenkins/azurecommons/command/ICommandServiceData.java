/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.command;

public interface ICommandServiceData {
    CommandService getCommandService();

    IBaseCommandData getDataForCommand(ICommand command);

    /**
     * Set the command state for the last command being executed in the command service.
     *
     * @param state the last command state.
     * @see IBaseCommandData#setCommandState(CommandState)
     */
    void setLastCommandState(CommandState state);

    /**
     * Get the command state for the last command being executed. This will be the state of the command service
     * execution.
     *
     * @see IBaseCommandData#getCommandState()
     */
    CommandState getLastCommandState();
}
