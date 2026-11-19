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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectSelection;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIStandardLabel;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Checkable, lazily loaded catalog/schema/table/view hierarchy, independent of navigator display settings.
 */
public class DatabaseObjectSelectionTree extends FilteredTree {
    private final DBSObjectContainer root;
    private final DBRRunnableContext runnableContext;
    private final DatabaseObjectSelectionTreeContentProvider contentProvider;
    private final DBSObjectSelection selection;
    private final List<Runnable> checkListeners = new ArrayList<>();

    public DatabaseObjectSelectionTree(
        @NotNull Composite parent,
        @NotNull DBRRunnableContext runnableContext,
        @NotNull DBSObjectContainer root
    ) {
        super(parent, SWT.MULTI | SWT.BORDER | SWT.CHECK, new EntityPatternFilter(), true, true);
        this.root = root;
        this.runnableContext = runnableContext;
        ((GridLayout) getLayout()).verticalSpacing = 0;
        CheckboxTreeViewer viewer = getCheckboxViewer();
        TreeColumnLayout columnLayout = new TreeColumnLayout();
        viewer.getTree().getParent().setLayout(columnLayout);
        viewer.getTree().setHeaderVisible(false);
        TreeViewerColumn nameColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        columnLayout.setColumnData(nameColumn.getColumn(), new ColumnWeightData(70, 0, false));
        nameColumn.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return element instanceof DBSObject object ? object.getName() :
                    UINavigatorMessages.ui_navigator_loading_text_loading;
            }

            @Nullable
            @Override
            public Image getImage(@NotNull Object element) {
                return element instanceof DBSObject object ?
                    DBeaverIcons.getImage(DBValueFormatting.getObjectImage(object)) : null;
            }
        });
        TreeViewerColumn descriptionColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        columnLayout.setColumnData(descriptionColumn.getColumn(), new ColumnWeightData(30, 0, false));
        descriptionColumn.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                if (element instanceof DBSObject object && object.getDescription() != null) {
                    return object.getDescription().replace('\r', ' ').replace('\n', ' ');
                }
                return "";
            }

            @NotNull
            @Override
            public Color getForeground(@NotNull Object element) {
                return getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
            }
        });
        contentProvider = new DatabaseObjectSelectionTreeContentProvider(viewer, root, () -> {
            String pattern = getFilterString();
            if (pattern != null && !pattern.isEmpty() && !pattern.equals(getInitialText())) {
                textChanged();
            }
        });
        viewer.setContentProvider(contentProvider);
        selection = new DBSObjectSelection(contentProvider::getParent, contentProvider::getCachedChildren);
        viewer.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isChecked(@NotNull Object element) {
                return element instanceof DBSObject object && selection.isChecked(object);
            }

            @Override
            public boolean isGrayed(@NotNull Object element) {
                return element instanceof DBSObject object && selection.isGrayed(object);
            }
        });
        viewer.addCheckStateListener(event -> {
            if (event.getElement() instanceof DBSObject object) {
                selection.setChecked(object, event.getChecked());
            }
            refreshChecks();
        });
        viewer.setInput(root);
        viewer.addDoubleClickListener(event -> {
            Object element = viewer.getStructuredSelection().getFirstElement();
            if (element != null && contentProvider.hasChildren(element)) {
                viewer.setExpandedState(element, !viewer.getExpandedState(element));
            }
        });
        createContextMenu();
        UIUtils.addDefaultEditActionsSupport(UIUtils.getActiveWorkbenchWindow(), getFilterControl());
    }

    @NotNull
    @Override
    protected TreeViewer doCreateTreeViewer(@NotNull Composite parent, int style) {
        return new CheckboxTreeViewer(parent, style);
    }

    @NotNull
    private CheckboxTreeViewer getCheckboxViewer() {
        return (CheckboxTreeViewer) getViewer();
    }

    public void addCheckListener(@NotNull Runnable listener) {
        checkListeners.add(listener);
    }

    public void setCheckedObjects(@NotNull List<? extends DBSObject> objects) {
        selection.clear();
        List<? extends DBSObject> supported = objects.stream()
            .filter(DatabaseObjectSelectionTreeContentProvider::isVisibleObject).toList();
        supported.forEach(object -> selection.setChecked(object, true));
        setShowSystemObjects(supported.stream().anyMatch(DatabaseObjectSelectionTreeContentProvider::isSystemObject));
        refreshChecks();
    }

    public boolean isShowSystemObjects() {
        return ((EntityPatternFilter) getPatternFilter()).showSystemObjects;
    }

    public void setShowSystemObjects(boolean show) {
        ((EntityPatternFilter) getPatternFilter()).showSystemObjects = show;
        getViewer().refresh();
    }

    public boolean hasCheckedObjects() {
        return !selection.getSelectedObjects().isEmpty();
    }

    @NotNull
    public List<DBSObject> getCheckedObjects() {
        return selection.getSelectedObjects();
    }

    /**
     * Expands only this object's path; checking saved objects never expands their branches.
     */
    public void expandObject(@NotNull DBSObject object) throws InvocationTargetException, InterruptedException {
        List<DBSObject> path = new ArrayList<>();
        for (DBSObject current = object; current != null && current != root; current = contentProvider.getParent(current)) {
            path.addFirst(current);
        }
        runnableContext.run(true, true, monitor -> {
            try {
                contentProvider.loadChildren(monitor, root);
                for (DBSObject parent : path) {
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                    if (parent instanceof DBSObjectContainer container) {
                        contentProvider.loadChildren(monitor, container);
                    }
                }
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        });
        getViewer().refresh();
        for (DBSObject parent : path) {
            getViewer().setExpandedState(parent, true);
        }
        if (!path.isEmpty()) {
            getViewer().setSelection(new StructuredSelection(path.getLast()), true);
        }
    }

    private void refreshChecks() {
        getViewer().refresh();
        checkListeners.forEach(Runnable::run);
    }

    private void createContextMenu() {
        MenuManager menu = new MenuManager();
        menu.setRemoveAllWhenShown(true);
        menu.addMenuListener(manager -> {
            Action copy = new Action(UIStandardLabel.COPY.getLabel()) {
                @Override
                public void run() {
                    copySelection();
                }
            };
            copy.setEnabled(Arrays.stream(getViewer().getStructuredSelection().toArray()).anyMatch(DBSObject.class::isInstance));
            manager.add(copy);
            DBSObjectContainer context = getContextContainer();
            Action selectAll = new Action(WorkbenchMessages.CheckedTreeSelectionDialog_select_all) {
                @Override
                public void run() {
                    if (context != null) {
                        setContextChecked(context, true);
                    }
                }
            };
            selectAll.setEnabled(context != null);
            manager.add(selectAll);
            Action deselectAll = new Action(WorkbenchMessages.CheckedTreeSelectionDialog_deselect_all) {
                @Override
                public void run() {
                    if (context != null) {
                        setContextChecked(context, false);
                    }
                }
            };
            deselectAll.setEnabled(context != null);
            manager.add(deselectAll);
        });
        getViewer().getControl().setMenu(menu.createContextMenu(getViewer().getControl()));
        addDisposeListener(event -> menu.dispose());
        getViewer().getTree().addListener(SWT.KeyDown, event -> {
            if ((event.stateMask & SWT.MOD1) != 0 && (event.keyCode == 'c' || event.keyCode == 'C')) {
                copySelection();
                event.doit = false;
            }
        });
    }

    @Nullable
    private DBSObjectContainer getContextContainer() {
        if (!(getViewer().getStructuredSelection().getFirstElement() instanceof DBSObject object)) {
            return null;
        }
        for (DBSObject current = object; current != null; current = contentProvider.getParent(current)) {
            if (current instanceof DBSSchema || current instanceof DBSCatalog) {
                return (DBSObjectContainer) current;
            }
        }
        return root;
    }

    private void setContextChecked(@NotNull DBSObjectContainer context, boolean checked) {
        EntityPatternFilter filter = new EntityPatternFilter();
        filter.showSystemObjects = isShowSystemObjects();
        String pattern = getFilterString();
        filter.setPattern(pattern != null && !pattern.equals(getInitialText()) ? pattern : null);
        if (!checked && !filter.filtering) {
            selection.setChecked(context, false);
            refreshChecks();
            return;
        }
        List<DBSObject> objects = new ArrayList<>();
        try {
            runnableContext.run(true, true, monitor -> {
                try {
                    collectMatchingObjects(monitor, context, filter, objects);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(UIStandardLabel.ERROR.getLabel(), null, e.getTargetException());
            return;
        } catch (InterruptedException e) {
            return;
        }
        objects.forEach(object -> selection.setChecked(object, checked));
        refreshChecks();
    }

    private void collectMatchingObjects(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer container,
        @NotNull EntityPatternFilter filter,
        @NotNull List<DBSObject> result
    ) throws DBException, InterruptedException {
        for (DBSObject object : contentProvider.loadChildren(monitor, container)) {
            if (monitor.isCanceled()) {
                throw new InterruptedException();
            }
            if (!filter.showSystemObjects && DatabaseObjectSelectionTreeContentProvider.isSystemObject(object)) {
                continue;
            }
            if (!filter.filtering || object instanceof DBSEntity entity && filter.matches(entity.getName())) {
                result.add(object);
            } else if (object instanceof DBSObjectContainer childContainer) {
                collectMatchingObjects(monitor, childContainer, filter, result);
            }
        }
    }

    private void copySelection() {
        String text = Arrays.stream(getViewer().getStructuredSelection().toArray())
            .filter(DBSObject.class::isInstance)
            .map(DBSObject.class::cast)
            .map(DBSObject::getName)
            .collect(Collectors.joining(System.lineSeparator()));
        if (!text.isEmpty()) {
            UIUtils.setClipboardContents(getDisplay(), TextTransfer.getInstance(), text);
        }
    }

    private static class EntityPatternFilter extends PatternFilter {
        private boolean showSystemObjects;
        private boolean filtering;

        EntityPatternFilter() {
            setIncludeLeadingWildcard(true);
            // The system-object predicate must also run when the name pattern is empty.
            super.setPattern("org.eclipse.ui.keys.optimization.false");
        }

        @Override
        public void setPattern(@Nullable String pattern) {
            filtering = pattern != null && !pattern.isEmpty();
            super.setPattern(pattern);
        }

        boolean matches(@NotNull String name) {
            return wordMatches(name);
        }

        @Override
        public boolean isElementVisible(@NotNull Viewer viewer, @NotNull Object element) {
            if (!showSystemObjects && element instanceof DBSObject object &&
                DatabaseObjectSelectionTreeContentProvider.isSystemObject(object)) {
                return false;
            }
            return !filtering || super.isElementVisible(viewer, element);
        }

        @Override
        protected boolean isLeafMatch(@NotNull Viewer viewer, @NotNull Object element) {
            return element instanceof DatabaseObjectSelectionTreeContentProvider.LoadingObject ||
                element instanceof DBSEntity entity && wordMatches(entity.getName());
        }
    }
}
