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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class HANASynonym extends GenericSynonym implements DBPQualifiedObject {

    private String targetObjectType, targetObjectSchema, targetObjectName;

    public HANASynonym(GenericStructContainer container, String name, String targetObjectType, String targetObjectSchema, String targetObjectName) {
        super(container, name, null);
        this.targetObjectType = targetObjectType;
        this.targetObjectSchema = targetObjectSchema;
        this.targetObjectName = targetObjectName;
    }

    @Property(viewable = true, order = 20)
    public String getTargetObjectType(DBRProgressMonitor monitor) throws DBException {
        return targetObjectType;
    }
    
    @Property(viewable = true, order = 21)
    public DBSObject getTargetObjectSchema(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().getSchema(targetObjectSchema);
    }

    @Property(viewable = true, order = 22)
	@Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
        GenericSchema schema = getDataSource().getSchema(targetObjectSchema);
        switch(targetObjectType) {
        case "PROCEDURE":
            return schema.getProcedure(monitor, targetObjectName); 
        default:
            return schema.getChild(monitor, targetObjectName);
        }
    }
   
}
