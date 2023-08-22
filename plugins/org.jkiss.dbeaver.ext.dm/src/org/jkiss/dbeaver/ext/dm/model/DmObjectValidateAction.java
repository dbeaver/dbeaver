package org.jkiss.dbeaver.ext.dm.model;

import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;

/**
 * Dm Persist action with validation
 * 
 * @author caosw
 *
 */
public class DmObjectValidateAction extends DmObjectPersistAction {

	private final DmSourceObject object;

	public DmObjectValidateAction(DmSourceObject object, DmObjectType objectType, String title, String script) {
		super(objectType, title, script);
		this.object = object;
	}

	@Override
	public void afterExecute(DBCSession session, Throwable error) throws DBCException {
		if(error != null) {
			return ;
		}
	}
	
}
