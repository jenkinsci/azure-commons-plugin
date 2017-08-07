/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.command;

import com.google.common.collect.ImmutableMap;
import com.microsoft.jenkins.azurecommons.JobContext;
import com.microsoft.jenkins.azurecommons.Messages;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.Step;

import java.util.Hashtable;
import java.util.Map;

public abstract class BaseCommandContext extends Step implements ICommandServiceData {
    private transient JobContext jobContext;
    private transient CommandState commandState = CommandState.Unknown;
    private transient Map<Class, TransitionInfo> commands;
    private transient Class startCommandClass;

    protected void configure(
            Run<?, ?> aRun,
            FilePath aWorkspace,
            Launcher aLauncher,
            TaskListener aTaskListener,
            Map<Class, TransitionInfo> cmds,
            Class startCmd) {
        this.jobContext = new JobContext(aRun, aWorkspace, aLauncher, aTaskListener);
        this.commands = ImmutableMap.copyOf(cmds);
        this.startCommandClass = startCmd;
    }

    protected void configure(JobContext jobCtx, Hashtable<Class, TransitionInfo> cmds, Class startCmdClass) {
        this.jobContext = jobCtx;
        this.commands = cmds;
        this.startCommandClass = startCmdClass;
    }

    @Override
    public Map<Class, TransitionInfo> getCommands() {
        return commands;
    }

    @Override
    public Class getStartCommandClass() {
        return startCommandClass;
    }

    @Override
    public abstract IBaseCommandData getDataForCommand(ICommand command);

    public void setCommandState(CommandState commandState) {
        this.commandState = commandState;
    }

    public CommandState getCommandState() {
        return this.commandState;
    }

    public boolean hasError() {
        return this.commandState.equals(CommandState.HasError);
    }

    public boolean isFinished() {
        return this.commandState.equals(CommandState.HasError)
                || this.commandState.equals(CommandState.Done);
    }

    public final JobContext jobContext() {
        return jobContext;
    }

    public void logStatus(String status) {
        jobContext().getTaskListener().getLogger().println(status);
    }

    public void logError(Exception ex) {
        this.logError(Messages.errorPrefix(), ex);
    }

    public void logError(String prefix, Exception ex) {
        ex.printStackTrace(jobContext().getTaskListener().error(prefix + ex.getMessage()));
        setCommandState(CommandState.HasError);
    }

    public void logError(String message) {
        jobContext().getTaskListener().error(message);
        setCommandState(CommandState.HasError);
    }
}
