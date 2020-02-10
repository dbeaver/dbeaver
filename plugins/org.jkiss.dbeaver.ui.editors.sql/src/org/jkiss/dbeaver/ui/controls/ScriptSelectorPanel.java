/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils.ResourceInfo;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Script selector panel (shell)
 */
public class ScriptSelectorPanel extends AbstractPopupPanel {

    private static final Log log = Log.getLog(ScriptSelectorPanel.class);

    private static final String DIALOG_ID = "DBeaver.ScriptSelectorPopup";

    private final IWorkbenchWindow workbenchWindow;
    @NotNull
    private final SQLNavigatorContext navigatorContext;
    @NotNull
    private final IFolder rootFolder;
    @NotNull
    private final List<ResourceInfo> scriptFiles;

    private Text patternText;
    private TreeViewer scriptViewer;

    private volatile FilterJob filterJob;

    private ScriptSelectorPanel(
        @NotNull final IWorkbenchWindow workbenchWindow,
        @NotNull final SQLNavigatorContext navigatorContext,
        @NotNull final IFolder rootFolder,
        @NotNull List<ResourceInfo> scriptFiles) {
        super(workbenchWindow.getShell(),
            navigatorContext.getDataSourceContainer() == null ?
                "Choose SQL script" :
                "Choose SQL script for '" + navigatorContext.getDataSourceContainer().getName() + "'");

        this.workbenchWindow = workbenchWindow;
        this.navigatorContext = navigatorContext;
        this.rootFolder = rootFolder;
        this.scriptFiles = scriptFiles;
    }

    @Override
    protected boolean isShowTitle() {
        return true;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        /*Rectangle bounds = new Rectangle(100, 100, 500, 200);
        final String boundsStr = getBoundsSettings().get(CONFIG_BOUNDS_PARAM);
        if (boundsStr != null && !boundsStr.isEmpty()) {
            final String[] bc = boundsStr.split(",");
            try {
                bounds = new Rectangle(
                    Integer.parseInt(bc[0]),
                    Integer.parseInt(bc[1]),
                    Integer.parseInt(bc[2]),
                    Integer.parseInt(bc[3]));
            } catch (NumberFormatException e) {
                log.warn(e);
            }
        }*/

        patternText = new Text(composite, SWT.BORDER);
        patternText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.addEmptyTextHint(patternText, text -> "Enter a part of script name here");
        //patternText.setForeground(fg);
        //patternText.setBackground(bg);
        patternText.addModifyListener(e -> {
            if (filterJob != null) {
                return;
            }
            filterJob = new FilterJob();
            filterJob.schedule(250);
        });
        final Color fg = patternText.getForeground();//parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);
        final Color bg = patternText.getBackground();//parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);

        composite.setForeground(fg);
        composite.setBackground(bg);

        Button newButton = new Button(composite, SWT.PUSH | SWT.FLAT);
        newButton.setText("&New Script");
        newButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IFile scriptFile;
                try {
                    scriptFile = SQLEditorUtils.createNewScript(
                        DBWorkbench.getPlatform().getWorkspace().getProject(rootFolder.getProject()),
                        rootFolder,
                        navigatorContext);
                    SQLEditorHandlerOpenEditor.openResource(scriptFile, navigatorContext);
                } catch (CoreException ex) {
                    log.error(ex);
                }
                cancelPressed();
            }
        });

        ((GridData) UIUtils.createHorizontalLine(composite).getLayoutData()).horizontalSpan = 2;

        Tree scriptTree = new Tree(composite, SWT.SINGLE | SWT.FULL_SELECTION);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        gd.widthHint = 500;
        gd.heightHint = 200;
        scriptTree.setLayoutData(gd);
        scriptTree.setForeground(fg);
        scriptTree.setBackground(bg);
        scriptTree.setLinesVisible(true);
        //scriptViewer.setHeaderVisible(true);

        this.scriptViewer = new TreeViewer(scriptTree);
        ColumnViewerToolTipSupport.enableFor(this.scriptViewer);
        //scriptTree.setS
        this.scriptViewer.setContentProvider(new TreeContentProvider() {
            @Override
            public Object[] getChildren(Object parentElement) {
                if (parentElement instanceof ResourceInfo) {
                    final List<ResourceInfo> children = ((ResourceInfo) parentElement).getChildren();
                    return CommonUtils.isEmpty(children) ? null : children.toArray();
                }
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                if (element instanceof ResourceInfo) {
                    final List<ResourceInfo> children = ((ResourceInfo) element).getChildren();
                    return !CommonUtils.isEmpty(children);
                }
                return false;
            }

        });
        ViewerColumnController columnController = new ViewerColumnController("scriptSelectorViewer", scriptViewer);
        columnController.addColumn("Script", "Resource name", SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public Image getImage(Object element) {
                final ResourceInfo ri = (ResourceInfo) element;
                if (!ri.isDirectory()) {
                    if (ri.getDataSource() == null) {
                        return DBeaverIcons.getImage(UIIcon.SQL_SCRIPT);
                    } else {
                        return DBeaverIcons.getImage(ri.getDataSource().getDriver().getIcon());
                    }
                } else {
                    return DBeaverIcons.getImage(DBIcon.TREE_FOLDER);
                }
            }

            @Override
            public String getText(Object element) {
                return ((ResourceInfo) element).getName();
            }

            @Override
            public String getToolTipText(Object element) {
                final DBPDataSourceContainer dataSource = ((ResourceInfo) element).getDataSource();
                return dataSource == null ? null : dataSource.getName();
            }

            @Override
            public Image getToolTipImage(Object element) {
                final DBPDataSourceContainer dataSource = ((ResourceInfo) element).getDataSource();
                return dataSource == null ? null : DBeaverIcons.getImage(dataSource.getDriver().getIcon());
            }
        });
        columnController.addColumn("Time", "Modification time", SWT.LEFT, true, true, new ColumnLabelProvider() {
            private SimpleDateFormat sdf = new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT);

            @Override
            public String getText(Object element) {
                final File localFile = ((ResourceInfo) element).getLocalFile();
                if (localFile.isDirectory()) {
                    return null;
                } else {
                    return sdf.format(new Date(localFile.lastModified()));
                }
            }
        });
        columnController.addColumn("Info", "Script preview", SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return "";//((ResourceInfo)element).getDescription();
            }

            @Override
            public Color getForeground(Object element) {
                return getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
            }

            @Override
            public String getToolTipText(Object element) {
                final ResourceInfo ri = (ResourceInfo) element;
                String description = ri.getDescription();
                return description == null ? null : description.trim();
            }
        });
        columnController.createColumns();
        columnController.sortByColumn(1, SWT.UP);

        scriptTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                List<ResourceInfo> files = new ArrayList<>();
                for (Object item : ((IStructuredSelection)scriptViewer.getSelection()).toArray()) {
                    if (!((ResourceInfo)item).isDirectory()) {
                        files.add((ResourceInfo) item);
                    }
                }
                if (files.isEmpty()) {
                    return;
                }
                cancelPressed();
                for (ResourceInfo ri : files) {
                    SQLEditorHandlerOpenEditor.openResourceEditor(ScriptSelectorPanel.this.workbenchWindow, ri, navigatorContext);
                }
            }
        });

        scriptTree.addListener(SWT.PaintItem, event -> {
            final TreeItem item = (TreeItem) event.item;
            final ResourceInfo ri = (ResourceInfo) item.getData();
            if (ri != null && !ri.isDirectory() && CommonUtils.isEmpty(item.getText(2))) {
                UIUtils.asyncExec(() -> {
                    if (!item.isDisposed()) {
                        item.setText(2, CommonUtils.getSingleLineString(CommonUtils.notEmpty(ri.getDescription())));
                    }
                });
            }
        });
        this.patternText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                final Tree tree = scriptViewer.getTree();
                if (e.keyCode == SWT.ARROW_DOWN) {
                    //scriptViewer.get
                    scriptViewer.setSelection(new StructuredSelection(tree.getItem(0).getData()));
                    tree.setFocus();
                } else if (e.keyCode == SWT.ARROW_UP) {
                    scriptViewer.setSelection(new StructuredSelection(tree.getItem(tree.getItemCount() - 1).getData()));
                    tree.setFocus();
                }
            }
        });

        closeOnFocusLost(patternText, scriptViewer.getTree(), newButton);

        scriptViewer.setInput(scriptFiles);
        UIUtils.expandAll(scriptViewer);

        final Tree tree = scriptViewer.getTree();
        final TreeColumn[] columns = tree.getColumns();
        columns[0].pack();
        columns[0].setWidth(columns[0].getWidth() + 10);
        columns[1].pack();
        columns[2].setWidth(200 * 8);

        scriptTree.setFocus();

        return composite;
    }

    protected void createButtonsForButtonBar(Composite parent)
    {
        // No buttons
    }

    public static void showTree(IWorkbenchWindow workbenchWindow, SQLNavigatorContext editorContext, IFolder rootFolder, List<ResourceInfo> scriptFiles) {
        ScriptSelectorPanel selectorPanel = new ScriptSelectorPanel(workbenchWindow, editorContext, rootFolder, scriptFiles);
        selectorPanel.setModeless(true);
        selectorPanel.open();
    }

    private class ScriptFilter extends ViewerFilter {
        private final String pattern;

        ScriptFilter() {
            pattern = patternText.getText().toLowerCase(Locale.ENGLISH);
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            final IResource resource = ((ResourceInfo) element).getResource();
            if (resource instanceof IFolder) {
                return isAnyChildMatches((ResourceInfo) element);
            } else {
                return ((ResourceInfo) element).getName().toLowerCase(Locale.ENGLISH).contains(pattern);
            }
        }

        private boolean isAnyChildMatches(ResourceInfo ri) {
            for (ResourceInfo child : ri.getChildren()) {
                if (child.getResource() instanceof IFolder) {
                    if (isAnyChildMatches(child)) {
                        return true;
                    }
                } else {
                    if (child.getName().toLowerCase(Locale.ENGLISH).contains(pattern)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private class FilterJob extends UIJob {
        FilterJob() {
            super("Filter scripts");
        }
        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            filterJob = null;
            scriptViewer.setFilters(new ViewerFilter[] { new ScriptFilter() });
            UIUtils.expandAll(scriptViewer);
            final Tree tree = scriptViewer.getTree();
            if (tree.getItemCount() > 0) {
                scriptViewer.reveal(tree.getItem(0).getData());
            }
            return Status.OK_STATUS;
        }
    }
}
