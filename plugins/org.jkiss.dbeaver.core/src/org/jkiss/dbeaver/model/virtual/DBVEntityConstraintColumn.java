package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

/**
 * Constraint column
 */
public class DBVEntityConstraintColumn implements DBSEntityAttributeRef {

    private final DBVEntityConstraint constraint;
    private final String attributeName;

    public DBVEntityConstraintColumn(String attributeName, DBVEntityConstraint constraint)
    {
        this.attributeName = attributeName;
        this.constraint = constraint;
    }

    @Override
    public DBSEntityAttribute getAttribute()
    {
        return constraint.getParentObject().getAttribute(attributeName);
    }

}
