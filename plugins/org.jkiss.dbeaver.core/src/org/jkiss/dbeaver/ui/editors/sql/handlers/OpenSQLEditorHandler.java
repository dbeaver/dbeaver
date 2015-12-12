/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.resources.ScriptsHandlerImpl;

import java.util.ArrayList;
import java.util.List;

public class OpenSQLEditorHandler extends BaseSQLEditorHandler {

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer(event, false);
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);

        IProject project = dataSourceContainer != null ?
            dataSourceContainer.getRegistry().getProject() :
            DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        try {
            final IFolder rootFolder = ScriptsHandlerImpl.getScriptsFolder(project, false);
            List<IFile> scriptFiles = new ArrayList<>();
            ScriptsHandlerImpl.findScriptsByDataSource(rootFolder, dataSourceContainer, scriptFiles);
            if (scriptFiles.isEmpty()) {
                // Create new script
                final IFile newScript = ScriptsHandlerImpl.createNewScript(project, rootFolder, dataSourceContainer);
                NavigatorHandlerObjectOpen.openResource(newScript, workbenchWindow);
            } else {
                // Show script chooser
                ScriptSelectorShell selector = new ScriptSelectorShell(HandlerUtil.getActiveShell(event), scriptFiles);
                selector.show();
            }
/*
            scriptFile = ScriptsHandlerImpl.findRecentScript(project, dataSourceContainer);
            if (scriptFile == null) {
                scriptFile = ScriptsHandlerImpl.createNewScript(project, scriptFolder, dataSourceContainer);
            }
            NavigatorHandlerObjectOpen.openResource(scriptFile, workbenchWindow);
*/
        }
        catch (CoreException e) {
            log.error(e);
        }


        return null;
    }

    private static class ScriptSelectorShell {

        private final Shell popup;
        private final List<IFile> scriptFiles;
        private final Text patternText;
        private final Tree scriptTable;

        public ScriptSelectorShell(Shell parent, List<IFile> scriptFiles) {
            this.scriptFiles = scriptFiles;

            final Color bg = parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);

            popup = new Shell(parent, SWT.RESIZE | SWT.NO_TRIM);
            popup.setLayout(new FillLayout());
            popup.setBounds(100, 100, 400, 200);

            Composite composite = new Composite(popup, SWT.BORDER);

            composite.setLayout(new GridLayout(1, false));
            composite.setBackground(bg);

            patternText = new Text(composite, SWT.NONE);
            patternText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            patternText.setBackground(bg);

            UIUtils.createHorizontalLine(composite);

            scriptTable = new Tree(composite, SWT.NONE);
            scriptTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            scriptTable.setBackground(bg);
            //scriptTable.setHeaderVisible(true);
            UIUtils.createTreeColumn(scriptTable, SWT.LEFT, "Script");
            UIUtils.createTreeColumn(scriptTable, SWT.LEFT, "Info");

            for (IFile scriptFile : scriptFiles) {
                final TreeItem item = new TreeItem(scriptTable, SWT.NONE);
                item.setImage(DBeaverIcons.getImage(UIIcon.SQL_SCRIPT));
                item.setText(0, scriptFile.getName());
                item.setText(1, "");
            }


            final Listener focusFilter = new Listener() {
                public void handleEvent(Event event)
                {
                    if (event.widget != scriptTable && event.widget != patternText) {
                        popup.dispose();
                    }
                }
            };

            popup.getDisplay().addFilter(SWT.FocusIn, focusFilter);
            popup.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    popup.getDisplay().removeFilter(SWT.FocusIn, focusFilter);
                }
            });
        }

        void show() {
            // Fill script list
            popup.layout();
            popup.setVisible(true);
            UIUtils.packColumns(scriptTable, true, null);

            patternText.setFocus();
        }
    }

}