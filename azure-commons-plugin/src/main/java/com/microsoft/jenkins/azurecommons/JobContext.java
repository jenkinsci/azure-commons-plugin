/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Encapsulates the context for a Jenkins build job.
 *
 * @deprecated see {@link com.microsoft.jenkins.azurecommons.core.JobContext}.
 */
@Deprecated
public class JobContext {
    private final Run<?, ?> run;
    private final FilePath workspace;
    private final Launcher launcher;
    private final TaskListener taskListener;

    public JobContext(Run<?, ?> run,
                      FilePath workspace,
                      Launcher launcher,
                      TaskListener taskListener) {
        this.run = run;
        this.workspace = workspace;
        this.launcher = launcher;
        this.taskListener = taskListener;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public TaskListener getTaskListener() {
        return taskListener;
    }

    /**
     * Gets the owner project of this run.
     *
     * @return the owner project
     */
    public Item getOwner() {
        Run<?, ?> currentRun = getRun();
        if (currentRun != null) {
            return currentRun.getParent();
        }
        return null;
    }

    public EnvVars envVars() {
        try {
            return getRun().getEnvironment(getTaskListener());
        } catch (IOException e) {
            throw new RuntimeException("Failed to get Job environment variables", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get Job environment variables", e);
        }
    }

    public PrintStream logger() {
        return getTaskListener().getLogger();
    }

    public FilePath remoteWorkspace() {
        return new FilePath(launcher.getChannel(), workspace.getRemote());
    }

    public ByteArrayInputStream replaceMacro(InputStream original, boolean enabled) throws IOException {
        try {
            String content = IOUtils.toString(original, Constants.UTF8);
            if (enabled) {
                content = Util.replaceMacro(content, envVars());
            }
            if (content != null) {
                return new ByteArrayInputStream(content.getBytes(Constants.UTF8));
            } else {
                throw new IllegalArgumentException("null content returned");
            }
        } finally {
            original.close();
        }
    }
}
