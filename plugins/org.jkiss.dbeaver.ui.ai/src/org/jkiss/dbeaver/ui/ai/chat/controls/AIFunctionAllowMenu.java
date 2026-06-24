/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Native SWT menu used by the browser-based AI chat to choose function allow mode.
 */
public final class AIFunctionAllowMenu {
    private static final Log log = Log.getLog(AIFunctionAllowMenu.class);

    private final Browser browser;
    private final AIToolboxManager toolboxManager;
    @Nullable
    private final Function<Integer, PendingConfirmationState> pendingConfirmationProvider;
    @Nullable
    private final Consumer<List<PendingApproval>> approvalCallback;
    private final Set<String> sessionAllowedFunctions = new LinkedHashSet<>();
    private final Set<String> sessionAllowedToolboxes = new LinkedHashSet<>();

    public AIFunctionAllowMenu(
        @NotNull Browser browser,
        @NotNull AIToolboxManager toolboxManager,
        @Nullable Function<Integer, PendingConfirmationState> pendingConfirmationProvider,
        @Nullable Consumer<List<PendingApproval>> approvalCallback
    ) {
        this.browser = browser;
        this.toolboxManager = toolboxManager;
        this.pendingConfirmationProvider = pendingConfirmationProvider;
        this.approvalCallback = approvalCallback;
        new BrowserFunction(AIFunctionAllowMenu.this.browser, "showAIFunctionAllowMenu") {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length < 3 || !(arguments[0] instanceof String functionFullId)) {
                    log.debug("Invalid arguments for showAIFunctionAllowMenu");
                    return null;
                }

                PendingApproval pendingApproval = arguments.length >= 5 ? new PendingApproval(
                    CommonUtils.toInt(arguments[3]),
                    CommonUtils.toInt(arguments[4])
                ) : null;
                showMenu(
                    functionFullId,
                    AIFunctionAllowMenu.this.browser.toDisplay(CommonUtils.toInt(arguments[1]), CommonUtils.toInt(arguments[2])),
                    pendingApproval
                );
                return null;
            }
        };
    }

    @NotNull
    public List<AIFunctionCall> getFunctionCallsToConfirm(@NotNull AIFunctionCallConfirmation confirmation) {
        List<AIFunctionCall> functionCalls = new ArrayList<>(confirmation.getFunctionCallsToConfirm(toolboxManager));
        functionCalls.removeIf(this::isAllowedInConversation);
        return functionCalls;
    }

    @NotNull
    public List<AIFunctionCall> getAutoApprovedFunctionCalls(
        @NotNull AIFunctionCallConfirmation confirmation,
        @NotNull List<AIFunctionCall> callsToConfirm
    ) {
        if (callsToConfirm.isEmpty()) {
            return confirmation.getFunctionCalls();
        }
        List<AIFunctionCall> autoApprovedCalls = new ArrayList<>();
        for (AIFunctionCall functionCall : confirmation.getFunctionCalls()) {
            if (!callsToConfirm.contains(functionCall)) {
                autoApprovedCalls.add(functionCall);
            }
        }
        return autoApprovedCalls;
    }

    private boolean isAllowedInConversation(@NotNull AIFunctionCall functionCall) {
        AIFunctionDescriptor function = functionCall.getOrResolveFunction(toolboxManager);
        return function != null && (sessionAllowedFunctions.contains(function.getFullId())
            || sessionAllowedToolboxes.contains(function.getToolbox().getToolboxId()));
    }

    private void showMenu(@NotNull String functionFullId, @NotNull Point location, @Nullable PendingApproval pendingApproval) {
        AIFunctionDescriptor function = toolboxManager.getFunctionByFullId(functionFullId);
        if (function == null) {
            log.debug("Function '" + functionFullId + "' not found");
            return;
        }

        AIFunctionSettings.ToolboxSettings toolboxSettings = toolboxManager.getFunctionSettings().getToolboxSettings(function.getToolbox());
        AIFunctionAllowMode allowMode = toolboxSettings.getFunctionAllowMode(function);

        Menu menu = new Menu(browser);
        menu.addListener(SWT.Hide, event -> UIUtils.asyncExec(menu::dispose));

        createActionItem(
            menu, AIUIMessages.ai_function_allow_menu_ask_every_time, SWT.RADIO, allowMode == AIFunctionAllowMode.ASK, () -> {
                toolboxSettings.setFunctionAllowMode(function, AIFunctionAllowMode.ASK);
                saveFunctionSettings();
                handleSelection(Action.ASK_EVERY_TIME, function, pendingApproval);
            }
        );

        new MenuItem(menu, SWT.SEPARATOR);
        createHeaderItem(menu, AIUIMessages.ai_function_allow_menu_session_only);
        createActionItem(
            menu,
            AIUIMessages.ai_function_allow_menu_allow_tool_session_only,
            SWT.PUSH,
            false,
            () -> handleSelection(Action.ALLOW_TOOL_IN_SESSION, function, pendingApproval)
        );
        createActionItem(
            menu,
            AIUIMessages.ai_function_allow_menu_allow_agent_session_only,
            SWT.PUSH,
            false,
            () -> handleSelection(Action.ALLOW_TOOLBOX_IN_SESSION, function, pendingApproval)
        );

        if (!DBWorkbench.isDistributed() || DBWorkbench.getPlatform().getWorkspace()
            .hasRealmPermission(DBAPermissionRealm.PERMISSION_ADMIN)) {
            new MenuItem(menu, SWT.SEPARATOR);
            createHeaderItem(menu, AIUIMessages.ai_function_allow_menu_always);
            createActionItem(
                menu,
                AIUIMessages.ai_function_allow_menu_allow_tool_always,
                SWT.RADIO,
                allowMode == AIFunctionAllowMode.ALWAYS_ALLOW,
                () -> {
                    toolboxSettings.setFunctionAllowMode(function, AIFunctionAllowMode.ALWAYS_ALLOW);
                    saveFunctionSettings();
                    handleSelection(Action.ALWAYS_ALLOW_TOOL, function, pendingApproval);
                }
            );
            createActionItem(
                menu, AIUIMessages.ai_function_allow_menu_allow_agent_always, SWT.PUSH, false, () -> {
                    allowToolbox(function.getToolbox(), toolboxSettings);
                    saveFunctionSettings();
                    handleSelection(Action.ALWAYS_ALLOW_TOOLBOX, function, pendingApproval);
                }
            );

            new MenuItem(menu, SWT.SEPARATOR);
            createActionItem(
                menu, AIUIMessages.ai_function_allow_menu_configure, SWT.PUSH, false, () -> {
                    UIUtils.showPreferencesFor(browser.getShell(), null, "com.dbeaver.preferences.ai.tools");
                    handleSelection(Action.CONFIGURE_TOOLS, function, pendingApproval);
                }
            );
        }

        menu.setLocation(location);
        menu.setVisible(true);
    }

    private void handleSelection(
        @NotNull Action action,
        @NotNull AIFunctionDescriptor function,
        @Nullable PendingApproval pendingApproval
    ) {
        switch (action) {
            case ALLOW_TOOL_IN_SESSION -> sessionAllowedFunctions.add(function.getFullId());
            case ALLOW_TOOLBOX_IN_SESSION -> sessionAllowedToolboxes.add(function.getToolbox().getToolboxId());
            default -> {}
        }

        if (pendingApproval != null && shouldApproveFunctionCall(action) && approvalCallback != null) {
            approvalCallback.accept(collectPendingApprovals(action, function, pendingApproval));
        }
    }

    @NotNull
    private List<PendingApproval> collectPendingApprovals(
        @NotNull Action action,
        @NotNull AIFunctionDescriptor function,
        @NotNull PendingApproval pendingApproval
    ) {
        List<PendingApproval> approvals = new ArrayList<>();
        approvals.add(pendingApproval);

        if (pendingConfirmationProvider == null) {
            return approvals;
        }
        PendingConfirmationState pendingState = pendingConfirmationProvider.apply(pendingApproval.messageId());
        if (pendingState == null) {
            return approvals;
        }

        for (int i = 0; i < pendingState.confirmation().getFunctionCalls().size(); i++) {
            if (i == pendingApproval.functionIndex() || pendingState.approvedIndices().contains(i) || pendingState.declinedIndices()
                .contains(i)) {
                continue;
            }
            AIFunctionCall siblingCall = pendingState.confirmation().getFunctionCalls().get(i);
            AIFunctionDescriptor siblingFunction = siblingCall.getOrResolveFunction(toolboxManager);
            if (siblingFunction == null || !matchesApprovalTarget(action, function, siblingFunction)) {
                continue;
            }
            approvals.add(new PendingApproval(pendingApproval.messageId(), i));
        }
        return approvals;
    }

    private static boolean matchesApprovalTarget(
        @NotNull Action action,
        @NotNull AIFunctionDescriptor baseFunction,
        @NotNull AIFunctionDescriptor siblingFunction
    ) {
        return switch (action) {
            case ALLOW_TOOL_IN_SESSION, ALWAYS_ALLOW_TOOL -> baseFunction.getFullId().equals(siblingFunction.getFullId());
            case ALLOW_TOOLBOX_IN_SESSION, ALWAYS_ALLOW_TOOLBOX ->
                baseFunction.getToolbox().getToolboxId().equals(siblingFunction.getToolbox().getToolboxId());
            case ASK_EVERY_TIME, CONFIGURE_TOOLS -> false;
        };
    }

    private static boolean shouldApproveFunctionCall(@NotNull Action action) {
        return switch (action) {
            case ALLOW_TOOL_IN_SESSION, ALLOW_TOOLBOX_IN_SESSION, ALWAYS_ALLOW_TOOL, ALWAYS_ALLOW_TOOLBOX -> true;
            case ASK_EVERY_TIME, CONFIGURE_TOOLS -> false;
        };
    }

    private void saveFunctionSettings() {
        try {
            toolboxManager.saveFunctionSettings();
        } catch (Exception e) {
            log.error("Error saving AI function allow settings", e);
        }
    }

    private static void allowToolbox(@NotNull AIToolbox toolbox, @NotNull AIFunctionSettings.ToolboxSettings toolboxSettings) {
        for (AIFunctionDescriptor function : toolbox.getSupportedFunctions(AIFunctionPurpose.TOOL)) {
            toolboxSettings.setFunctionAllowMode(function, AIFunctionAllowMode.ALWAYS_ALLOW);
        }
    }

    private static void createHeaderItem(@NotNull Menu menu, @NotNull String text) {
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText(text);
        item.setEnabled(false);
    }

    private static void createActionItem(@NotNull Menu menu, @NotNull String text, int style, boolean selected, @NotNull Runnable action) {
        MenuItem item = new MenuItem(menu, style);
        item.setText(text);
        item.setSelection(selected);
        item.addListener(SWT.Selection, event -> action.run());
    }

    public record PendingApproval(int messageId, int functionIndex) {
    }

    public record PendingConfirmationState(@NotNull AIFunctionCallConfirmation confirmation,
        @NotNull Set<Integer> approvedIndices,
        @NotNull Set<Integer> declinedIndices) {
        public PendingConfirmationState {
            approvedIndices = Set.copyOf(approvedIndices);
            declinedIndices = Set.copyOf(declinedIndices);
        }
    }

    private enum Action {
        ASK_EVERY_TIME,
        ALLOW_TOOL_IN_SESSION,
        ALLOW_TOOLBOX_IN_SESSION,
        ALWAYS_ALLOW_TOOL,
        ALWAYS_ALLOW_TOOLBOX,
        CONFIGURE_TOOLS
    }
}
