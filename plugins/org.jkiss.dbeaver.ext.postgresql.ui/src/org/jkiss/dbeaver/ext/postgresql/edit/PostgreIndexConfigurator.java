/*
 * DBeaver - Universal Database Manager
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

package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreAttribute;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreIndex;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreIndexColumn;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
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
 * Postgre index configurator
 */
public class PostgreIndexConfigurator implements DBEObjectConfigurator<PostgreTableBase, PostgreIndex> {


    @Override
    public PostgreIndex configureObject(DBRProgressMonitor monitor, PostgreTableBase parent, PostgreIndex index) {
        return new UITask<PostgreIndex>() {
            @Override
            protected PostgreIndex runTask() {
                EditIndexPage editPage = new EditIndexPage(
                    "Edit index",
                    parent,
                    Collections.singletonList(DBSIndexType.OTHER));
                if (!editPage.edit()) {
                    return null;
                }

                StringBuilder idxName = new StringBuilder(64);
                idxName.append(CommonUtils.escapeIdentifier(parent.getName()));
                index.setName(idxName.toString());
                index.setIndexType(editPage.getIndexType());
                index.setUnique(editPage.isUnique());
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    if (colIndex == 1) {
                        idxName.append("_").append(CommonUtils.escapeIdentifier(tableColumn.getName())); //$NON-NLS-1$
                    }
                    index.addColumn(
                        new PostgreIndexColumn(
                            index,
                            (PostgreAttribute) tableColumn,
                            null,
                            colIndex++,
                            !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC)),
                            -1,
                            false));
                }
                idxName.append("_IDX"); //$NON-NLS-1$
                index.setName(DBObjectNameCaseTransformer.transformObjectName(index, idxName.toString()));
                return index;
            }
        }.execute();
    }

}
