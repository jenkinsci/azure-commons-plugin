/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.command;

import com.microsoft.jenkins.azurecommons.JobContext;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mixed abstract base class with both command data and command service data.
 */
public abstract class BaseCommandContext extends Step implements ICommandServiceData, IBaseCommandData {
    private transient JobContext jobContext;
    private transient CommandState commandState = CommandState.Unknown;
    private transient CommandState lastCommandState = CommandState.Unknown;
    private transient CommandService commandService;

    private transient EnvVars envVars;

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

    /**
     * Pipeline function call entrance.
     *
     * @param context the pipeline step context
     * @return the execution logic object.
     */
    @Override
    public final StepExecution start(StepContext context) throws Exception {
        envVars = context.get(EnvVars.class);
        return startImpl(context);
    }

    public abstract StepExecution startImpl(StepContext context) throws Exception;

    /**
     * When running from pipeline, the result returned from {@link Run#getEnvironment(TaskListener)} won't include
     * the system environments set by the {@code env.VAR=value} or {@code withEnv(['VAR=value'])}. We need to call
     * {@code StepContext.get(EnvVar.class)} for the full environment variables set.
     * <p>
     * If the field is not null, we are running from pipeline and we use this as the source of environment variables;
     * otherwise we fetch the environment variables from {@code Run} as normal.
     */
    @Override
    public EnvVars getEnvVars() {
        if (envVars != null) {
            return envVars;
        }
        return getJobContext().envVars();
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
        this.logError("ERROR: ", ex);
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
