package org.jkiss.dbeaver.ui.editors.sql.commands;


import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
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


public class ToggleUseTabPerResultMenuContribution extends ActionContributionItem {

    private static final ImageDescriptor useTabPerResultImageTrue = DBeaverIcons.getImageDescriptor(UIIcon.SQL_USE_TAB_PER_RESULT_TRUE);
    private static final ImageDescriptor useTabPerResultImageFalse = DBeaverIcons.getImageDescriptor(UIIcon.SQL_USE_TAB_PER_RESULT_FALSE);

    public static Image TRUE_IMAGE = useTabPerResultImageTrue.createImage();
    public static Image FALSE_IMAGE = useTabPerResultImageFalse.createImage();

    private static Action action = null;

    private static abstract class CommandAction extends Action {
        protected final Command command;

        public CommandAction(@NotNull IServiceLocator serviceLocator, String commandId) {
            Command command = ActionUtils.findCommand(commandId);
            Throwable error;
            String label, tooltip;
            try {
                label = command.getName();
                tooltip = command.getDescription();
                error = null;
            } catch (Throwable e) {
                label = null;
                tooltip = null;
                error = e;
            }
            if (error != null || command == null) {
                final String errorMessage = "Failed to resolve command parameters for unknown command '" + commandId + "'";
                throw command == null ? new RuntimeException(errorMessage) : new RuntimeException(errorMessage, error);
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

//            this.setEnabled(isEnabled());
//            this.setChecked(isChecked());
        }

        @Override
        public void run() {
            try {
                executeCommand(true);
            } catch (CommandException e) {
                DBWorkbench.getPlatformUI().showError("Command action error", "An error occurred during command action execution", e);
            }
        }

        protected void executeCommand(boolean withChecks)
                throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
            // ISelection selection = new StructuredSelection(objects);
            ISelection selection = new StructuredSelection();
            EvaluationContext context = new EvaluationContext(null, selection);
            context.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, selection);

            ExecutionEvent event = new ExecutionEvent(command, Collections.EMPTY_MAP, null, context);
            if (withChecks) {
                command.executeWithChecks(event);
            } else {
                command.execute(event);
            }
        }

    }

    private static class ToggleUseTabPerResultAction extends CommandAction {
        public ToggleUseTabPerResultAction() {
            super(PlatformUI.getWorkbench(), SQLEditorCommands.CMD_TOGGLE_USE_TAB_PER_RESULT);
        }
    }   

    
    // this thing instantiated once for the main menu and then each time on editor context menu preparation 
    
    public ToggleUseTabPerResultMenuContribution() {
        super(getContributedAction());
    }

    @NotNull
    private static Action getContributedAction() {
        return action != null ? action : (action = new ToggleUseTabPerResultAction());
    }
    
    public static void syncWithEditor(@NotNull SQLEditor editor) {
        Action action = getContributedAction();
        boolean useTabPerResult = editor.isUseTabPerResultEnabled();
        action.setImageDescriptor(useTabPerResult ? useTabPerResultImageTrue : useTabPerResultImageFalse);
        action.setChecked(useTabPerResult);
    }
}
