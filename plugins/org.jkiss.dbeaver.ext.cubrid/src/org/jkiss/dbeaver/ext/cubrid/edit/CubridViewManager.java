package org.jkiss.dbeaver.ext.cubrid.edit;

import org.jkiss.dbeaver.ext.generic.edit.GenericViewManager;

public class CubridViewManager extends GenericViewManager {

	@Override
	public boolean canCreateObject(Object container) {
		return false;
	}

}
