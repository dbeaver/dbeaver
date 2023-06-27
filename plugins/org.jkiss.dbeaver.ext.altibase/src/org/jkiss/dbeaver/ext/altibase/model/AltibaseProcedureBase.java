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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.ext.generic.model.GenericScriptObject;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AltibaseProcedureBase extends GenericProcedure implements GenericScriptObject, DBPUniqueObject {

    protected List<GenericProcedureParameter> columns;
    protected static final Log log = Log.getLog(AltibaseProcedureBase.class);

    public AltibaseProcedureBase(GenericStructContainer container, String procedureName, 
            DBSProcedureType procedureType) {
        super(container, procedureName, null, null, procedureType, GenericFunctionResultType.UNKNOWN);
    }
    
    public AltibaseProcedureBase(GenericStructContainer container, String procedureName, String specificName,
            String description, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        super(container, procedureName, specificName, description, procedureType, functionResultType);
    }

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
    public GenericCatalog getCatalog()
    {
        return getContainer().getCatalog();
    }
    
    @Property(viewable = false, hidden = true, order = 7)
    public GenericFunctionResultType getFunctionResultType() {
        return super.getFunctionResultType();
    }
    
    public void setProcedureType(DBSProcedureType procedureType) {
        Field procedureTypeField = null;
        try {
            procedureTypeField = 
                    AltibaseProcedureBase.class.getSuperclass().getDeclaredField("procedureType");
            procedureTypeField.setAccessible(true);
            procedureTypeField.set(this, procedureType);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
        } catch (IllegalAccessException e) {
            log.error(e.getMessage());
        } catch (NoSuchFieldException e) {
            log.error(e.getMessage());
        }catch (SecurityException e) {
            log.error(e.getMessage());
        }
    }

}
