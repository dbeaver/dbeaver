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

    public DBVEntityConstraintColumn(DBVEntityConstraint constraint, String attributeName)
    {
        this.constraint = constraint;
        this.attributeName = attributeName;
    }

    @Override
    public DBSEntityAttribute getAttribute()
    {
        return constraint.getParentObject().getAttribute(attributeName);
    }

    public String getAttributeName()
    {
        return attributeName;
    }
}
