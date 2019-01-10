/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.jkiss.dbeaver.debug.DBGVariable;

public class DatabaseValue extends DatabaseDebugElement implements IValue {

    private DBGVariable<?> dbgVariable;

    public DatabaseValue(IDatabaseDebugTarget target, DBGVariable<?> dbgObject) {
        super(target);
        this.dbgVariable = dbgObject;
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getValueString() throws DebugException {
        // TODO Auto-generated method stub
        return String.valueOf(dbgVariable.getVal());
    }

    @Override
    public boolean isAllocated() throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasVariables() throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

}
