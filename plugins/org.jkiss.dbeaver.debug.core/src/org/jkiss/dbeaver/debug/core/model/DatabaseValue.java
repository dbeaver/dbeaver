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
