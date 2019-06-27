package org.jkiss.dbeaver.team.git.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.egit.ui.internal.sharing.SharingWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.team.git.ui.utils.GitUIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;

public class ShareHandler extends AbstractHandler {
    
    private static final String CMD_SHARE = "org.eclipse.egit.ui.command.shareProject";

    @Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    
	    //IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

        IProject project = GitUIUtils.extractActiveProject(event);

        if (project == null) {
            DBWorkbench.getPlatformUI().showError(
                    "Nothing to share - no active project",
                    "Select a project or resource to share");
            return null;
        }
        //IHandlerService handlerService = window.getService(IHandlerService.class);
	    //ICommandService commandService = window.getService(ICommandService.class);

        IWorkbench workbench = HandlerUtil.getActiveWorkbenchWindow(event).getWorkbench();
        SharingWizard wizard = new SharingWizard();
        wizard.init(workbench, project);
        Shell shell = HandlerUtil.getActiveShell(event);
        WizardDialog wizardDialog = new WizardDialog(shell, wizard);
        wizardDialog.setHelpAvailable(false);
        wizardDialog.open();
/*

        Command shareCommand = commandService.getCommand(CMD_SHARE);
	    
	    Parameterization[] params = new Parameterization[1];
	    
	    try {
            params[0] = new Parameterization(shareCommand.getParameters()[0], "General");
        } catch (NotDefinedException e) {
           throw new ExecutionException("Error in share command parameter", e);
        }
	    

	    ParameterizedCommand pshareCommand = new ParameterizedCommand(shareCommand,
	            params);
	    
        try {
            handlerService.executeCommand(pshareCommand, null);
        } catch (Exception ex) {
            DBWorkbench.getPlatformUI().showError("Error sharing a project", "Can't execute command '" + CMD_SHARE + "'", ex);
        }
*/

	    return null;
		
	}
}
