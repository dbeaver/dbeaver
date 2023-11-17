/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AltibaseProcedureBase extends GenericProcedure implements DBSObjectWithScript {

    protected static final Log log = Log.getLog(AltibaseProcedureBase.class);
    
    protected List<GenericProcedureParameter> columns;
    private DBSProcedureType procedureType;

    /**
     * Constructor
     */
    public AltibaseProcedureBase(GenericStructContainer container, String procedureName, String specificName,
            String description, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        super(container, procedureName, specificName, description, procedureType, functionResultType);
        this.procedureType = procedureType;
    }

    @Override
    public void addColumn(GenericProcedureParameter column) {
        if (this.columns == null) {
            this.columns = new ArrayList<>();
        }
        this.columns.add(new AltibaseProcedureParameter(column));
    }

    @Override
    public Collection<GenericProcedureParameter> getParameters(DBRProgressMonitor monitor)
            throws DBException {
        if (columns == null) {
            loadProcedureColumns(monitor);
        }
        return columns;
    }

    @Nullable
    @Override
    @Property(viewable = false, hidden = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return description;
    }
    
    @Property(viewable = false, hidden = true, order = 3)
    public GenericCatalog getCatalog() {
        return getContainer().getCatalog();
    }
    
    @Property(viewable = false, hidden = true, order = 7)
    public GenericFunctionResultType getFunctionResultType() {
        return super.getFunctionResultType();
    }
    
    /**
     * Set procedure type, especially for Typeset
     */
    public void setProcedureType(DBSProcedureType procedureType) {
        this.procedureType = procedureType;
    }
    
    /**
     * Get Procedure type, especially for TYPESET
     */
    public String getProcedureTypeName() {
        if (procedureType == DBSProcedureType.UNKNOWN) {
            return AltibaseConstants.OBJ_TYPE_TYPESET;
        } else {
            return procedureType.name();
        }
    }
    
    @Override
    @Property(hidden = true, editable = true, updatable = true)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return super.getObjectDefinitionText(monitor, options);
    }

    @Override
    public void setObjectDefinitionText(String source) {
        super.setSource(source);
    }
    
}
