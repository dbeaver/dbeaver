/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.ui.config;

import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableColumn;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableIndex;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableIndexColumn;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;

/**
 * SQL Server index configurator
 */
public class SQLServerIndexConfigurator implements DBEObjectConfigurator<SQLServerTableIndex> {

    @Override
    public SQLServerTableIndex configureObject(DBRProgressMonitor monitor, Object container, SQLServerTableIndex index) {
        return UITask.run(() -> {
            EditIndexPage editPage = new EditIndexPage(
                "Create index",
                index,
                Collections.singletonList(DBSIndexType.OTHER));
            if (!editPage.edit()) {
                return null;
            }
            index.setUnique(editPage.isUnique());
            index.setIndexType(editPage.getIndexType());
            index.setDescription(editPage.getDescription());
            StringBuilder idxName = new StringBuilder(64);
            idxName.append(CommonUtils.escapeIdentifier(index.getTable().getName()));
            int colIndex = 1;
            for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                if (colIndex == 1) {
                    idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName()));
                }
                index.addColumn(
                    new SQLServerTableIndexColumn(
                        index,
                        0,
                        (SQLServerTableColumn) tableColumn,
                        colIndex++,
                        !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC))));
            }
            idxName.append("_IDX");
            index.setName(DBObjectNameCaseTransformer.transformObjectName(index, idxName.toString()));
            return index;
        });
    }

}
