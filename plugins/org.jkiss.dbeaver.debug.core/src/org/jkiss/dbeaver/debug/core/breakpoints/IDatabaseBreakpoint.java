package org.jkiss.dbeaver.debug.core.breakpoints;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;

public interface IDatabaseBreakpoint extends IBreakpoint {
    
    String getDatasourceId() throws CoreException;

    String getDatabaseName() throws CoreException;

    String getSchemaName() throws CoreException;

    String getProcedureName() throws CoreException;

    String getProcedureOid() throws CoreException;

    String getNodePath() throws CoreException;

}
