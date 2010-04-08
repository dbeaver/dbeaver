package org.jkiss.dbeaver.model.impl.meta;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSSchema;

/**
 * AbstractSchema
 */
public abstract class AbstractSchema implements DBSSchema
{
    @Override
    public String toString()
    {
        return getName() + " [" + getDataSource().getContainer().getName() + "]";
    }

}
