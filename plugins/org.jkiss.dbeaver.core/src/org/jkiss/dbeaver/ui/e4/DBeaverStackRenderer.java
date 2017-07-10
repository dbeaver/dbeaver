package org.jkiss.dbeaver.ui.e4;

import org.eclipse.core.resources.IFile;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityPart;
import org.jkiss.dbeaver.core.CoreCommands;
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
            IFile inputFile = EditorUtils.getFileFromInput(((IEditorPart) workbenchPart).getEditorInput());
            if (inputFile != null) {
                File localFile = inputFile.getLocation().toFile();
                if (localFile != null) {
                    populateFileMenu(menu, workbenchPart, inputFile, localFile);
                }
            }
        }
    }

    private void populateFileMenu(final Menu menu, final IWorkbenchPart workbenchPart, final IFile inputFile, final File file) {
        new MenuItem(menu, SWT.SEPARATOR);

        {
            MenuItem menuItemOpenFolder = new MenuItem(menu, SWT.NONE);
            menuItemOpenFolder.setText("Open Folder in Explorer");
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
            menuItemOthers.setText("Copy File Path");
            menuItemOthers.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String filePath = file.getAbsolutePath();
                    UIUtils.setClipboardContents(Display.getCurrent(), TextTransfer.getInstance(), filePath);
                }
            });
        }

        {
            MenuItem menuItemOthers = new MenuItem(menu, SWT.NONE);
            String renameText = "Rename File";
            if (workbenchPart instanceof SQLEditor) {
                renameText += "\t" + ActionUtils.findCommandDescription(CoreCommands.CMD_SQL_RENAME, workbenchPart.getSite(), true);
            }
            menuItemOthers.setText(renameText);
            menuItemOthers.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    RenameHandler.renameFile(workbenchPart, inputFile, "file");
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