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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Map;

public class SQLEditorHandlerToggleMultipleResultsPerTab extends AbstractHandler implements IElementUpdater {
    private static final Log log = Log.getLog(SQLEditorHandlerToggleMultipleResultsPerTab.class);

    private static final ICommandListener toolItemSelectionListener = cmdEvent -> {
        // Each time a new contribution instance is being created, workbench subscribes for the command to update its state,
        // which we don't actually want in this case, but cannot prevent, apparently.
        // Then this listener sometimes updates UI state of the tool items without IElementUpdater invocation, which breaks the whole point.
        // And workbench never actually removes this command listener, so it leaks with each and any contribution instance created.
        //     see org.eclipse.e4.ui.workbench.renderers.swt.HandledContributionItem::generateCommand()

        // We want our update logic to always be the last performed, but internal listener of the workbench uses asyncExec(),
        // so we need to skip a few cycles (no less than two) to ensure we'll override whatever the state workbench sets.
        UIUtils.asyncExec(() -> UIUtils.asyncExec(() -> UIUtils.asyncExec(SQLEditor::updateLocalCommandsState)));

        // Example scenario:
        //         [workbench activity]                                         [our activity]
        //  1. command event raised
        //                                                              2. this listener called and queues callback A1
        //  3. workbench's listener called and queues callback B1
        //                                                              4. our callback A1 called and queues callback A2
        //  5. workbench's callback B1 called and modifies widget state
        //     (applying unwanted global command state)                 6. our callback A2 called and queues callback A3
        //  7. maybe other events occur,
        //     whose outcome we might want to override                  8. our callback A3 called and asks for widget state update
        //
    };

    private final Command command;

    public SQLEditorHandlerToggleMultipleResultsPerTab() {
        ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
        command = commandService.getCommand(SQLEditorCommands.CMD_MULTIPLE_RESULTS_PER_TAB);
        command.addCommandListener(toolItemSelectionListener);
    }

    @Override
    public void dispose() {
        command.removeCommandListener(toolItemSelectionListener);
        super.dispose();
    }

    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor == null) {
            log.error("No active SQL editor found");
            return null;
        }

        editor.toggleMultipleResultsPerTab();
        editor.refreshActions();

        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        //
        // DO NOT update element's state here, because command's associated flag state is unique for each SQLEditor instance,
        //        while IElementUpdater intended for all the command contributions to reflect only one shared state!
        //
        // BUT workbench still uses IElementUpdater sometimes even if we don't fire refreshElements() explicitly,
        // which resets our adjustments, so aggregate all these update notifications with job and apply desired state contextfully.
        //
        // Associated tool item state should always be updated explicitly only for the containing SQLEditor instance!
        //     see SQLEditor::refreshActions(..)
        //         SQLEditor::updateMultipleResultsPerTabToolItem(..)
        //
        // Calling it in the end of SQLEditor::createControlsBar(..) should have been enough for initialization stage,
        // and all other cases were handled explicitly, but this IElementUpdater implementation was still introduced for some reason.
        //
        // TODO consider certain infrastructure for such contextful toggle commands because we have others like this
        //     (see NavigatorHandlerConnectionFilter for example)

        SQLEditor.updateLocalCommandsState();
    }
}
