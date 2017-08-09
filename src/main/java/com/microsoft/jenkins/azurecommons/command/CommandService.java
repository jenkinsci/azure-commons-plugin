/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.command;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * State machine for command execution.
 * <p>
 * NB. Not fully type safe on generic types.
 */
@SuppressWarnings("unchecked")
public final class CommandService {
    private final Map<Class<? extends ICommand>, Class<? extends ICommand>> transitionMap;
    private final Map<Class<? extends ICommand>, ICommand> instanceMap;
    private final Class startCommand;
    private final Class cleanUpCommand;

    public static Builder builder() {
        return new Builder();
    }

    private CommandService(Map<Class<? extends ICommand>, Class<? extends ICommand>> transitionMap,
                           Map<Class<? extends ICommand>, ICommand> instanceMap,
                           Class startCommand,
                           Class cleanUpCommand) {
        this.transitionMap = transitionMap;
        this.instanceMap = instanceMap;
        this.startCommand = startCommand;
        this.cleanUpCommand = cleanUpCommand;
    }

    public ICommand<IBaseCommandData> getCleanUpCommand() {
        return instanceMap.get(cleanUpCommand);
    }

    public Class getStartCommandClass() {
        return startCommand;
    }

    public ImmutableMap<Class, Class> getTransitions() {
        ImmutableMap.Builder<Class, Class> builder = ImmutableMap.builder();
        for (Map.Entry<Class<? extends ICommand>, Class<? extends ICommand>> entry : transitionMap.entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    public ImmutableSet<Class> getRegisteredCommands() {
        ImmutableSet.Builder<Class> builder = ImmutableSet.builder();
        for (Class<? extends ICommand> clazz : instanceMap.keySet()) {
            builder.add(clazz);
        }
        return builder.build();
    }

    /**
     * Executes the commands described by the given {@code commandServiceData}, and runs the clean up command if
     * present.
     *
     * @param commandServiceData the service data that describes the commands state transitions.
     */
    public void executeCommands(ICommandServiceData commandServiceData) {
        try {
            execute(commandServiceData);
        } finally {
            if (cleanUpCommand != null) {
                runCommand(cleanUpCommand, commandServiceData);
            }
        }
    }

    private CommandState runCommand(Class<? extends ICommand> clazz, ICommandServiceData commandServiceData) {
        checkNotNull(clazz);
        ICommand<IBaseCommandData> command = ensureCreation(clazz, instanceMap);
        checkNotNull(command);
        IBaseCommandData commandData = commandServiceData.getDataForCommand(command);
        command.execute(commandData);
        return commandData.getCommandState();
    }

    private void execute(ICommandServiceData commandServiceData) {
        //successfully started
        Class current = startCommand;
        while (current != null) {
            final CommandState state = runCommand(current, commandServiceData);
            commandServiceData.setLastCommandState(state);

            switch (state) {
                case Success:
                    if (INextCommandAware.class.isAssignableFrom(current)) {
                        // If the command itself is INextCommandAware, use it to determine the next command;
                        // otherwise check the transition from the service data.
                        // This is useful if we need to implement a command with if / switch semantics.
                        current = ((INextCommandAware) instanceMap.get(current)).nextCommand();
                    } else {
                        current = transitionMap.get(current);
                    }
                    break;
                case HasError:
                case Done:
                    // we're done
                    return;
                default:
                    // the command didn't set a meaningful state
                    break;
            }
        }
    }

    private static ICommand ensureCreation(Class<? extends ICommand> clazz,
                                           Map<Class<? extends ICommand>, ICommand> cache) {
        try {
            ICommand command = cache.get(clazz);
            if (command == null) {
                command = clazz.newInstance();
                cache.put(clazz, command);
            }
            return command;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static final class Builder {
        private Map<Class<? extends ICommand>, Class<? extends ICommand>> transitionMap;
        private Map<Class<? extends ICommand>, ICommand> instanceMap;
        private Class<? extends ICommand> cleanUpCommand;
        private Class<? extends ICommand> startCmd;

        private Builder() {
            transitionMap = new HashMap<>();
            instanceMap = new HashMap<>();
        }

        /**
         * Register a transition from current command to the next command if current command is executed without error.
         *
         * @param current the current command
         * @param next    the next command to be executed if current finishes successfully
         * @return the builder itself
         */
        public Builder withTransition(Class<? extends ICommand<? extends IBaseCommandData>> current,
                                      Class<? extends ICommand<? extends IBaseCommandData>> next) {
            transitionMap.put(current, next);
            ensureCreation(current, instanceMap);
            ensureCreation(next, instanceMap);
            return this;
        }

        /**
         * Register the start command.
         *
         * @param command the start command.
         * @return the builder itself
         */
        public Builder withStartCommand(Class<? extends ICommand<? extends IBaseCommandData>> command) {
            checkNotNull(command);
            this.startCmd = command;
            ensureCreation(command, instanceMap);
            return this;
        }

        /**
         * Register an optional clean up command that will be executed when the all the normal commands finish
         * execution, regardless of the termination state of the last command.
         *
         * @param command the clean up command
         * @return the builder itself.
         */
        public Builder withCleanUpCommand(Class<? extends ICommand<? extends IBaseCommandData>> command) {
            checkNotNull(command);
            this.cleanUpCommand = command;
            ensureCreation(command, instanceMap);
            return this;
        }

        /**
         * Register a single command that may be used by other transitions.
         * <p>
         * This ensures the command is instantiated early in the execution flow. It may be used for the followed
         * commands of an {@link INextCommandAware} command, where the transition is defined by the command.
         *
         * @param command the command to be registered.
         * @return the builder itself.
         */
        public Builder withSingleCommand(Class<? extends ICommand<? extends IBaseCommandData>> command) {
            ensureCreation(command, instanceMap);
            return this;
        }

        public CommandService build() {
            checkState(!instanceMap.isEmpty());
            checkNotNull(startCmd);
            return new CommandService(transitionMap, instanceMap, startCmd, cleanUpCommand);
        }
    }
}
