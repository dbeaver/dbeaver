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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ViewerColumnController;
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

    static final Log log = Log.getLog(ScriptSelectorPanel.class);

    private final IWorkbenchWindow workbenchWindow;
    private final Shell popup;
    private final Text patternText;
    private final TreeViewer scriptViewer;
    private final Button newButton;
    private List<ResourceInfo> rawFiles;
    private List<ResourceInfo> input;
    private final ViewerColumnController columnController;

    public ScriptSelectorPanel(final IWorkbenchWindow workbenchWindow, final DBPDataSourceContainer dataSourceContainer, final IFolder rootFolder) {
        this.workbenchWindow = workbenchWindow;
        Shell parent = this.workbenchWindow.getShell();

        final Color bg = parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);

        popup = new Shell(parent, SWT.RESIZE);
        popup.setLayout(new FillLayout());
        popup.setBounds(100, 100, 500, 200);

        Composite composite = new Composite(popup, SWT.NONE);

        final GridLayout gl = new GridLayout(2, false);
        //gl.marginHeight = 0;
        //gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setBackground(bg);

        patternText = new Text(composite, SWT.NONE);
        patternText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        patternText.setBackground(bg);
        patternText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                scriptViewer.setFilters(new ViewerFilter[] { new PatternFilter() });
            }
        });

        newButton = new Button(composite, SWT.PUSH | SWT.FLAT);
        newButton.setText("&New Script");
        newButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                popup.dispose();
                IFile scriptFile;
                try {
                    scriptFile = ResourceUtils.createNewScript(rootFolder.getProject(), rootFolder, dataSourceContainer);
                    NavigatorHandlerObjectOpen.openResource(scriptFile, workbenchWindow);
                } catch (CoreException ex) {
                    log.error(ex);
                }
            }
        });

        ((GridData) UIUtils.createHorizontalLine(composite).getLayoutData()).horizontalSpan = 2;

        Tree scriptTree = new Tree(composite, SWT.MULTI | SWT.FULL_SELECTION);
        final GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        scriptTree.setLayoutData(gd);
        scriptTree.setBackground(bg);
        scriptTree.setLinesVisible(true);
        //scriptViewer.setHeaderVisible(true);

        this.scriptViewer = new TreeViewer(scriptTree);
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
        columnController = new ViewerColumnController("scriptSelectorViewer", scriptViewer);
        columnController.addColumn("Script", "Resource name", SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public Image getImage(Object element) {
                if (element instanceof ResourceInfo && ((ResourceInfo) element).getResource() instanceof IFile) {
                    return DBeaverIcons.getImage(UIIcon.SQL_SCRIPT);
                } else {
                    return DBeaverIcons.getImage(DBIcon.TREE_FOLDER);
                }
            }

            @Override
            public String getText(Object element) {
                return ((ResourceInfo) element).getResource().getName();
            }
        });
        columnController.addColumn("Time", "Modification time", SWT.LEFT, true, true, new ColumnLabelProvider() {
            private SimpleDateFormat sdf = new SimpleDateFormat(UIUtils.DEFAULT_TIMESTAMP_PATTERN);

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
                final File localFile = ((ResourceInfo) element).getLocalFile();
                if (localFile.isDirectory()) {
                    return null;
                } else {
                    String desc = SQLUtils.getScriptDescription((IFile) ((ResourceInfo) element).getResource());
                    if (CommonUtils.isEmptyTrimmed(desc)) {
                        desc = "<empty>";
                    }
                    return desc;
                }
            }
        });
        columnController.createColumns();

        scriptTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                List<IFile> files = new ArrayList<>();
                for (Object item : ((IStructuredSelection)scriptViewer.getSelection()).toArray()) {
                    if (((ResourceInfo)item).getResource() instanceof IFile) {
                        files.add((IFile) ((ResourceInfo)item).getResource());
                    }
                }
                if (files.isEmpty()) {
                    return;
                }
                popup.dispose();
                for (IFile file : files) {
                    NavigatorHandlerObjectOpen.openResource(
                        file,
                        ScriptSelectorPanel.this.workbenchWindow);
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
                popup.getDisplay().removeFilter(SWT.FocusIn, focusFilter);
            }
        });
    }

    public void showTree(List<ResourceInfo> scriptFiles) {
        // Fill script list
        popup.layout();
        popup.setVisible(true);

        loadScriptTree(scriptFiles);

        final Tree tree = scriptViewer.getTree();
        final int totalWidth = tree.getSize().x;
        final TreeColumn[] columns = tree.getColumns();
        columns[0].pack();
        columns[1].pack();
        columns[2].pack();
        if (columns[0].getWidth() + columns[1].getWidth() + columns[2].getWidth() < totalWidth) {
            columns[2].setWidth(totalWidth - columns[0].getWidth() - columns[1].getWidth());
        }
        columnController.sortByColumn(1, SWT.UP);

        patternText.setFocus();
    }

    private void loadScriptTree(List<ResourceInfo> scriptFiles) {
//            scriptFiles = new ArrayList<>(scriptFiles);
//            Collections.sort(scriptFiles, new Comparator<IFile>() {
//                @Override
//                public int compare(IFile o1, IFile o2) {
//                    return (int)(o2.getLocation().toFile().lastModified() / 1000 - o1.getLocation().toFile().lastModified() / 1000);
//                }
//            });
        this.rawFiles = new ArrayList<>(scriptFiles);
        this.input = new ArrayList<>(rawFiles);

        scriptViewer.setInput(this.input);
        scriptViewer.expandAll();
/*

        scriptViewer.removeAll();

        for (ResourceInfo file : rawFiles) {
            createFileItem(null, file);
        }
*/
    }

    private class PatternFilter extends ViewerFilter {
        private final String pattern;

        public PatternFilter() {
            pattern = patternText.getText().toLowerCase(Locale.ENGLISH);
        }

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            final IResource resource = ((ResourceInfo) element).getResource();
            if (resource instanceof IFolder) {
                return true;
            } else {
                return resource.getName().toLowerCase(Locale.ENGLISH).contains(pattern);
            }
        }
    }

}
