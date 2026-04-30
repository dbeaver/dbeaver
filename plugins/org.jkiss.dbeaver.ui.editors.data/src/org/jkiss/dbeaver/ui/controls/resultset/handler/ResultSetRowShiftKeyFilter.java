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
package org.jkiss.dbeaver.ui.controls.resultset.handler;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetCommands;

/**
 * Shift-companion key filter for row Add / Duplicate commands (#12106).
 *
 * The toolbar buttons for "Add row" and "Duplicate row" already support holding
 * Shift to insert the new row before the current row — the click handler reads
 * the SWT.SHIFT bit from the trigger event. The keyboard shortcuts could not
 * mirror that, because Eclipse's binding manager requires an exact chord match
 * and normalizes the modifier mask before dispatch, so {@code Shift+<chord>}
 * never reached the row.add / row.copy handler.
 *
 * Rather than declaring a hard-coded {@code Shift+Alt+Insert} binding, this
 * filter watches every {@link SWT#KeyDown} event and, while the result-set
 * context is active, checks whether the chord is the user's currently-bound
 * row.add / row.copy chord with an extra Shift modifier. If so, it dispatches
 * the corresponding {@code .before} command and consumes the event. This keeps
 * the Shift companion working even after the user customizes the row.add /
 * row.copy shortcut in Preferences > Keys.
 */
public final class ResultSetRowShiftKeyFilter implements Listener {

    private static final Log log = Log.getLog(ResultSetRowShiftKeyFilter.class);
    private static final String RS_FOCUSED_CONTEXT_ID = "org.jkiss.dbeaver.ui.context.resultset.focused";

    private static volatile boolean installed;

    private ResultSetRowShiftKeyFilter() {
    }

    /**
     * Installs the global filter once per JVM. Subsequent calls are no-ops.
     * Safe to call from any UI-thread context.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (display == null || display.isDisposed()) {
            return;
        }
        display.addFilter(SWT.KeyDown, new ResultSetRowShiftKeyFilter());
        installed = true;
    }

    @Override
    public void handleEvent(Event event) {
        if ((event.stateMask & SWT.SHIFT) == 0 || event.keyCode == 0) {
            return;
        }
        try {
            IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
            if (contextService == null
                || !contextService.getActiveContextIds().contains(RS_FOCUSED_CONTEXT_ID)
            ) {
                return;
            }
            IBindingService bindingService = PlatformUI.getWorkbench().getService(IBindingService.class);
            if (bindingService == null) {
                return;
            }

            int chordModifiers = event.stateMask & ~SWT.SHIFT;
            int chordKey = event.keyCode;

            if (matchesActiveBinding(bindingService, IResultSetCommands.CMD_ROW_ADD, chordModifiers, chordKey)) {
                if (dispatch(IResultSetCommands.CMD_ROW_ADD, event)) {
                    event.doit = false;
                }
                return;
            }
            if (matchesActiveBinding(bindingService, IResultSetCommands.CMD_ROW_COPY, chordModifiers, chordKey)) {
                if (dispatch(IResultSetCommands.CMD_ROW_COPY, event)) {
                    event.doit = false;
                }
            }
        } catch (Throwable t) {
            // Never let this filter break unrelated keystrokes
            log.debug("Shift companion dispatch failed", t);
        }
    }

    private static boolean matchesActiveBinding(
        IBindingService bindingService,
        String commandId,
        int modifiers,
        int keyCode
    ) {
        TriggerSequence trigger = bindingService.getBestActiveBindingFor(commandId);
        if (!(trigger instanceof KeySequence keySequence)) {
            return false;
        }
        KeyStroke[] strokes = keySequence.getKeyStrokes();
        if (strokes.length != 1) {
            // Multi-stroke chords (e.g. Ctrl+K Ctrl+R) are out of scope.
            return false;
        }
        KeyStroke stroke = strokes[0];
        return stroke.getNaturalKey() == keyCode && stroke.getModifierKeys() == modifiers;
    }

    private static boolean dispatch(String commandId, Event triggerEvent) {
        IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        if (handlerService == null || commandService == null) {
            return false;
        }
        Command command = commandService.getCommand(commandId);
        if (command == null || !command.isDefined()) {
            return false;
        }
        try {
            IParameter placementParam = command.getParameter(IResultSetCommands.PARAM_ROW_PLACEMENT);
            ParameterizedCommand parameterized = placementParam == null
                ? ParameterizedCommand.generateCommand(command, null)
                : new ParameterizedCommand(command, new Parameterization[]{
                    new Parameterization(placementParam, IResultSetCommands.PARAM_ROW_PLACEMENT_BEFORE)
                });
            handlerService.executeCommand(parameterized, triggerEvent);
            return true;
        } catch (Exception e) {
            log.debug("Failed to execute " + commandId + " from Shift companion filter", e);
            return false;
        }
    }
}
