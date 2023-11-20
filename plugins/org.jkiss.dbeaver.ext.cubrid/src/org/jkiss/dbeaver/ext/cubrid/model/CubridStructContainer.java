package org.jkiss.dbeaver.ext.cubrid.model;

import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public interface CubridStructContainer extends GenericStructContainer {

	@NotNull
	@Override
	CubridDataSource getDataSource();
    
	CubridStructContainer getObject();

	Collection<? extends CubridUser> getCubridUsers(DBRProgressMonitor monitor) throws DBException;

}
