/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class EnvironmentInjector {
    /**
     * Add an environment variable binding for the given Run.
     * <p>
     * The {@link hudson.model.EnvironmentContributingAction} is more preferred for the environment variable injection.
     * However, is not compatible with workflow at the time of implementation.
     * <p>
     * We register the {@link CustomEnvironmentContributor} which scans for private
     * {@code EnvironmentInjectionAction} bound to the Run instance, and updates the environment variables
     * accordingly. This will be called by {@link Run#getEnvironment(TaskListener)}.
     * <p>
     * In pipeline, the {@code EnvVars} should be fetched from {@code StepContext#get(EnvVars.class)}, and it will
     * not contain the changes to the environment variables bound to the {@code Run} object. In that case we should
     * modify the {@code EnvVars} returned from {@code StepContext#get(EnvVars.class)}. Passing it as the second
     * parameter of this method.
     * <p>
     * Note that {@link Run#getEnvironment(TaskListener)} returns a snapshot of the environment variables. If new
     * variables are injected, we need to call it again to get the latest values. Or if you have a snapshot of the
     * {@code EnvVars}, pass it as the second parameter so that it gets updated at the same time.
     *
     * @param run     the run object
     * @param envVars the current set of {@code EnvVars} which needs to be updated at the same time
     * @param name    the variable name
     * @param value   the variable value
     * @see CustomEnvironmentContributor
     * @see <a href="https://issues.jenkins-ci.org/browse/JENKINS-29537">JENKINS-29537</a>
     */
    public static void inject(Run<?, ?> run, EnvVars envVars, String name, Object value) {
        checkNotNull(value, "environment variable value is null");
        String strVal = value.toString();
        if (envVars != null) {
            envVars.put(name, strVal);
        }
        if (run != null) {
            EnvironmentInjectionAction action = run.getAction(EnvironmentInjectionAction.class);
            if (action == null) {
                run.addAction(new EnvironmentInjectionAction(name, strVal));
            } else {
                action.put(name, strVal);
            }
        }
    }

    @Extension
    public static final class CustomEnvironmentContributor extends EnvironmentContributor {
        @Override
        public void buildEnvironmentFor(@Nonnull Run r,
                                        @Nonnull EnvVars envs,
                                        @Nonnull TaskListener listener) throws IOException, InterruptedException {
            super.buildEnvironmentFor(r, envs, listener);
            EnvironmentInjectionAction action = r.getAction(EnvironmentInjectionAction.class);
            if (action != null) {
                envs.putAll(action.getEnvs());
            }
        }
    }

    private static class EnvironmentInjectionAction implements Action {
        private final Map<String, String> pairs;

        EnvironmentInjectionAction(String name, String value) {
            pairs = new HashMap<>();
            pairs.put(name, value);
        }

        EnvironmentInjectionAction(Map<String, String> inputs) {
            pairs = new HashMap<>(inputs);
        }

        Map<String, String> getEnvs() {
            // no need to copy as it will only be used internally
            return pairs;
        }

        void put(String name, String value) {
            pairs.put(name, value);
        }

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return "Environment Injection Action";
        }

        @Override
        public String getUrlName() {
            return null;
        }
    }

    private EnvironmentInjector() {
        // hide constructor
    }
}
