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
package org.jkiss.dbeaver.ext.oracle.ui.config;

import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndex;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndexColumn;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIMessages;
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
 * Oracle index manager
 */
public class OracleIndexConfigurator implements DBEObjectConfigurator<OracleTableIndex> {

    @Override
    public OracleTableIndex configureObject(DBRProgressMonitor monitor, Object container, OracleTableIndex index) {
        return UITask.run(() -> {
            EditIndexPage editPage = new EditIndexPage(
                OracleUIMessages.edit_oracle_index_manager_dialog_title,
                index,
                Collections.singletonList(DBSIndexType.OTHER));
            if (!editPage.edit()) {
                return null;
            }

            StringBuilder idxName = new StringBuilder(64);
            idxName.append(CommonUtils.escapeIdentifier(index.getTable().getName())).append("_") //$NON-NLS-1$
                .append(CommonUtils.escapeIdentifier(editPage.getSelectedAttributes().iterator().next().getName()))
                .append("_IDX"); //$NON-NLS-1$
            index.setName(DBObjectNameCaseTransformer.transformName(index.getDataSource(), idxName.toString()));
            index.setUnique(editPage.isUnique());
            index.setIndexType(editPage.getIndexType());
            int colIndex = 1;
            for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                index.addColumn(
                    new OracleTableIndexColumn(
                        index,
                        (OracleTableColumn) tableColumn,
                        colIndex++,
                        !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC)),
                        null));
            }
            return index;
        });
    }

}
