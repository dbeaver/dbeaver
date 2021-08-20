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

package org.jkiss.dbeaver.ext.oceanbase.mysql.model;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

public class OceanbaseMySQLDatabaseManager extends SQLObjectEditor<OceanbaseMySQLCatalog, OceanbaseMySQLDataSource>{

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, OceanbaseMySQLCatalog> getObjectsCache(OceanbaseMySQLCatalog object) {
        return ((OceanbaseMySQLDataSource) object.getDataSource()).getOceanbaseCatalogCache();
    }

    @Override
    protected OceanbaseMySQLCatalog createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
            Object container, Object copyFrom, Map<String, Object> options) {
        return new OceanbaseMySQLCatalog((OceanbaseMySQLDataSource) container, null);
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
            List<DBEPersistAction> actions,
            SQLObjectEditor<OceanbaseMySQLCatalog, OceanbaseMySQLDataSource>.ObjectCreateCommand command,
            Map<String, Object> options) throws DBException {
        final OceanbaseMySQLCatalog catalog = command.getObject();
        final StringBuilder script = new StringBuilder("CREATE SCHEMA `" + catalog.getName() + "`");
        appendDatabaseModifiers(catalog, script);
        actions.add(new SQLDatabasePersistAction("Create schema", script.toString()) // $NON-NLS-2$
        );
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
            List<DBEPersistAction> actions,
            SQLObjectEditor<OceanbaseMySQLCatalog, OceanbaseMySQLDataSource>.ObjectDeleteCommand command,
            Map<String, Object> options) throws DBException {
        actions.add(new SQLDatabasePersistAction("Drop schema", "DROP SCHEMA `" + command.getObject().getName() + "`"));
    }

    private void appendDatabaseModifiers(OceanbaseMySQLCatalog catalog, StringBuilder script) {
        if (catalog.getAdditionalInfo().getDefaultCharset() != null) {
            script.append("\nDEFAULT CHARACTER SET ").append(catalog.getAdditionalInfo().getDefaultCharset().getName());
        }
        if (catalog.getAdditionalInfo().getDefaultCollation() != null) {
            script.append("\nDEFAULT COLLATE ").append(catalog.getAdditionalInfo().getDefaultCollation().getName());
        }
    }

}
