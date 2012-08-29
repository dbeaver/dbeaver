package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.IOException;

/**
 * Virtual model object
 */
public abstract class DBVObject implements DBSObject {

    @Override
    public abstract DBVContainer getParentObject();

    @Override
    public boolean isPersisted() {
        return true;
    }

    public abstract void persist(XMLBuilder xml) throws IOException;

    abstract public boolean hasValuableData();

}
