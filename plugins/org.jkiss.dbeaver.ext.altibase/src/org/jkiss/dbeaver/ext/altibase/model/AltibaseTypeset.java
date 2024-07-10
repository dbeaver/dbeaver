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

import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

public class AltibaseTypeset extends AltibaseProcedureStandAlone {

    public AltibaseTypeset(GenericStructContainer container, String procedureName, boolean valid) {
        super(container, procedureName, valid, DBSProcedureType.UNKNOWN, GenericFunctionResultType.NO_TABLE);
    }
    
    @Override
    @Property(viewable = false, hidden = true, order = 6)
    public DBSProcedureType getProcedureType() {
        return super.getProcedureType();
    }
    
    @Override
    @Property(viewable = false, hidden = true, order = 7)
    public GenericFunctionResultType getFunctionResultType() {
        return super.getFunctionResultType();
    }
}
