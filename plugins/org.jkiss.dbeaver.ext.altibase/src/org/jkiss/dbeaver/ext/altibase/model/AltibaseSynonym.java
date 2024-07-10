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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class AltibaseSynonym extends GenericSynonym implements DBPScriptObject {

    private static final Log log = Log.getLog(AltibaseSynonym.class);
    
    protected boolean isPublicSynonym;
    protected String refObjectSchema;
    protected String refObjectName;

    protected String ddl;
    protected DBSObject refObj = null;

    protected AltibaseSynonym(GenericStructContainer container, int ownerId, 
            String name, String description, String refObjectSchema, String refObjectName) {
        super(container, name, description);

        isPublicSynonym = (ownerId < 0);
        this.refObjectSchema = refObjectSchema;
        this.refObjectName = refObjectName;
    }

    @Property(viewable = true, linkPossible = true, order = 4)
    public DBSObject getObject(DBRProgressMonitor monitor) throws DBException {
        if (refObj == null) {
            refObj = getTargetObject(monitor);
        }
        
        return refObj;
    }
    
    /**
     * There are three cases unable to access reference object.
     * 1. Show system object set as false and PUBLIC SYNONYM pointing to a SystemObject. e.g. SYSTEM_'s objects
     *    -> Resolve: Set Show system object as true
     * 2. Altibase SYSTEM_.SYS_SYNONYMS_ meta table says 
     */
    @Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) {
        if (refObj == null) {
            try {
                refObj = ((AltibaseDataSource) getDataSource()).findSynonymTargetObject(monitor, refObjectSchema, refObjectName);
            } catch (DBException e) {
                log.warn("Failed to get a synonym's target object: " 
                        + getFullyQualifiedName(DBPEvaluationContext.DDL)
                        + " for " + refObjectSchema + "." + refObjectName + ": "
                        + e.getMessage());
            }
        }
        
        return refObj;
    }
    
    /**
     * Create Synonym DDL in case of unable to use DBMS_METADATA.
     */
    public String getBuiltDdlLocaly(DBRProgressMonitor monitor) {
        DBSObject refObject = getTargetObject(monitor);
        String refObjFullName = "";
        
        if (refObject != null) {
            // Public synonym doesn't need schema name
            if ((refObject instanceof AltibaseSynonym) && ((AltibaseSynonym) refObject).isPublicSynonym()) {
                refObjFullName = DBUtils.getFullQualifiedName(getDataSource(), refObject);
            // Otherwise, schema name required. 
            } else {
                refObjFullName = DBUtils.getFullQualifiedName(getDataSource(), refObject.getParentObject(), refObject);
            }
        }
    
        return new StringBuilder("CREATE ").append(getSynonymBody()).append(" FOR ").append(refObjFullName).toString();
    }
    
    // BODY = "[PUBLIC] SYNONYM synonym_name"
    public String getSynonymBody() {
        StringBuilder ddl = new StringBuilder();
        String name;
        
        if (isPublicSynonym) {
            name = getName();
            ddl.append("PUBLIC ");
        } else {
            name = getFullyQualifiedName(DBPEvaluationContext.DDL);
        }
        
        ddl.append("SYNONYM ").append(name);
        
        return ddl.toString();
    }
    
    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_REFRESH)) {
            ddl = null;
        }
        
        // Try to get from DBMS_METADATA
        if (CommonUtils.isEmpty(ddl)) {
            ddl = ((AltibaseMetaModel) getDataSource().getMetaModel()).getSynonymDDL(monitor, this, options);
        }

        if (CommonUtils.isEmpty(ddl)) {
            ddl = AltibaseConstants.NO_DBMS_METADATA + getBuiltDdlLocaly(monitor);
        }
        
        return (CommonUtils.isEmpty(ddl)) ? "" : ddl + ";";
    }
    
    public boolean isPublicSynonym() {
        return isPublicSynonym;
    }
    
    public void setPublicSynonym() {
        isPublicSynonym = true;
    }
}
