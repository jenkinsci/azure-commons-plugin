/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.microsoft.jenkins.azurecommons.command;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.microsoft.jenkins.azurecommons.JobContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CommandServiceTest {
    private List<String> records;
    private CommandState lastCommandState;
    private ICommandServiceData commandServiceData;
    private MockCommandData commandData;

    @Before
    public void setup() {
        records = new ArrayList<>();
        commandServiceData = mock(ICommandServiceData.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                lastCommandState = invocation.getArgument(0);
                return null;
            }
        }).when(commandServiceData).setLastCommandState(any(CommandState.class));
        commandData = new MockCommandData(null);
        doReturn(commandData).when(commandServiceData).getDataForCommand(any(ICommand.class));
    }

    @Test
    public void testBuilder() {
        // Success -> Choice -> Success2 -> Error
        CommandService commandService = CommandService.builder()
                .withTransition(CSuccess.class, CChoice.class)
                .withTransition(CSuccess2.class, CError.class)
                .withStartCommand(CSuccess.class)
                .build();
        assertNull(commandService.getCleanUpCommand());
        assertEquals(CSuccess.class, commandService.getStartCommandClass());
        assertEquals(ImmutableSet.of(CSuccess.class, CChoice.class, CSuccess2.class, CError.class), commandService.getRegisteredCommands());
        assertEquals(ImmutableMap.of(CSuccess.class, CChoice.class, CSuccess2.class, CError.class), commandService.getTransitions());
    }

    @Test
    public void testSuccessTransition() {
        // Success -> Success
        CommandService commandService = CommandService.builder()
                .withTransition(CSuccess.class, CSuccess2.class)
                .withStartCommand(CSuccess.class)
                .build();

        assertEquals(ImmutableMap.of(CSuccess.class, CSuccess2.class), commandService.getTransitions());

        commandService.executeCommands(commandServiceData);
        verify(commandServiceData, times(2)).setLastCommandState(CommandState.Success);
        assertEquals(Arrays.asList("CSuccess", "CSuccess2"), records);
        assertEquals(CommandState.Success, lastCommandState);
    }

    @Test
    public void testSuccessErrorTransition() {
        CommandService commandService = CommandService.builder()
                .withTransition(CSuccess.class, CError.class)
                .withTransition(CError.class, CSuccess2.class)
                .withStartCommand(CSuccess.class)
                .build();

        commandService.executeCommands(commandServiceData);
        verify(commandServiceData, times(1)).setLastCommandState(CommandState.Success);
        verify(commandServiceData, times(1)).setLastCommandState(CommandState.HasError);
        assertEquals(Arrays.asList("CSuccess", "CError"), records);
        assertEquals(CommandState.HasError, lastCommandState);
    }

    @Test
    public void testChoiceTransition() {
        CommandService commandService = CommandService.builder()
                .withTransition(CChoice.class, CError.class) // dummy transition
                .withStartCommand(CChoice.class)
                .build();

        assertEquals(ImmutableMap.of(CChoice.class, CError.class), commandService.getTransitions());

        commandService.executeCommands(commandServiceData);
        verify(commandServiceData, times(2)).setLastCommandState(CommandState.Success);
        verify(commandServiceData, never()).setLastCommandState(CommandState.HasError);
        assertEquals(Arrays.asList("CChoice", "CSuccess2"), records);
        assertEquals(CommandState.Success, lastCommandState);
    }

    @Test
    public void testFinishWithDone() {
        CommandService commandService = CommandService.builder()
                .withTransition(CSuccess.class, CDone.class)
                .withTransition(CDone.class, CError.class)
                .withStartCommand(CSuccess.class)
                .build();

        assertEquals(ImmutableMap.of(CSuccess.class, CDone.class, CDone.class, CError.class), commandService.getTransitions());

        commandService.executeCommands(commandServiceData);
        verify(commandServiceData, times(1)).setLastCommandState(CommandState.Success);
        verify(commandServiceData, times(1)).setLastCommandState(CommandState.Done);
        verify(commandServiceData, never()).setLastCommandState(CommandState.HasError);
        assertEquals(Arrays.asList("CSuccess", "CDone"), records);
        assertEquals(CommandState.Done, lastCommandState);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoState() {
        CommandService commandService = CommandService.builder()
                .withTransition(CNoState.class, CSuccess.class)
                .withStartCommand(CNoState.class)
                .build();

        commandService.executeCommands(commandServiceData);
    }

    @Test(expected = RuntimeException.class)
    public void testException() {
        CommandService commandService = CommandService.builder()
                .withTransition(CSuccess.class, CException.class)
                .withStartCommand(CSuccess.class)
                .build();

        commandService.executeCommands(commandServiceData);
    }

    public static class CSuccess implements ICommand<IBaseCommandData> {
        @Override
        public void execute(IBaseCommandData context) {
            context.logStatus("CSuccess");
            context.setCommandState(CommandState.Success);
        }
    }

    public static class CSuccess2 implements ICommand<IBaseCommandData> {
        @Override
        public void execute(IBaseCommandData context) {
            context.logStatus("CSuccess2");
            context.setCommandState(CommandState.Success);
        }
    }

    public static class CDone implements ICommand<IBaseCommandData> {
        @Override
        public void execute(IBaseCommandData context) {
            context.logStatus("CDone");
            context.setCommandState(CommandState.Done);
        }
    }

    public static class CException implements ICommand<IBaseCommandData> {
        @Override
        public void execute(IBaseCommandData context) {
            throw new RuntimeException();
        }
    }

    public static class CChoice implements ICommand<IBaseCommandData>, INextCommandAware {
        @Override
        public void execute(IBaseCommandData context) {
            context.logStatus("CChoice");
            context.setCommandState(CommandState.Success);
        }

        @Override
        public Class nextCommand() {
            return CSuccess2.class;
        }
    }

    public static class CError implements ICommand<IBaseCommandData> {
        @Override
        public void execute(IBaseCommandData context) {
            context.logStatus("CError");
            context.setCommandState(CommandState.HasError);
        }
    }

    public static class CNoState implements ICommand<IBaseCommandData> {
        @Override
        public void execute(IBaseCommandData context) {
            context.logStatus("CNoState");
        }
    }

    private class MockCommandData implements IBaseCommandData {
        private CommandState commandState = CommandState.Unknown;
        private JobContext jobContext;

        MockCommandData(JobContext jobContext) {
            this.jobContext = jobContext;
        }

        @Override
        public void logError(String message) {
            records.add(message);
        }

        @Override
        public void logStatus(String status) {
            records.add(status);
        }

        @Override
        public void logError(Exception ex) {
            records.add(ex.getMessage());
        }

        @Override
        public void logError(String prefix, Exception ex) {
            records.add(prefix + ex.getMessage());
        }

        @Override
        public JobContext getJobContext() {
            return jobContext;
        }

        @Override
        public void setCommandState(CommandState state) {
            this.commandState = state;
        }

        @Override
        public CommandState getCommandState() {
            return commandState;
        }
    }
}
