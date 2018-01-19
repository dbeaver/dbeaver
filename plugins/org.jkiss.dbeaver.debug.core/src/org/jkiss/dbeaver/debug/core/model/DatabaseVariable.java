package org.jkiss.dbeaver.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.jkiss.dbeaver.debug.DBGVariable;

public class DatabaseVariable extends DatabaseDebugElement implements IVariable {
    
    private final DBGVariable<?> dbgVariable;

    public DatabaseVariable(IDatabaseDebugTarget target, DBGVariable<?> variable) {
        super(target);
        this.dbgVariable = variable;
    }

    @Override
    public void setValue(String expression) throws DebugException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setValue(IValue value) throws DebugException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean supportsValueModification() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean verifyValue(String expression) throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean verifyValue(IValue value) throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IValue getValue() throws DebugException {
        // TODO Auto-generated method stub
        return new DatabaseValue(getDatabaseDebugTarget(), dbgVariable);
    }

    @Override
    public String getName() throws DebugException {
        return dbgVariable.getName();
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        return dbgVariable.getType().toString();
    }

    @Override
    public boolean hasValueChanged() throws DebugException {
        // TODO Auto-generated method stub
        return false;
    }

}
