package org.jkiss.dbeaver.ext.hana.model;

import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

public class HANAProcedure extends GenericProcedure {

    public HANAProcedure(GenericStructContainer container, String procedureName, String specificName,
            String description, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        super(container, procedureName, specificName, description, procedureType, functionResultType);
    }

    @Association
    public List<HANADependency> getDependencies(DBRProgressMonitor monitor) throws DBException {
        return HANADependency.readDependencies(monitor, this);
    }
}
