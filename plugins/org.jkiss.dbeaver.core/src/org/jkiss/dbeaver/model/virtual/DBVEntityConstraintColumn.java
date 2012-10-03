package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.DBUtils;
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
        // Here we use void monitor.
        // In real life entity columns SHOULD be already read so it doesn't matter
        // But I'm afraid that in some very special cases it does. Thant's too bad.
        return constraint.getParentObject().getAttribute(VoidProgressMonitor.INSTANCE, attributeName);
    }

    public String getAttributeName()
    {
        return attributeName;
    }
}
