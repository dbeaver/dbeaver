/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2IndexColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2IndexType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2UniqueRule;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DB2 Index manager
 * 
 * @author Denis Forveille
 * 
 */
public class DB2IndexManager extends SQLIndexManager<DB2Index, DB2TableBase> {

    private static final String             CONS_IX_NAME = "%s_%s_IDX";

    private static final List<DBSIndexType> IX_TYPES;

    static {
        IX_TYPES = new ArrayList<>(DB2IndexType.values().length);
        for (DB2IndexType db2IndexType : DB2IndexType.values()) {
            if (db2IndexType.isValidForCreation()) {
                IX_TYPES.add(db2IndexType.getDBSIndexType());
            }
        }
    }

    @Override
    public boolean canEditObject(DB2Index object)
    {
        return false;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DB2Index> getObjectsCache(DB2Index object)
    {
        return object.getParentObject().getSchema().getIndexCache();
    }

    @Override
    protected DB2Index createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final DB2TableBase table, Object from)
    {
        return new UITask<DB2Index>() {
            @Override
            protected DB2Index runTask() {
                EditIndexPage editPage = new EditIndexPage(
                    DB2Messages.edit_db2_index_manager_dialog_title, table, IX_TYPES);
                if (!editPage.edit()) {
                    return null;
                }

                String tableName = CommonUtils.escapeIdentifier(table.getName());
                String colName = CommonUtils.escapeIdentifier(editPage.getSelectedAttributes().iterator().next().getName());

                String indexBaseName = String.format(CONS_IX_NAME, tableName, colName);
                String indexName = DBObjectNameCaseTransformer.transformName(table.getDataSource(), indexBaseName);

                DB2Index index = new DB2Index(
                    table,
                    indexName,
                    editPage.getIndexType(),
                    editPage.isUnique() ? DB2UniqueRule.U : DB2UniqueRule.D);

                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    index.addColumn(new DB2IndexColumn(
                        index,
                        (DB2TableColumn) tableColumn,
                        colIndex++,
                        !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC))));
                }
                return index;
            }
        }.execute();
    }

}
