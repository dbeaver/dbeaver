/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.ui.controls.TableColumnSortListener;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;
import java.util.function.Predicate;

/**
 * AttributesSelectorPage
 *
 * @author Serge Rider
 */
public abstract class AttributesSelectorPage extends BaseObjectEditPage {

    protected DBSEntity entity;
    protected Table columnsTable;

    protected List<AttributeInfo> attributes = new ArrayList<>();
    protected Button toggleButton;
    protected Composite columnsGroup;

    protected static class AttributeInfo {
        DBSEntityAttribute attribute;
        int position;
        Map<String, Object> properties = new HashMap<>();

        public AttributeInfo(DBSEntityAttribute attribute)
        {
            this.attribute = attribute;
            this.position = -1;
        }

        public DBSEntityAttribute getAttribute() {
            return attribute;
        }

        public int getPosition() {
            return position;
        }

        public Object getProperty(String name) {
            return properties.get(name);
        }

        public void setProperty(String name, Object value) {
            if (value == null) {
                properties.remove(name);
            } else {
                properties.put(name, value);
            }
        }

        @Override
        public String toString() {
            return attribute.getName();
        }
    }

    public AttributesSelectorPage(
        String title,
        DBSEntity entity)
    {
        super(NLS.bind(EditorsMessages.dialog_struct_columns_select_title, title, entity.getName()));
        this.entity = entity;
    }

    protected AttributesSelectorPage() {
        super(null);
        this.entity = null;
    }

    public Map<String, Object> getAttributeProperties(DBSEntityAttribute attr) {
        for (AttributeInfo attrInfo : attributes) {
            if (attrInfo.attribute == attr) {
                return attrInfo.properties;
            }
        }
        return Collections.emptyMap();
    }

    public Object getAttributeProperty(DBSEntityAttribute attr, String propName) {
        for (AttributeInfo attrInfo : attributes) {
            if (attrInfo.attribute == attr) {
                return attrInfo.properties.get(propName);
            }
        }
        return null;
    }

    @Override
    protected Composite createPageContents(Composite parent) {
        final Composite panel = UIUtils.createPlaceholder(parent, 1);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            final Composite tableGroup = createTableNameInput(panel);

            createContentsBeforeColumns(tableGroup);
        }

        createColumnsGroup(panel);
        createContentsAfterColumns(panel);
        fillAttributes(entity);
        return panel;
    }

    protected void createColumnsGroup(Composite panel)
    {
        columnsGroup = new Composite(panel, SWT.NONE);
        columnsGroup.setLayout(new GridLayout(1, false));
        columnsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createControlLabel(columnsGroup, EditorsMessages.dialog_struct_columns_select_group_columns);

        //columnsViewer = new TableViewer(columnsGroup, SWT.BORDER | SWT.SINGLE | SWT.CHECK);
        columnsTable = new Table(columnsGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK);
        columnsTable.setHeaderVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumWidth = 300;
        gd.minimumHeight = 150;
        columnsTable.setLayoutData(gd);
        columnsTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                handleItemSelect((TableItem) e.item, true);
            }
        });

        createAttributeColumns(columnsTable);

        final CustomTableEditor tableEditor = new CustomTableEditor(columnsTable) {
            @Override
            protected Control createEditor(Table table, final int index, final TableItem item) {
                return createCellEditor(table, index, item, (AttributeInfo)item.getData());
            }
            @Override
            protected void saveEditorValue(Control control, int index, TableItem item) {
                saveCellValue(control, index, item, (AttributeInfo)item.getData());
            }
        };

        toggleButton = new Button(columnsGroup, SWT.PUSH);
        toggleButton.setText("Select All");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = 120;
        toggleButton.setLayoutData(gd);
        toggleButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem[] items = columnsTable.getItems();
                if (hasCheckedColumns()) {
                    // Clear all checked
                    for (TableItem item : items) {
                        if (item.getChecked()) {
                            item.setChecked(false);
                            handleItemSelect(item, true);
                        }
                    }
                } else {
                    // Check all
                    for (TableItem item : items) {
                        if (!item.getChecked()) {
                            item.setChecked(true);
                            handleItemSelect(item, true);
                        }
                    }
                }
            }
        });
    }

    protected void createAttributeColumns(Table columnsTable) {
        TableColumn colName = UIUtils.createTableColumn(columnsTable, SWT.NONE, EditorsMessages.dialog_struct_columns_select_column);
        colName.addListener(SWT.Selection, new TableColumnSortListener(columnsTable, 0));

        TableColumn colPosition = UIUtils.createTableColumn(columnsTable, SWT.CENTER, "#"); //$NON-NLS-1$
        colPosition.addListener(SWT.Selection, new TableColumnSortListener(columnsTable, 1));

        TableColumn colType = UIUtils.createTableColumn(columnsTable, SWT.RIGHT, EditorsMessages.dialog_struct_columns_type); //$NON-NLS-1$
        colType.addListener(SWT.Selection, new TableColumnSortListener(columnsTable, 2));
    }

    protected int fillAttributeColumns(DBSEntityAttribute attribute, AttributeInfo attributeInfo, TableItem columnItem) {
        columnItem.setText(0, attribute.getName());
        //columnItem.setText(1, String.valueOf(attribute.getOrdinalPosition()));
        columnItem.setText(2, attribute.getFullTypeName());
        return 2;
    }

    protected Control createCellEditor(Table table, int index, TableItem item, AttributeInfo data) {
/*
        final Text text = new Text(table, SWT.BORDER);
        text.setText(item.getText(index));
        text.selectAll();
        return text;
*/
        return null;
    }

    protected void saveCellValue(Control control, int index, TableItem item, AttributeInfo data) {
        //item.setText(index, control.getText());
    }

    protected void setEntity(DBSEntity entity) {
        this.entity = entity;
        this.attributes.clear();
        this.columnsTable.removeAll();
        fillAttributes(entity);
    }

    private void fillAttributes(final DBSEntity entity)
    {
        if (entity == null) {
            return;
        }
        final List<DBSEntityAttribute> attrList = new ArrayList<>();
        AbstractJob loadJob = new AbstractJob("Load entity attributes") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                monitor.beginTask("Load attributes", 1);
                try {
                    for (DBSEntityAttribute attr : CommonUtils.safeCollection(entity.getAttributes(monitor))) {
                        if (isShowHiddenAttributes() || !DBUtils.isHiddenObject(attr) || DBUtils.isRowIdAttribute(attr)) {
                            attrList.add(attr);
                            // Preload node - required later to display its icon
                            DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(monitor, attr, true);
                        }
                    }
                } catch (DBException e) {
                    return GeneralUtils.makeErrorStatus("Error loading attributes", e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }
        };
        loadJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                UIUtils.syncExec(() -> {
                    for (DBSEntityAttribute attribute : attrList) {
                        TableItem columnItem = new TableItem(columnsTable, SWT.NONE);

                        AttributeInfo col = new AttributeInfo(attribute);
                        attributes.add(col);

                        DBNDatabaseNode attributeNode = DBWorkbench.getPlatform().getNavigatorModel().findNode(attribute);
                        if (attributeNode != null) {
                            columnItem.setImage(0, DBeaverIcons.getImage(attributeNode.getNodeIcon()));
                        }
                        fillAttributeColumns(attribute, col, columnItem);
                        columnItem.setData(col);
                    }
                    UIUtils.packColumns(columnsTable);
                    updateColumnSelection();
                    onAttributesLoad();
                    preselectAttributes();
                });
            }
        });
        loadJob.schedule();
    }

    private void preselectAttributes() {
        IStructuredSelection selection = getCurrentSelection();
        if (selection == null) {
            return;
        }

        for (Object selItem: selection) {
            DBNNode selNode = RuntimeUtils.getObjectAdapter(selItem, DBNNode.class);
            if (!(selNode instanceof DBNDatabaseNode)) {
                continue;
            }
            DBSObject dbsObject = ((DBNDatabaseNode) selNode).getObject();
            TableItem[] tableColumns = columnsTable.getItems();
            for (TableItem tableItem: tableColumns) {
                Object data = tableItem.getData();
                if (!(data instanceof AttributesSelectorPage.AttributeInfo)) {
                    continue;
                }
                AttributesSelectorPage.AttributeInfo attributeInfo = (AttributesSelectorPage.AttributeInfo) data;
                if (Objects.equals(dbsObject, attributeInfo.attribute)) {
                    tableItem.setChecked(true);
                    handleItemSelect(tableItem, true);
                    break;
                }
            }
        }
    }

    @Nullable
    private static IStructuredSelection getCurrentSelection() {
        IWorkbenchPart part = UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (part == null) { //fixme it's a copy from navigator utils
            return null;
        }
        ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            return null;
        }
        ISelection selection = selectionProvider.getSelection();
        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            return null;
        }
        return (IStructuredSelection) selection;
    }

    protected void onAttributesLoad() {
    }

    protected boolean isShowHiddenAttributes() {
        return false;
    }

    private Composite createTableNameInput(Composite panel) {
        final Composite tableGroup = new Composite(panel, SWT.NONE);
        tableGroup.setLayout(new GridLayout(2, false));
        tableGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createLabelText(
            tableGroup,
            EditorsMessages.dialog_struct_columns_select_label_table,
            DBUtils.getObjectFullName(entity, DBPEvaluationContext.UI), SWT.BORDER | SWT.READ_ONLY, new GridData(GridData.FILL_HORIZONTAL));
        return tableGroup;
    }

    void handleItemSelect(TableItem item, boolean notify) {
        final AttributeInfo col = (AttributeInfo) item.getData();
        if (item.getChecked() && col.position < 0) {
            // Checked
            col.position = 0;
            for (AttributeInfo tmp : attributes) {
                if (tmp != col && tmp.position >= col.position) {
                    col.position = tmp.position + 1;
                }
            }
            item.setText(1, String.valueOf(col.position + 1));
        } else if (!item.getChecked() && col.position >= 0) {
            // Unchecked
            item.setText(1, ""); //$NON-NLS-1$
            TableItem[] allItems = columnsTable.getItems();
            for (AttributeInfo tmp : attributes) {
                if (tmp != col && tmp.position >= col.position) {
                    tmp.position--;
                    for (TableItem ai : allItems) {
                        if (ai.getData() == tmp) {
                            ai.setText(1, String.valueOf(tmp.position + 1));
                            break;
                        }
                    }
                }
            }
            col.position = -1;
        }
        if (notify) {
            handleColumnsChange();
            updateToggleButton();
            updatePageState();
        }
    }

    private boolean hasCheckedColumns()
    {
        boolean hasCheckedColumns = false;
        for (AttributeInfo tmp : attributes) {
            if (tmp.position >= 0) {
                hasCheckedColumns = true;
                break;
            }
        }
        return hasCheckedColumns;
    }

    private void updateToggleButton()
    {
        if (hasCheckedColumns()) {
            toggleButton.setText("Clear All");
        } else {
            toggleButton.setText("Select All");
        }
    }

    @NotNull
    public List<DBSEntityAttribute> getSelectedAttributes()
    {
        List<DBSEntityAttribute> tableColumns = new ArrayList<>();
        Set<AttributeInfo> orderedAttributes = new TreeSet<>(new Comparator<AttributeInfo>() {
            @Override
            public int compare(AttributeInfo o1, AttributeInfo o2) {
                return o1.position - o2.position;
            }
        });
        orderedAttributes.addAll(attributes);
        for (AttributeInfo col : orderedAttributes) {
            if (col.position >= 0) {
                tableColumns.add(col.attribute);
            }
        }
        return tableColumns;
    }

    protected void createContentsBeforeColumns(Composite panel)
    {

    }

    protected void createContentsAfterColumns(Composite panel)
    {

    }

    public void updateColumnSelection(@NotNull Predicate<DBSEntityAttribute> predicate) {
        for (TableItem item : columnsTable.getItems()) {
            item.setChecked(false);
            if (item.getData() instanceof AttributeInfo) {
                DBSEntityAttribute attribute = ((AttributeInfo) item.getData()).getAttribute();
                if (predicate.test(attribute)) {
                    item.setChecked(true);
                }
            }
            handleItemSelect(item, false);
        }
        updateToggleButton();
    }

    public void updateColumnSelection() {
        updateColumnSelection(this::isColumnSelected);
    }

    public boolean isColumnSelected(DBSEntityAttribute attribute)
    {
        return false;
    }

    protected void handleColumnsChange()
    {

    }

    @Override
    public boolean isPageComplete() {
        for (TableItem item : columnsTable.getItems()) {
            if (item.getChecked()) {
                return true;
            }
        }
        return false;
    }
}
