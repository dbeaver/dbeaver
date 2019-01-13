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
package org.jkiss.dbeaver.ext.mssql.model.generic;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
* SQL server synonym
*/
public class SQLServerGenericSynonym extends GenericSynonym implements DBPQualifiedObject {

    private static final Log log = Log.getLog(SQLServerGenericSynonym.class);

    private String targetObjectName;

    public SQLServerGenericSynonym(GenericStructContainer container, String name, String description, String targetObjectName) {
        super(container, name, description);
        this.targetObjectName = targetObjectName;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(
            getDataSource(),
            getParentObject(),
            this);
    }

    @Property(viewable = true, order = 20)
    @Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
        int divPos = targetObjectName.indexOf("].[");
        if (divPos == -1) {
            log.debug("Bad target object name '" + targetObjectName + "' for synonym '" + getName() + "'");
            return null;
        }
        String schemaName = DBUtils.getUnQuotedIdentifier(getDataSource(), targetObjectName.substring(0, divPos + 1));
        String objectName = DBUtils.getUnQuotedIdentifier(getDataSource(), targetObjectName.substring(divPos + 2));
        GenericCatalog database = getParentObject().getCatalog();
        GenericSchema schema = database.getSchema(monitor, schemaName);
        if (schema == null) {
            log.debug("Schema '" + schemaName + "' not found for synonym '" + getName() + "'");
            return null;
        }
        return schema.getChild(monitor, objectName);
    }
}
