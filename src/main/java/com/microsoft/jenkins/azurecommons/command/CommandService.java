/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.command;

import java.util.Map;

public class CommandService {
    private final ICommand<IBaseCommandData> cleanUpCommand;

    public CommandService() {
        cleanUpCommand = null;
    }

    @SuppressWarnings("unchecked")
    public CommandService(ICommand cleanUpCommand) {
        this.cleanUpCommand = cleanUpCommand;
    }

    /**
     * Executes the commands described by the given {@code commandServiceData}, and runs the clean up command if
     * present.
     * <p>
     * If the clean up is not required, try the static method {@link #executeCommands(ICommandServiceData)}.
     *
     * @param commandServiceData the service data that describes the commands state transitions.
     * @return whether the commands are executed gracefully
     */
    public boolean safeExecuteCommands(ICommandServiceData commandServiceData) {
        try {
            return executeCommands(commandServiceData);
        } finally {
            if (cleanUpCommand != null) {
                IBaseCommandData commandData = commandServiceData.getDataForCommand(cleanUpCommand);
                cleanUpCommand.execute(commandData);
            }
        }
    }

    public ICommand<IBaseCommandData> getCleanUpCommand() {
        return cleanUpCommand;
    }

    public static boolean executeCommands(ICommandServiceData commandServiceData) {
        Class startCommand = commandServiceData.getStartCommandClass();
        Map<Class, TransitionInfo> commands = commandServiceData.getCommands();
        if (!commands.isEmpty() && startCommand != null) {
            //successfully started
            TransitionInfo current = commands.get(startCommand);
            while (current != null) {
                ICommand<IBaseCommandData> command = current.getCommand();
                IBaseCommandData commandData = commandServiceData.getDataForCommand(command);
                command.execute(commandData);

                INextCommandAware previous;
                // If the command itself is INextCommandAware, use it to determine the next command;
                // otherwise check the transition from the service data.
                // This is useful if we need to implement a command with if / switch semantics.
                if (command instanceof INextCommandAware) {
                    previous = (INextCommandAware) command;
                } else {
                    previous = current;
                }

                current = null;

                final CommandState state = commandData.getCommandState();
                if (state == CommandState.Success && previous.getSuccess() != null) {
                    current = commands.get(previous.getSuccess());
                } else if (state == CommandState.UnSuccessful && previous.getFail() != null) {
                    current = commands.get(previous.getFail());
                } else if (state == CommandState.HasError) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }
}
