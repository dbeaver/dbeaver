package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

/**
 * Reference to another object (usually DBDStructure).
 */
public interface DBDReference extends DBDComplexType {

    /**
     * Retrieves referenced object.
     * Object is retrieved in lazy way because references may point to owner objects in circular way.
     * @return referenced object
     * @throws DBCException
     */
    Object getReferencedObject(DBCExecutionContext context)
        throws DBCException;

}
