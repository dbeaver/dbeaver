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
package org.jkiss.dbeaver.ui.editors.sql.generator;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.generator.SQLGenerator;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingType;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetDataContainerOptions;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.utils.ArrayUtils;

import java.util.Collections;

public class SQLGeneratorDDLFromResultSet extends SQLGenerator<IResultSetController> {

    @Override
    public boolean hasOptions() {
        return false;
    }
    
    @Override
    protected void generateSQL(
        @NotNull DBRProgressMonitor monitor, @NotNull StringBuilder sql, @NotNull IResultSetController object
    ) throws DBException {
        ResultSetDataContainer dataContainer = new ResultSetDataContainer(object, new ResultSetDataContainerOptions());
        DatabaseMappingContainer mapping = new DatabaseMappingContainer(new DatabaseConsumerSettings(), dataContainer);
        mapping.refreshMappingType(monitor, DatabaseMappingType.create, true, true);
        
        DBPDataSource dataSource = object.getDataContainer().getDataSource();
        if (dataSource.getInfo().isDynamicMetadata()) {
            sql.append(SQLEditorMessages.sql_generator_nonsql_text);
            return;
        }

        DBSDataContainer container = object.getModel().getAttributes()[0].getDataContainer();
        DBSObjectContainer objContainer = null;
        for (DBSObject obj = container; obj != null; obj = obj.getParentObject()) {
            if (obj instanceof DBSObjectContainer) {
                objContainer = (DBSObjectContainer) obj;
            }
        }
        if (objContainer == null && dataSource instanceof DBSObjectContainer) {
            objContainer = (DBSObjectContainer) dataSource;
        }
        if (objContainer == null) {
            sql.append(SQLEditorMessages.sql_generator_no_obj_container_text);
            return;
        }

        while (!DBSEntity.class.isAssignableFrom(objContainer.getPrimaryChildType(monitor))) {
            objContainer = (DBSObjectContainer) objContainer.getChildren(monitor).iterator().next();
        }
        
        DBCExecutionContext executionContext = DBUtils.getDefaultContext(dataSource, true);
        DBEPersistAction[] ddl = DatabaseTransferUtils.generateTargetTableDDL(
            monitor,
            executionContext,
            objContainer,
            mapping,
            Collections.emptyMap());

        if (ArrayUtils.isEmpty(ddl)) {
            sql.append(SQLEditorMessages.sql_generator_no_ddl_text);
            return;
        }
        
        String text = SQLUtils.generateScript(dataSource, ddl, true);
        sql.append(text);
    }
}
