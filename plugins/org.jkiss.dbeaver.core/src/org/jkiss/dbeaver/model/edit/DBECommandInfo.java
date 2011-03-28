package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Command info
 */
public interface DBECommandInfo<OBJECT_TYPE extends DBSObject> {

    DBECommand<OBJECT_TYPE> getCommand();

    DBECommandReflector getReflector();

}
