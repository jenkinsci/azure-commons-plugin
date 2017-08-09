/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.command;

import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.Messages;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.Step;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mixed abstract base class with both command data and command service data.
 */
public abstract class BaseCommandContext extends Step implements ICommandServiceData, IBaseCommandData {
    private transient JobContext jobContext;
    private transient CommandState commandState = CommandState.Unknown;
    private transient CommandState lastCommandState = CommandState.Unknown;
    private transient CommandService commandService;

    protected void configure(
            Run<?, ?> aRun,
            FilePath aWorkspace,
            Launcher aLauncher,
            TaskListener aTaskListener,
            CommandService aCommandService) {
        configure(new JobContext(aRun, aWorkspace, aLauncher, aTaskListener), aCommandService);
    }

    protected void configure(JobContext jobCtx, CommandService aCommandService) {
        this.jobContext = jobCtx;
        this.commandService = aCommandService;
    }

    public void executeCommands() {
        checkNotNull(commandService, "configure should be called prior to execution");
        commandService.executeCommands(this);
    }

    @Override
    public abstract IBaseCommandData getDataForCommand(ICommand command);

    @Override
    public void setCommandState(CommandState commandState) {
        this.commandState = commandState;
    }

    @Override
    public CommandState getCommandState() {
        return this.commandState;
    }

    @Override
    public CommandState getLastCommandState() {
        return lastCommandState;
    }

    @Override
    public void setLastCommandState(CommandState lastCommandState) {
        this.lastCommandState = lastCommandState;
    }

    @Override
    public CommandService getCommandService() {
        return commandService;
    }

    @Override
    public final JobContext getJobContext() {
        return jobContext;
    }

    @Override
    public void logStatus(String status) {
        getJobContext().logger().println(status);
    }

    @Override
    public void logError(Exception ex) {
        this.logError(Messages.errorPrefix(), ex);
    }

    @Override
    public void logError(String prefix, Exception ex) {
        ex.printStackTrace(getJobContext().getTaskListener().error(prefix + ex.getMessage()));
        setCommandState(CommandState.HasError);
    }

    @Override
    public void logError(String message) {
        getJobContext().getTaskListener().error(message);
        setCommandState(CommandState.HasError);
    }
}
