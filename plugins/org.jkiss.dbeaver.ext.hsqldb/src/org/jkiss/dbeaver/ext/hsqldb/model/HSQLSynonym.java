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
package org.jkiss.dbeaver.ext.hsqldb.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * HSQLSynonym
 */
public class HSQLSynonym extends GenericSynonym {

    private static final Log log = Log.getLog(HSQLSynonym.class);

    private String targetSchemaName;
    private String targetObjectName;
    private String targetObjectType;

    protected HSQLSynonym(GenericStructContainer container, JDBCResultSet dbResult) {
        super(container, JDBCUtils.safeGetString(dbResult, "SYNONYM_NAME"), null);
        targetSchemaName = JDBCUtils.safeGetString(dbResult, "OBJECT_SCHEMA");
        targetObjectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        targetObjectType = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
    }

    @Property(viewable = true, order = 3)
    public String getTargetObjectType() {
        return targetObjectType;
    }

    @Property(viewable = true, order = 4)
    @Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
        GenericSchema schema = getDataSource().getSchema(targetSchemaName);
        if (schema == null) {
            log.error("Schema '" + targetSchemaName + "' not found");
        } else {
            return schema.getTable(monitor, targetObjectName);
        }
        return null;
    }
}
