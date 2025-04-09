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
package org.jkiss.dbeaver.ui.navigator.dialogs;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNNodeReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.ui.navigator.itemlist.DatabaseObjectListControl;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ObjectListDialog
 *
 * @author Serge Rider
 */
public class ObjectListDialog<T extends DBPObject> extends AbstractPopupPanel {

    private static final String DIALOG_ID = "DBeaver.SelectDatabaseObjectDialog";//$NON-NLS-1$

    private String listId;
    private boolean singleSelection;

    protected List<T> objects;
    protected List<T> selectedObjects = new ArrayList<>();
    protected DatabaseObjectListControl<T> objectList;

    public ObjectListDialog(Shell parentShell, String title, boolean singleSelection, String listId, Collection<T> objects, Collection<T> selected)
    {
        super(parentShell, title);
        this.singleSelection = singleSelection;
        this.listId = listId;
        this.objects = new ArrayList<>(objects);
        if (selected != null) {
            selectedObjects.addAll(selected);
        }
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return null;//UIUtils.getDialogSettings(DIALOG_ID + "." + listId);
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite group = super.createDialogArea(parent);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        createUpperControls(group);

        objectList = createObjectSelector(group, singleSelection, listId, selectedObjects, false, new DBRRunnableWithResult<>() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException {
                try {
                    result = getObjects(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
        objectList.createProgressPanel();
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 300;
        gd.minimumWidth = 500;
        objectList.setLayoutData(gd);
        objectList.getSelectionProvider().addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            selectedObjects.clear();
            selectedObjects.addAll(selection.toList());
            if (!isModeless()) {
                getButton(IDialogConstants.OK_ID).setEnabled(!selectedObjects.isEmpty());
            }
        });
        objectList.setDoubleClickHandler(event -> {
            if (isModeless() || getButton(IDialogConstants.OK_ID).isEnabled()) {
                okPressed();
            }
        });

        objectList.loadData();

        closeOnFocusLost(objectList.getItemsViewer().getControl(), objectList.getSearchTextControl());

        return group;
    }

    @NotNull
    protected static <T extends DBPObject> DatabaseObjectListControl<T> createObjectSelector(
        Composite group,
        boolean singleSelection,
        String listId,
        List<T> selectedObjects,
        DBRRunnableWithResult<List<T>> objectReader
    ) {
        return createObjectSelector(group, singleSelection, listId, selectedObjects, true, objectReader);
    }
    
    @NotNull
    private static <T extends DBPObject> DatabaseObjectListControl<T> createObjectSelector(
        Composite group,
        boolean singleSelection,
        String listId,
        List<T> selectedObjects,
        boolean isSetFocusAfterLoad,
        DBRRunnableWithResult<List<T>> objectReader
    ) {
        return new DialogObjectListControl<>(group, singleSelection, listId, objectReader, isSetFocusAfterLoad, selectedObjects);
    }

    protected List<T> getObjects(DBRProgressMonitor monitor) throws DBException {
        return objects;
    }

    protected void createUpperControls(Composite composite) {

    }

    @Override
    public int open() {
        return super.open();
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control ctl = super.createContents(parent);
        if (!isModeless()) {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
        return ctl;
    }

    public List<T> getSelectedObjects()
    {
        return selectedObjects;
    }

    public T getSelectedObject()
    {
        return selectedObjects.isEmpty() ? null : selectedObjects.get(0);
    }

    public static <T extends DBPObject> T selectObject(Shell parentShell, String title, String listId, Collection<T> objects)
    {
        ObjectListDialog<T> scDialog = new ObjectListDialog<>(parentShell, title, true, listId, objects, null);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            final List<T> selectedObjects = scDialog.getSelectedObjects();
            return CommonUtils.isEmpty(selectedObjects) ? null : selectedObjects.get(0);
        } else {
            return null;
        }
    }

    private static class DialogObjectListControl<T extends DBPObject> extends DatabaseObjectListControl<T> implements DBNNodeReference {
        private final Composite group;
        private final String listId;
        private final DBRRunnableWithResult<List<T>> objectReader;
        private final boolean isSetFocusAfterLoad;
        private final List<T> selectedObjects;
        private Font boldFont;
        private final ISearchExecutor searcher;

        public DialogObjectListControl(
            Composite group,
            boolean singleSelection,
            String listId,
            DBRRunnableWithResult<List<T>> objectReader,
            boolean isSetFocusAfterLoad,
            List<T> selectedObjects
        ) {
            super(group, (singleSelection ? SWT.SINGLE : SWT.MULTI), null, new ListContentProvider());
            this.group = group;
            this.listId = listId;
            this.objectReader = objectReader;
            this.isSetFocusAfterLoad = isSetFocusAfterLoad;
            this.selectedObjects = selectedObjects;
            searcher = new SearcherFilter();
        }

        @NotNull
        @Override
        protected String getListConfigId(List<Class<?>> classList) {
            return listId;
        }

        @Override
        protected LoadingJob<Collection<T>> createLoadService(boolean forUpdate) {
            return LoadingJob.createService(
                new AbstractLoadService<>() {
                    @Override
                    public Collection<T> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        objectReader.run(monitor);
                        return objectReader.getResult();
                    }

                    @Override
                    public Object getFamily() {
                        return ObjectListDialog.class;
                    }
                },
                new ObjectsLoadVisualizer() {
                    @Override
                    public void completeLoading(Collection<T> items) {
                        super.completeLoading(items);
                        performSearch(SearchType.NONE, false);
                        if (isSetFocusAfterLoad) {
                            getItemsViewer().getControl().setFocus();
                        }
                    }
                });
        }

        protected CellLabelProvider getColumnLabelProvider(ObjectColumn objectColumn) {
            return new ObjectLabelProvider(objectColumn);
        }

        @Override
        protected Object getObjectValue(T item) {
            if (item instanceof DBSWrapper) {
                return ((DBSWrapper) item).getObject();
            }
            return super.getObjectValue(item);
        }

        @Override
        protected DBPImage getObjectImage(T item) {
            if (item instanceof DBNNode node) {
                return node.getNodeIcon();
            } else if (item instanceof DBPImageProvider imageProvider) {
                return imageProvider.getObjectImage();
            }
            return null;
        }

        @Override
        protected void setListData(Collection<T> items, boolean append, boolean forUpdate) {
            super.setListData(items, append, forUpdate);
            if (selectedObjects != null) {
                getItemsViewer().setSelection(new StructuredSelection(selectedObjects), true);
            }
        }

        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);
            addColumnConfigAction(contributionManager);
        }

        protected void addSearchAction(IContributionManager contributionManager) {
            contributionManager.add(new Action("Filter objects", DBeaverIcons.getImageDescriptor(UIIcon.SEARCH)) {
                @Override
                public void run() {
                    performSearch(SearchType.NONE);
                }
            });
        }

        @Override
        protected ISearchExecutor getSearchRunner() {
            return searcher;
        }

        @Override
        public DBNNode getReferencedNode() {
            return selectedObjects.isEmpty() ? null : selectedObjects.get(0) instanceof DBNNode node ? node : null;
        }

        class ObjectLabelProvider extends ObjectColumnLabelProvider implements IFontProvider {
            ObjectLabelProvider(ObjectColumn objectColumn) {
                super(objectColumn);
            }

            @Override
            public Font getFont(Object element) {
                if (selectedObjects.contains(element)) {
                    if (boldFont == null) {
                        boldFont = UIUtils.makeBoldFont(group.getFont());
                        group.addDisposeListener(e -> boldFont.dispose());
                    }
                    return boldFont;
                }
                return null;
            }
        }
    }
}
