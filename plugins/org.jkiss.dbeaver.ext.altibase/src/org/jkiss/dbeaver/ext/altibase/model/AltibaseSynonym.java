/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.AltibaseUtils;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class AltibaseSynonym extends GenericSynonym implements DBPScriptObject {

    protected boolean isPublic;
    protected String refObjectSchema;
    protected String refObjectName;

    protected String ddl;

    protected AltibaseSynonym(GenericStructContainer container, int ownerId, 
            String name, String description, String refObjectSchema, String refObjectName) {
        super(container, name, description);

        isPublic = (ownerId < 1);
        this.refObjectSchema = refObjectSchema;
        this.refObjectName = refObjectName;
    }

    @Nullable
    @Property(id = "Reference", viewable = true, order = 3)
    public String getReferencedObjectName() {
        return AltibaseUtils.getQuotedName(refObjectSchema, refObjectName);
    }

    @Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
        //getDataSource().
        return null;
    }

    public String getSchemaName() {
        return getParentObject().getName();
    }

    public String getDdlLocal() {
        String ddl;

        if (isPublic) {
            ddl = String.format("CREATE PUBLIC SYNONYM %s FOR %s;", 
                    AltibaseUtils.getQuotedName(null, getName()), 
                    getReferencedObjectName());
        } else {
            ddl = String.format("CREATE SYNONYM %s FOR %s", 
                    getFullyQualifiedName(DBPEvaluationContext.DDL), 
                    getReferencedObjectName());
        }

        return ddl;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_REFRESH)) {
            ddl = null;
        }

        if (ddl == null) {
            if (isPublic) {
                ddl = getDdlLocal();
            } else {
                ddl = ((AltibaseMetaModel) getDataSource().getMetaModel()).getSynonymDDL(monitor, this, options);
            }
        }
        return ddl;
    }
}
