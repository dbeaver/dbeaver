package org.jkiss.dbeaver.ext.dm.model.source;

import org.jkiss.dbeaver.ext.dm.model.DmSourceType;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;

public interface DmSourceObject extends DBSObjectWithScript, DmStatefulObject {
	
	void setName(String name);
	
	DmSourceType getSourceType();
	 
	DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor);
}
