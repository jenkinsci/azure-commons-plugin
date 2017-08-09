/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.command;

import com.google.common.collect.ImmutableSet;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import javax.annotation.Nonnull;

/**
 * Provides the basic implementation of {@link org.jenkinsci.plugins.workflow.steps.StepExecution} which can be used
 * to implement the {@link org.jenkinsci.plugins.workflow.steps.Step#start(StepContext)} method.
 * <p>
 * A general example would be
 * <pre><code>
 * \@Override
 * public StepExecution start(StepContext context) throws Exception {
 *     return new SimpleBuildStepExecution(new YourSimpleBuildStep(this), context);
 * }
 * </code></pre>
 * <p>
 * where {@code YourSimpleBuildStep} is the describable instance that uses the context that extends from
 * {@link org.jenkinsci.plugins.workflow.steps.Step}. For the context descriptor, it should extends from
 * {@link org.jenkinsci.plugins.workflow.steps.StepDescriptor} and its {@code getRequiredContext()} should return
 * at least {@link SimpleBuildStepExecution#REQUIRED_CONTEXT}.
 */
public class SimpleBuildStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    public static final ImmutableSet<? extends Class<?>> REQUIRED_CONTEXT =
            ImmutableSet.of(Run.class, FilePath.class, Launcher.class, TaskListener.class);

    private static final long serialVersionUID = 1L;

    private final transient SimpleBuildStep delegate;

    public SimpleBuildStepExecution(SimpleBuildStep delegate, StepContext context) {
        super(context);
        this.delegate = delegate;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected Void run() throws Exception {
        StepContext context = getContext();
        FilePath workspace = context.get(FilePath.class);
        workspace.mkdirs();
        delegate.perform(
                context.get(Run.class),
                workspace,
                context.get(Launcher.class),
                context.get(TaskListener.class));
        return null;
    }

    @Nonnull
    @Override
    public String getStatus() {
        String base = super.getStatus();
        if (delegate != null) {
            return delegate.getClass().getName() + ": " + base;
        } else {
            return base;
        }
    }
}
