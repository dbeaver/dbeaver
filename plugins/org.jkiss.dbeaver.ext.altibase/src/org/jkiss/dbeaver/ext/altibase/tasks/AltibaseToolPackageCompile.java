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
package org.jkiss.dbeaver.ext.altibase.tasks;

import org.jkiss.dbeaver.ext.altibase.model.AltibasePackage;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;

import java.util.List;

public class AltibaseToolPackageCompile extends SQLToolExecuteHandler<AltibasePackage, AltibaseToolPackageCompileSettings> {

    @Override
    public AltibaseToolPackageCompileSettings createToolSettings() {
        return new AltibaseToolPackageCompileSettings();
    }

    @Override
    public void generateObjectQueries(DBCSession session, AltibaseToolPackageCompileSettings settings, 
            List<DBEPersistAction> queries, AltibasePackage object) throws DBCException {
        String sql = String.format("ALTER PACKAGE %s COMPILE PACKAGE", object.getFullyQualifiedName(DBPEvaluationContext.DDL));
        queries.add(new SQLDatabasePersistAction(sql));
    }

    public boolean needsRefreshOnFinish() {
        return true;
    }
}
