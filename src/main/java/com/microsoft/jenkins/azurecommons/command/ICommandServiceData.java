/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.command;

import java.util.Map;

public interface ICommandServiceData {
    Class getStartCommandClass();

    Map<Class, TransitionInfo> getCommands();

    IBaseCommandData getDataForCommand(ICommand command);
}
