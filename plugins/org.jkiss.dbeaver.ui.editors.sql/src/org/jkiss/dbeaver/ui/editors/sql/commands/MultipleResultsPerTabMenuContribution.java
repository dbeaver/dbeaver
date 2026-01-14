/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.commands;


import org.eclipse.core.commands.*;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandImageService;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;

import java.util.Collections;


public class MultipleResultsPerTabMenuContribution extends ActionContributionItem {

    private static final ImageDescriptor multipleResultsPerTabImageFalse =
        DBeaverIcons.getImageDescriptor(UIIcon.SQL_MULTIPLE_RESULTS_PER_TAB_FALSE);
    private static final ImageDescriptor multipleResultsPerTabImageTrue =
        DBeaverIcons.getImageDescriptor(UIIcon.SQL_MULTIPLE_RESULTS_PER_TAB_TRUE);

    public static Image TRUE_IMAGE = multipleResultsPerTabImageFalse.createImage();
    public static Image FALSE_IMAGE = multipleResultsPerTabImageTrue.createImage();

    private static Action action = null;

    private abstract static  class CommandAction extends Action {
        protected final Command command;

        public CommandAction(@NotNull IServiceLocator serviceLocator, String commandId) {
            Command command = ActionUtils.findCommand(commandId);
            if (command == null) {
                throw new IllegalArgumentException("Failed to resolve command by id '" + commandId + "'");
            }
            String label;
            String tooltip;
            try {
                label = command.getName();
                tooltip = command.getDescription();
            } catch (Throwable e) {
                final String errorMessage = "Failed to resolve command parameters for unknown command '" + commandId + "'";
                throw new IllegalArgumentException(errorMessage, e);
            }

            this.command = command;
            ICommandImageService service = serviceLocator.getService(ICommandImageService.class);
            this.setImageDescriptor(service.getImageDescriptor(
                    command.getId(), ICommandImageService.TYPE_DEFAULT, ICommandImageService.IMAGE_STYLE_DEFAULT));
            this.setDisabledImageDescriptor(service.getImageDescriptor(
                    command.getId(), ICommandImageService.TYPE_DISABLED, ICommandImageService.IMAGE_STYLE_DEFAULT));
            this.setHoverImageDescriptor(service.getImageDescriptor(
                    command.getId(), ICommandImageService.TYPE_HOVER, ICommandImageService.IMAGE_STYLE_DEFAULT));

            this.setText(label);
            this.setDescription(tooltip);
            this.setToolTipText(tooltip);
        }

        @Override
        public void run() {
            try {
                executeCommand();
            } catch (CommandException e) {
                DBWorkbench.getPlatformUI().showError("Command action error", "An error occurred during command action execution", e);
            }
        }

        protected void executeCommand() throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
            ISelection selection = new StructuredSelection();
            EvaluationContext context = new EvaluationContext(null, selection);
            context.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, selection);

            ExecutionEvent event = new ExecutionEvent(command, Collections.EMPTY_MAP, null, context);
            command.executeWithChecks(event);
        }

    }

    private static class MultipleResultsPerTabAction extends CommandAction {
        public MultipleResultsPerTabAction() {
            super(PlatformUI.getWorkbench(), SQLEditorCommands.CMD_MULTIPLE_RESULTS_PER_TAB);
        }
    }
    
    // this thing instantiated once for the main menu and then each time on editor context menu preparation 
    
    public MultipleResultsPerTabMenuContribution() {
        super(getContributedAction());
    }

    @NotNull
    private static Action getContributedAction() {
        return action != null ? action : (action = new MultipleResultsPerTabAction());
    }
    
    public static void syncWithEditor(@NotNull SQLEditor editor) {
        Action action = getContributedAction();
        boolean multipleResultsPerTab = editor.isMultipleResultsPerTabEnabled();
        action.setImageDescriptor(multipleResultsPerTab ? multipleResultsPerTabImageTrue : multipleResultsPerTabImageFalse);
        action.setChecked(multipleResultsPerTab);
    }
}
