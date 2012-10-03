package org.jkiss.dbeaver.model.virtual;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.IOException;

/**
 * Virtual model object
 */
public abstract class DBVObject implements DBSObject {

    static final Log log = LogFactory.getLog(DBVObject.class);

    @Override
    public abstract DBVContainer getParentObject();

    @Override
    public boolean isPersisted() {
        return true;
    }

    public abstract void persist(XMLBuilder xml) throws IOException;

    abstract public boolean hasValuableData();

}
