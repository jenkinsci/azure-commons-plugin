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

public class EnvironmentInjector {
    /**
     * Add an environment variable binding for the given Run.
     * <p>
     * The {@link hudson.model.EnvironmentContributingAction} is more preferred for the environment variable injection.
     * However, is not compatible with workflow at the time of implementation.
     * <p>
     * We register the {@link CustomEnvironmentContributor} which scans for private
     * {@code EnvironmentInjectionAction} bound to the Run instance, and updates the environment variables
     * accordingly. This will be called by {@link Run#getEnvironment(TaskListener)}.
     *
     * @param run   the run object
     * @param name  the variable name
     * @param value the variable value
     * @see CustomEnvironmentContributor
     * @see <a href="https://issues.jenkins-ci.org/browse/JENKINS-29537">JENKINS-29537</a>
     */
    public static void inject(Run<?, ?> run, String name, Object value) {
        EnvironmentInjectionAction action = run.getAction(EnvironmentInjectionAction.class);
        if (action == null) {
            run.addAction(new EnvironmentInjectionAction(name, value.toString()));
        } else {
            action.put(name, value.toString());
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
}
