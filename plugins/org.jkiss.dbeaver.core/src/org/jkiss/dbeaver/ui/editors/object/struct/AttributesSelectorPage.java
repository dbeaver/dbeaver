/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.*;
import java.util.List;

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
    protected Group columnsGroup;

    private static class AttributeInfo {
        DBSEntityAttribute attribute;
        int position;

        public AttributeInfo(DBSEntityAttribute attribute)
        {
            this.attribute = attribute;
            this.position = -1;
        }
    }

    public AttributesSelectorPage(
        String title,
        DBSEntity entity)
    {
        super(NLS.bind(CoreMessages.dialog_struct_columns_select_title, title, entity.getName()));
        this.entity = entity;
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
        columnsGroup = UIUtils.createControlGroup(panel, CoreMessages.dialog_struct_columns_select_group_columns, 1, GridData.FILL_BOTH, 0);
        //columnsViewer = new TableViewer(columnsGroup, SWT.BORDER | SWT.SINGLE | SWT.CHECK);
        columnsTable = new Table(columnsGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK);
        columnsTable.setHeaderVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        //gd.widthHint = 300;
        //gd.heightHint = 200;
        columnsTable.setLayoutData(gd);
        columnsTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                handleItemSelect((TableItem) e.item, true);
            }
        });

        TableColumn colName = UIUtils.createTableColumn(columnsTable, SWT.NONE, CoreMessages.dialog_struct_columns_select_column);
        colName.addListener(SWT.Selection, new SortListener(0));

        TableColumn colPosition = UIUtils.createTableColumn(columnsTable, SWT.CENTER, "#"); //$NON-NLS-1$
        colPosition.addListener(SWT.Selection, new SortListener(1));

        TableColumn colType = UIUtils.createTableColumn(columnsTable, SWT.RIGHT, "Type"); //$NON-NLS-1$
        colType.addListener(SWT.Selection, new SortListener(2));

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

    protected void fillAttributes(final DBSEntity entity)
    {
        // Collect attributes
        final List<DBSEntityAttribute> attributes = new ArrayList<>();
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        for (DBSEntityAttribute attr : CommonUtils.safeCollection(entity.getAttributes(monitor))) {
                            if (!DBUtils.isHiddenObject(attr)) {
                                attributes.add(attr);
                            }
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(
                getShell(),
                CoreMessages.dialog_struct_columns_select_error_load_columns_title,
                CoreMessages.dialog_struct_columns_select_error_load_columns_message,
                e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }

        for (DBSEntityAttribute attribute : attributes) {
            TableItem columnItem = new TableItem(columnsTable, SWT.NONE);

            AttributeInfo col = new AttributeInfo(attribute);
            this.attributes.add(col);

            DBNDatabaseNode attributeNode = DBeaverCore.getInstance().getNavigatorModel().findNode(attribute);
            if (attributeNode != null) {
                columnItem.setImage(0, DBeaverIcons.getImage(attributeNode.getNodeIcon()));
            }
            columnItem.setText(0, attribute.getName());
            columnItem.setText(1, String.valueOf(attribute.getOrdinalPosition()));
            columnItem.setText(2, attribute.getFullTypeName());
            columnItem.setData(col);
            if (isColumnSelected(attribute)) {
                columnItem.setChecked(true);
                handleItemSelect(columnItem, false);
            }
        }
        UIUtils.packColumns(columnsTable);
        updateToggleButton();
    }

    private Composite createTableNameInput(Composite panel) {
        final Composite tableGroup = new Composite(panel, SWT.NONE);
        tableGroup.setLayout(new GridLayout(2, false));
        tableGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Text tableNameLabel = UIUtils.createLabelText(
            tableGroup,
            CoreMessages.dialog_struct_columns_select_label_table,
            DBUtils.getObjectFullName(entity, DBPEvaluationContext.UI), SWT.BORDER | SWT.READ_ONLY);
        return tableGroup;
    }

    private void handleItemSelect(TableItem item, boolean notify)
    {
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

    public Collection<DBSEntityAttribute> getSelectedAttributes()
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

    private class SortListener implements Listener
    {
        int columnIndex;
        int sortDirection = SWT.DOWN;
        TableColumn prevColumn = null;

        private SortListener(int columnIndex) {
            this.columnIndex = columnIndex;
        }

        @Override
        public void handleEvent(Event e) {
            final Collator collator = Collator.getInstance(Locale.getDefault());
            TableColumn column = (TableColumn)e.widget;
            if (prevColumn == column) {
                // Set reverse order
                sortDirection = (sortDirection == SWT.UP ? SWT.DOWN : SWT.UP);
            }
            prevColumn = column;
            columnsTable.setSortColumn(column);
            columnsTable.setSortDirection(sortDirection);
            UIUtils.sortTable(columnsTable, new Comparator<TableItem>() {
                @Override
                public int compare(TableItem e1, TableItem e2) {
                    int mul = (sortDirection == SWT.UP ? 1 : -1);
                    String text1 = e1.getText(columnIndex);
                    String text2 = e2.getText(columnIndex);
                    try {
                        return (int)(Double.parseDouble(text1) - Double.parseDouble(text2)) * mul;
                    } catch (NumberFormatException e3) {
                        return collator.compare(text1, text2) * mul;
                    }
                }
            });
        }
    }

}
