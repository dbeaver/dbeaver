package org.jkiss.dbeaver.ui.e4;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.RenameHandler;

import java.io.File;

public class DBeaverStackRenderer extends StackRenderer {

    @Override
    protected void populateTabMenu(Menu menu, MPart part) {
        super.populateTabMenu(menu, part);

        IWorkbenchPart workbenchPart = getWorkbenchPart(part);
        if (workbenchPart instanceof IEditorPart) {
            IEditorInput editorInput = ((IEditorPart) workbenchPart).getEditorInput();
            File localFile = EditorUtils.getLocalFileFromInput(editorInput);
            if (localFile != null) {
                populateFileMenu(menu, workbenchPart, EditorUtils.getFileFromInput(editorInput), localFile);
            }
        }
    }

    private void populateFileMenu(@NotNull final Menu menu, @NotNull final IWorkbenchPart workbenchPart, @Nullable final IFile inputFile, @NotNull final File file) {
        new MenuItem(menu, SWT.SEPARATOR);

        {
            MenuItem menuItemOpenFolder = new MenuItem(menu, SWT.NONE);
            menuItemOpenFolder.setText(CoreMessages.editor_file_open_in_explorer);
            menuItemOpenFolder.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (file.getParentFile().isDirectory()) {
                        UIUtils.launchProgram(file.getParentFile().getAbsolutePath());
                    }
                }
            });
        }
        {
            MenuItem menuItemOthers = new MenuItem(menu, SWT.NONE);
            menuItemOthers.setText(CoreMessages.editor_file_copy_path);
            menuItemOthers.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String filePath = file.getAbsolutePath();
                    UIUtils.setClipboardContents(Display.getCurrent(), TextTransfer.getInstance(), filePath);
                }
            });
        }
    
        if (inputFile != null) {
            {
		        MenuItem menuItemDelete = new MenuItem(menu, SWT.NONE);
		        menuItemDelete.setText(CoreMessages.dbeaver_stack_renderer_menu_item_delete_this_script);
		        menuItemDelete.addSelectionListener(new SelectionAdapter() {
			        @Override
			        public void widgetSelected(SelectionEvent e) {
			           if (UIUtils.confirmAction(CoreMessages.dbeaver_stack_renderer_widget_selected_confirm_delete_title, NLS.bind(CoreMessages.dbeaver_stack_renderer_widget_selected_confirm_delete_text, inputFile.getName()))) {			      //$NON-NLS-3$
			        	   try {
			        		   inputFile.delete(true, true, new NullProgressMonitor());
			        	   } catch (CoreException e1) {
			        		   DBWorkbench.getPlatformUI().showError(CoreMessages.dbeaver_stack_renderer_delete_error_title, NLS.bind(CoreMessages.dbeaver_stack_renderer_delete_error_text, inputFile.getName(), e1));
						   }
			           }
			        } 
			    });
            }
            
            MenuItem menuItemOthers = new MenuItem(menu, SWT.NONE);
            String renameText = CoreMessages.editor_file_rename;
            if (workbenchPart instanceof SQLEditor) {
                renameText += "\t" + ActionUtils.findCommandDescription(SQLEditorCommands.CMD_SQL_RENAME, workbenchPart.getSite(), true); //$NON-NLS-1$
            }
            menuItemOthers.setText(renameText);
            menuItemOthers.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    RenameHandler.renameFile(workbenchPart, inputFile, "file"); //$NON-NLS-1$
                }
            });
        }

    }

    private IWorkbenchPart getWorkbenchPart(MPart part) {
        if (part != null) {
            Object clientObject = part.getObject();
            if (clientObject instanceof CompatibilityPart) {
                return ((CompatibilityPart) clientObject).getPart();
            }
        }
        return null;
    }

}