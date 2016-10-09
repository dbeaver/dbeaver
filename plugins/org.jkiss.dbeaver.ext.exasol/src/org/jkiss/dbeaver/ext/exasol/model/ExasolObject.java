package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
//import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

public abstract class ExasolObject<PARENT extends DBSObject> implements DBSObject, DBPSaveableObject {
    private static final Log log = Log.getLog(ExasolObject.class);

    protected final PARENT parent;
    protected String name;
    private boolean persisted;
    private long objectId;

    protected ExasolObject(PARENT parent, String name, long objectId, boolean persisted)
    {
        this.parent = parent;
        this.name = name;
        this.objectId = objectId;
        this.persisted = persisted;
    }

    protected ExasolObject(PARENT parent, String name, boolean persisted)
    {
        this.parent = parent;
        this.name = name;
        this.persisted = persisted;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public PARENT getParentObject()
    {
        return parent;
    }

    @NotNull
    @Override
    public ExasolDataSource getDataSource()
    {
        return (ExasolDataSource) parent.getDataSource();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public long getObjectId()
    {
        return objectId;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }
}
