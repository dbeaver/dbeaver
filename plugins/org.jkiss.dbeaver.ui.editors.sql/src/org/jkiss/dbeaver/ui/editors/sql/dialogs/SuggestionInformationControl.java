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
package org.jkiss.dbeaver.ui.editors.sql.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBInfoUtils;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.navigator.itemlist.ItemListControl;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SuggestionInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {
    private static final Log log = Log.getLog(SuggestionInformationControl.class);
    private Composite infoComposite;
    private Object input;
    private Font boldFont;
    private Composite tableComposite;
    private Composite mainComposite;
    private ItemListControl itemListControl;

    public SuggestionInformationControl(Shell parentShell, boolean isResizable) {
        super(parentShell, isResizable);
        create();
    }

    @Override
    protected void createContent(Composite parent) {
        GridData mainGridData = new GridData(GridData.FILL_BOTH);
        mainComposite = UIUtils.createPlaceholder(parent, 1);
        mainComposite.setLayoutData(mainGridData);

        GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
        this.infoComposite = UIUtils.createPlaceholder(mainComposite, 1);
        infoComposite.setLayoutData(infoGridData);

        this.tableComposite = UIUtils.createPlaceholder(mainComposite, 1);
        tableComposite.setLayoutData(mainGridData);

        final FontDescriptor fontDescriptor = FontDescriptor.createFrom(parent.getFont());

        this.boldFont = fontDescriptor.setStyle(SWT.BOLD).createFont(parent.getDisplay());

    }

    @Override
    public boolean hasContents() {
        return input != null;
    }

    @Override
    public void setInput(Object input) {
        this.input = input;
        if (input instanceof DBPNamedObject && !infoComposite.isDisposed() && !tableComposite.isDisposed()) {
            createMetadataFields((DBPNamedObject) input);
            if (input instanceof DBSTable) {
                createTreeControl((DBSTable) input);
            }
        }

    }

    private void createMetadataFields(@NotNull DBPNamedObject input) {
        GridLayout layout = new GridLayout(1, true);
        layout.marginTop = 0;
        layout.marginBottom = 5;
        layout.marginLeft = 5;
        layout.marginRight = 5;
        Composite metadataComposite = new Composite(infoComposite, SWT.NONE);
        metadataComposite.setLayout(layout);
        metadataComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final DBPNamedObject[] targetObject = {input};
        AbstractJob resolveObject = new AbstractJob("Resolving object") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                if (input instanceof DBSObjectReference) {
                    try {
                        targetObject[0] = ((DBSObjectReference) input).resolveObject(monitor);
                    } catch (DBException e) {
                        log.error("Error resolving object", e);
                        return Status.CANCEL_STATUS;
                    }
                }
                return Status.OK_STATUS;
            }
        };
        resolveObject.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                if (!event.getResult().isOK()) {
                    return;
                }
                UIUtils.syncExec(() -> {
                    if (!metadataComposite.isDisposed() && !infoComposite.isDisposed() && !mainComposite.isDisposed()) {
                        PropertyCollector collector = new PropertyCollector(targetObject[0], false);
                        collector.collectProperties();
                        for (DBPPropertyDescriptor descriptor : collector.getProperties()) {
                            String propertyString = DBInfoUtils.getPropertyString(collector, descriptor);
                            if (CommonUtils.isEmpty(propertyString) || !descriptor.hasFeature(DBConstants.PROP_FEATURE_VIEWABLE)) {
                                continue;
                            }
                            Composite placeholder = UIUtils.createPlaceholder(metadataComposite, 2);
                            placeholder.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
                            Label label = new Label(placeholder, SWT.READ_ONLY);
                            label.setText(descriptor.getDisplayName() + ":");
                            label.setFont(boldFont);
                            Text valueText = new Text(placeholder, SWT.READ_ONLY);
                            valueText.setText(propertyString);
                        }
                        infoComposite.layout(true, true);
                        mainComposite.layout(true, true);
                    }
                });
            }
        });
        resolveObject.schedule();
    }

    private void createTreeControl(@NotNull DBSTable input) {
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 0;
        gridData.verticalSpan = 0;
        IEditorSite subSite = new SubEditorSite(UIUtils.getActiveWorkbenchWindow()
            .getActivePage()
            .getActivePart()
            .getSite());
        DBNDatabaseNode node = DBWorkbench.getPlatform().getNavigatorModel().findNode(input);
        itemListControl = new ItemListControl(tableComposite, SWT.NONE, subSite, node, null) {
            @NotNull
            @Override
            protected String getListConfigId(List<Class<?>> classList) {
                return "Suggestion/" + super.getListConfigId(classList);
            }
        };
        itemListControl.setLayoutData(gridData);
        final Object[] columnNodes = new Object[1];
        AbstractJob abstractJob = new AbstractJob("Populating table tip columns") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                monitor.beginTask("load table columns", 1);
                try {
                    columnNodes[0] = getColumnNodes(monitor, node);
                } catch (DBException e) {
                    log.error("Error reading table columns", e);
                    return Status.CANCEL_STATUS;
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }
        };
        abstractJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void done(IJobChangeEvent event) {
                if (!event.getResult().isOK()) {
                    return;
                }
                UIUtils.syncExec(() -> {
                    if (itemListControl != null && !itemListControl.isDisposed()) {
                        Collection<DBNNode> columnNodeList = (Collection<DBNNode>) columnNodes[0];
                        if (CommonUtils.isEmpty(columnNodeList)) {
                            itemListControl.dispose();
                        } else {
                            itemListControl.appendListData(columnNodeList);
                            for (int i = 0; i < itemListControl.getColumnController().getColumnsCount(); i++) {
                                itemListControl.getColumnController().setIsColumnVisible(i, false);
                            }
                            itemListControl.setIsColumnVisibleById("ordinalPosition", true); //NON-NLS-1
                            itemListControl.setIsColumnVisibleById("name", true); //NON-NLS-1
                            itemListControl.setIsColumnVisibleById("fullTypeName", true); //NON-NLS-1
                            itemListControl.setIsColumnVisibleById("identity", true); //NON-NLS-1
                            itemListControl.setIsColumnVisibleById("description", true); //NON-NLS-1
                            itemListControl.getColumnController().createColumns(false);
                            itemListControl.getItemsViewer().refresh();
                            itemListControl.getColumnController().autoSizeColumns();
                        }
                        tableComposite.layout(true, true);
                    }
                });
            }
        });
        abstractJob.schedule();
    }

    @NotNull
    private Collection<DBNNode> getColumnNodes(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBNNode node
    ) throws DBException {
        if (node == null) {
            return Collections.emptyList();
        }
        List<DBNNode> children = new ArrayList<>();
        for (DBNNode child : node.getChildren(monitor)) {
            if (child instanceof DBNDatabaseFolder) {
                Class<? extends DBSObject> childrenClass = ((DBNDatabaseFolder) child).getChildrenClass();
                if (childrenClass != null && DBSTableColumn.class.isAssignableFrom(childrenClass)) {
                    if (itemListControl != null && !itemListControl.isDisposed()) {
                        itemListControl.setRootNode(child);
                    }
                    DBNNode[] folderChildren = child.getChildren(monitor);
                    children.addAll(List.of(folderChildren));
                }
            } else {
                children.add(child);
            }
        }
        return children;
    }

    @Override
    public void dispose() {
        boldFont.dispose();
        super.dispose();
    }
}
