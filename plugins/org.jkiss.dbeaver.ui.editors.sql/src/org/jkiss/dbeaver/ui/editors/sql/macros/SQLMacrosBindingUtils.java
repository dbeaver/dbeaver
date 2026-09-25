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
package org.jkiss.dbeaver.ui.editors.sql.macros;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.keys.IBindingService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for registering user-defined macro key bindings in the workbench.
 * <p>
 * Every macro with a custom shortcut, e.g. Ctrl+Alt+F1, gets its own runtime binding added via
 * the workbench {@link BindingService}. The binding is a parameterized command
 * ({@link SQLMacrosConstants#APPLY_MACRO_COMMAND_ID} with the
 * {@link SQLMacrosConstants#MACRO_ID_PARAMETER} set to the macro id), so the number of macros is
 * not limited. Bindings are added to the live e4 model only and are not persisted; they are
 * registered again on each application start (see {@link SQLMacrosStartup}) and synchronized
 * whenever the macros configuration changes.
 */
public final class SQLMacrosBindingUtils {

    private static final Log log = Log.getLog(SQLMacrosBindingUtils.class);

    private static final List<KeyBinding> registeredBindings = new ArrayList<>();

    private SQLMacrosBindingUtils() {
    }

    /**
     * Rebuilds the user-defined macro key bindings from the given macros. The work is performed on the UI thread.
     * Does nothing if the workbench is not running yet.
     */
    public static void refreshMacroBindings(@NotNull List<SQLMacro> macros) {
        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }
        List<SQLMacro> snapshot = List.copyOf(macros);
        if (UIUtils.isUIThread()) {
            doRefreshMacroBindings(snapshot);
        } else {
            UIUtils.asyncExec(() -> doRefreshMacroBindings(snapshot));
        }
    }

    /**
     * Returns the name of the macro or workbench command which already uses the given key sequence,
     * or null if the sequence is free.
     * <p>
     * The macro itself is excluded from the search by its id, and the user-defined macro bindings
     * (parameterized commands of the apply-macro command) are ignored as well - they are covered
     * by the macro list scan above.
     */
    @Nullable
    public static String findShortcutConflict(
            @NotNull List<SQLMacro> macros,
            @NotNull SQLMacro editedMacro,
            @NotNull KeySequence sequence
    ) {
        for (SQLMacro macro : macros) {
            if (CommonUtils.equalObjects(macro.getId(), editedMacro.getId())) {
                continue;
            }
            KeySequence macroSequence = macro.getShortcutKeySequence();
            if (macroSequence != null && macroSequence.equals(sequence)) {
                return macro.getName();
            }
        }
        if (!PlatformUI.isWorkbenchRunning() || !UIUtils.isUIThread()) {
            return null;
        }
        IBindingService bindingService = PlatformUI.getWorkbench().getService(IBindingService.class);
        if (!(bindingService instanceof BindingService bindingSvc)) {
            return null;
        }
        for (Binding binding : bindingSvc.getBindings()) {
            ParameterizedCommand bindingCommand = binding.getParameterizedCommand();
            if (bindingCommand == null) {
                continue;
            }
            if (SQLMacrosConstants.APPLY_MACRO_COMMAND_ID.equals(bindingCommand.getId())) {
                continue;
            }
            if (binding.getTriggerSequence() instanceof KeySequence boundSequence
                && boundSequence.equals(sequence)) {
                return getCommandName(bindingCommand);
            }
        }
        return null;
    }

    private static synchronized void doRefreshMacroBindings(@NotNull List<SQLMacro> macros) {
        try {
            IBindingService bindingService = PlatformUI.getWorkbench().getService(IBindingService.class);
            if (!(bindingService instanceof BindingService bindingSvc)) {
                return;
            }
            for (KeyBinding binding : registeredBindings) {
                bindingSvc.removeBinding(binding);
            }
            registeredBindings.clear();
            ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
            Command applyCommand = commandService.getCommand(SQLMacrosConstants.APPLY_MACRO_COMMAND_ID);
            for (SQLMacro macro : macros) {
                KeySequence sequence = macro.getShortcutKeySequence();
                if (sequence == null) {
                    continue;
                }
                Parameterization[] parameters = new Parameterization[]{
                        new Parameterization(
                                applyCommand.getParameter(SQLMacrosConstants.MACRO_ID_PARAMETER),
                                macro.getId())
                };
                ParameterizedCommand command = new ParameterizedCommand(applyCommand, parameters);
                KeyBinding binding = new KeyBinding(
                        sequence,
                        command,
                        SQLMacrosConstants.DEFAULT_SCHEME_ID,
                        SQLMacrosConstants.SQL_EDITOR_SCRIPT_FOCUSED_CONTEXT_ID,
                        null,
                        null,
                        null,
                        Binding.USER);
                bindingSvc.addBinding(binding);
                registeredBindings.add(binding);
            }
        } catch (Throwable e) {
            log.debug("Error updating SQL macro key bindings", e);
        }
    }

    @NotNull
    private static String getCommandName(@NotNull ParameterizedCommand command) {
        try {
            return command.getName();
        } catch (NotDefinedException e) {
            return command.getId();
        }
    }
}