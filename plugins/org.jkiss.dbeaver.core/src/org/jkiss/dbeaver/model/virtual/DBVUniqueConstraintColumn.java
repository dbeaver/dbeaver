package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;

/**
 * Constraint column
 */
public class DBVUniqueConstraintColumn implements DBSEntityAttributeRef {

    private final DBVUniqueConstraint constraint;
    private final String attributeName;

    public DBVUniqueConstraintColumn(String attributeName, DBVUniqueConstraint constraint)
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
