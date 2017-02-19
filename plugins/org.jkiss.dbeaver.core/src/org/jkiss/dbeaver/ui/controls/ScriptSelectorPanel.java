/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;
import org.jkiss.dbeaver.ui.resources.ResourceUtils.ResourceInfo;
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
public class ScriptSelectorPanel {

    private static final Log log = Log.getLog(ScriptSelectorPanel.class);
    public static final String CONFIG_BOUNDS_PARAM = "bounds";

    private final IWorkbenchWindow workbenchWindow;
    private final Shell popup;
    private final Text patternText;
    private final TreeViewer scriptViewer;
    private final Button newButton;
    private volatile FilterJob filterJob;

    public ScriptSelectorPanel(@NotNull final IWorkbenchWindow workbenchWindow, @NotNull final DBPDataSourceContainer[] containers, @NotNull final IFolder rootFolder) {
        this.workbenchWindow = workbenchWindow;
        Shell parent = this.workbenchWindow.getShell();

        popup = new Shell(parent, SWT.RESIZE | SWT.TITLE | SWT.CLOSE);
        if (containers.length == 1) {
            popup.setText("Choose SQL script for '" + containers[0].getName() + "'");
            popup.setImage(DBeaverIcons.getImage(containers[0].getDriver().getIcon()));
        } else {
            popup.setText("Choose SQL script");
        }
        popup.setLayout(new FillLayout());
        Rectangle bounds = new Rectangle(100, 100, 500, 200);
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
        }
        popup.setBounds(bounds);

        Composite composite = new Composite(popup, SWT.NONE);

        final GridLayout gl = new GridLayout(2, false);
        //gl.marginHeight = 0;
        //gl.marginWidth = 0;
        composite.setLayout(gl);

        patternText = new Text(composite, SWT.NONE);
        patternText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        //patternText.setForeground(fg);
        //patternText.setBackground(bg);
        patternText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (filterJob != null) {
                    return;
                }
                filterJob = new FilterJob();
                filterJob.schedule(250);
            }
        });
        final Color fg = patternText.getForeground();//parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND);
        final Color bg = patternText.getBackground();//parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);

        composite.setForeground(fg);
        composite.setBackground(bg);

        newButton = new Button(composite, SWT.PUSH | SWT.FLAT);
        newButton.setText("&New Script");
        newButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                popup.dispose();
                IFile scriptFile;
                try {
                    scriptFile = ResourceUtils.createNewScript(rootFolder.getProject(), rootFolder, containers.length == 0 ? null : containers[0]);
                    NavigatorHandlerObjectOpen.openResource(scriptFile, workbenchWindow);
                } catch (CoreException ex) {
                    log.error(ex);
                }
            }
        });

        ((GridData) UIUtils.createHorizontalLine(composite).getLayoutData()).horizontalSpan = 2;

        Tree scriptTree = new Tree(composite, SWT.SINGLE | SWT.FULL_SELECTION);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
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
                return popup.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
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
                popup.dispose();
                for (ResourceInfo ri : files) {
                    NavigatorHandlerObjectOpen.openResourceEditor(ScriptSelectorPanel.this.workbenchWindow, ri);
                }
            }
        });

        scriptTree.addListener(SWT.PaintItem, new Listener() {
            public void handleEvent(Event event) {
                final TreeItem item = (TreeItem) event.item;
                final ResourceInfo ri = (ResourceInfo) item.getData();
                if (ri != null && !ri.isDirectory() && CommonUtils.isEmpty(item.getText(2))) {
                    DBeaverUI.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!item.isDisposed()) {
                                item.setText(2, TextUtils.getSingleLineString(CommonUtils.notEmpty(ri.getDescription())));
                            }
                        }
                    });
                }
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

        final Listener focusFilter = new Listener() {
            public void handleEvent(Event event) {
                if (event.widget != scriptViewer.getTree() && event.widget != patternText && event.widget != newButton) {
                    popup.dispose();
                }
            }
        };

        popup.getDisplay().addFilter(SWT.FocusIn, focusFilter);
        popup.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                final Rectangle bounds = popup.getBounds();
                getBoundsSettings().put(CONFIG_BOUNDS_PARAM, bounds.x + "," + bounds.y + "," + bounds.width + "," + bounds.height);
                popup.getDisplay().removeFilter(SWT.FocusIn, focusFilter);
            }
        });
    }

    IDialogSettings getBoundsSettings() {
        return UIUtils.getDialogSettings("DBeaver.ScriptSelectorPanel");
    }

    public void showTree(List<ResourceInfo> scriptFiles) {
        // Fill script list
        popup.layout();
        popup.setVisible(true);

        loadScriptTree(scriptFiles);

        final Tree tree = scriptViewer.getTree();
        final TreeColumn[] columns = tree.getColumns();
        columns[0].pack();
        columns[0].setWidth(columns[0].getWidth() + 10);
        columns[1].pack();
        columns[2].setWidth(200 * 8);

        patternText.setFocus();
    }

    private void loadScriptTree(List<ResourceInfo> scriptFiles) {
        scriptViewer.setInput(scriptFiles);
        scriptViewer.expandToLevel(2);
    }

    private class ScriptFilter extends ViewerFilter {
        private final String pattern;

        public ScriptFilter() {
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
        public FilterJob() {
            super("Filter scripts");
        }
        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            filterJob = null;
            scriptViewer.setFilters(new ViewerFilter[] { new ScriptFilter() });
            scriptViewer.expandAll();
            final Tree tree = scriptViewer.getTree();
            if (tree.getItemCount() > 0) {
                scriptViewer.reveal(tree.getItem(0).getData());
            }
            return Status.OK_STATUS;
        }
    }
}
