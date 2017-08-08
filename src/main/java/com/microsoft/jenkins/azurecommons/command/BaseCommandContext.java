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

public abstract class BaseCommandContext extends Step implements ICommandServiceData {
    private transient JobContext jobContext;
    private transient CommandState commandState = CommandState.Unknown;
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

    public void setCommandState(CommandState commandState) {
        this.commandState = commandState;
    }

    public CommandState getCommandState() {
        return this.commandState;
    }

    @Override
    public CommandService getCommandService() {
        return commandService;
    }

    public boolean hasError() {
        return this.commandState.equals(CommandState.HasError);
    }

    public boolean isFinished() {
        return this.commandState.equals(CommandState.HasError)
                || this.commandState.equals(CommandState.Done);
    }

    public final JobContext getJobContext() {
        return jobContext;
    }

    public void logStatus(String status) {
        getJobContext().logger().println(status);
    }

    public void logError(Exception ex) {
        this.logError(Messages.errorPrefix(), ex);
    }

    public void logError(String prefix, Exception ex) {
        ex.printStackTrace(getJobContext().getTaskListener().error(prefix + ex.getMessage()));
        setCommandState(CommandState.HasError);
    }

    public void logError(String message) {
        getJobContext().getTaskListener().error(message);
        setCommandState(CommandState.HasError);
    }
}
