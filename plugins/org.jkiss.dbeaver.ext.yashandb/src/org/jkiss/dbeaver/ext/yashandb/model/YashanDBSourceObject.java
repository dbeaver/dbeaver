package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBStatefulObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public interface YashanDBSourceObject extends DBSObjectWithScript, YashanDBStatefulObject {
    void setName(String name);

    YashanDBSourceType getSourceType();

    DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) throws DBCException;
}
