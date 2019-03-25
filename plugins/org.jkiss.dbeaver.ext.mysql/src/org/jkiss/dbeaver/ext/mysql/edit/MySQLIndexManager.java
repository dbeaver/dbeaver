/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;

/**
 * MySQL index manager
 */
public class MySQLIndexManager extends SQLIndexManager<MySQLTableIndex, MySQLTable> {

    @Nullable
    @Override
    public DBSObjectCache<MySQLDatabase, MySQLTableIndex> getObjectsCache(MySQLTableIndex object)
    {
        return object.getTable().getContainer().getIndexCache();
    }

    @Override
    protected MySQLTableIndex createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final MySQLTable parent,
        Object from)
    {
        return new UITask<MySQLTableIndex>() {
            @Override
            protected MySQLTableIndex runTask() {
                MyEditIndexPage editPage = new MyEditIndexPage(parent);
                if (!editPage.edit()) {
                    return null;
                }

                final MySQLTableIndex index = new MySQLTableIndex(
                    parent,
                    !editPage.isUnique(),
                    null,
                    editPage.getIndexType(),
                    null,
                    false);
                StringBuilder idxName = new StringBuilder(64);
                idxName.append(CommonUtils.escapeIdentifier(parent.getName()));
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    if (colIndex == 1) {
                        idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName())); //$NON-NLS-1$
                    }
                    Integer length = (Integer) editPage.getAttributeProperty(tableColumn, MyEditIndexPage.PROP_LENGTH);
                    index.addColumn(
                        new MySQLTableIndexColumn(
                            index,
                            (MySQLTableColumn) tableColumn,
                            colIndex++,
                            !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC)),
                            false,
                            length == null ? null : String.valueOf(length)));
                }
                idxName.append("_IDX"); //$NON-NLS-1$
                index.setName(DBObjectNameCaseTransformer.transformObjectName(index, idxName.toString()));
                return index;
            }
        }.execute();
    }

    @Override
    protected String getDropIndexPattern(MySQLTableIndex index)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected void appendIndexType(MySQLTableIndex index, StringBuilder decl) {
        DBSIndexType indexType = index.getIndexType();
        if (indexType != MySQLConstants.INDEX_TYPE_FULLTEXT) {
            decl.append(" USING ").append(indexType.getId());
        }
    }

    protected void appendIndexModifiers(MySQLTableIndex index, StringBuilder decl) {
        if (index.getIndexType() == MySQLConstants.INDEX_TYPE_FULLTEXT) {
            decl.append(" FULLTEXT");
        } else {
            super.appendIndexModifiers(index, decl);
        }
    }

    protected void appendIndexColumnModifiers(DBRProgressMonitor monitor, StringBuilder decl, DBSTableIndexColumn indexColumn) {
        final String subPart = ((MySQLTableIndexColumn) indexColumn).getSubPart();
        if (!CommonUtils.isEmpty(subPart)) {
            decl.append(" (").append(subPart).append(")");
        }
        if (!indexColumn.isAscending()) {
            decl.append(" DESC"); //$NON-NLS-1$
        }
    }

    private static class MyEditIndexPage extends EditIndexPage {

        public static final String PROP_LENGTH = "length";

        private int lengthColumnIndex;

        public MyEditIndexPage(MySQLTable parent) {
            super(MySQLMessages.edit_index_manager_title, parent,
                    Arrays.asList(MySQLConstants.INDEX_TYPE_BTREE,
                    MySQLConstants.INDEX_TYPE_FULLTEXT,
                    MySQLConstants.INDEX_TYPE_HASH,
                    MySQLConstants.INDEX_TYPE_RTREE));

        }

        @Override
        protected void createAttributeColumns(Table columnsTable) {
            super.createAttributeColumns(columnsTable);

            TableColumn colDesc = UIUtils.createTableColumn(columnsTable, SWT.NONE, "Length");
            colDesc.setToolTipText("Index length (for varchar columns)");
        }

        @Override
        protected int fillAttributeColumns(DBSEntityAttribute attribute, AttributeInfo attributeInfo, TableItem columnItem) {
            lengthColumnIndex = super.fillAttributeColumns(attribute, attributeInfo, columnItem) + 1;
            Integer length = (Integer) attributeInfo.getProperty(PROP_LENGTH);
            columnItem.setText(lengthColumnIndex, length == null ? "" : length.toString());

            return lengthColumnIndex;
        }

        @Override
        protected Control createCellEditor(Table table, int index, TableItem item, AttributeInfo attributeInfo) {
            if (index == lengthColumnIndex && attributeInfo.getAttribute().getDataKind() == DBPDataKind.STRING) {
                Integer length = (Integer) attributeInfo.getProperty(PROP_LENGTH);
                Spinner spinner = new Spinner(table, SWT.BORDER);
                spinner.setMinimum(0);
                spinner.setMaximum((int) attributeInfo.getAttribute().getMaxLength());
                if (length != null) {
                    spinner.setSelection(length);
                }
                return spinner;
            }
            return super.createCellEditor(table, index, item, attributeInfo);
        }

        @Override
        protected void saveCellValue(Control control, int index, TableItem item, AttributeInfo attributeInfo) {
            if (index == lengthColumnIndex) {
                Spinner spinner = (Spinner) control;
                int length = spinner.getSelection();
                item.setText(index, length <= 0 ? "" : String.valueOf(length));
                if (length <= 0) {
                    attributeInfo.setProperty(PROP_LENGTH, null);
                } else {
                    attributeInfo.setProperty(PROP_LENGTH, length);
                }
            } else {
                super.saveCellValue(control, index, item, attributeInfo);
            }
        }
    }

}
