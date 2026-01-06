package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBStatefulObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;

public interface YashanDBSourceObject extends DBSObjectWithScript, YashanDBStatefulObject {

	void setName(String name);

	YashanDBSourceType getSourceType();
}
