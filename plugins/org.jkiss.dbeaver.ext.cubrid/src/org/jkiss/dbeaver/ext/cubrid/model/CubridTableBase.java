package org.jkiss.dbeaver.ext.cubrid.model;

import java.util.Collection;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public abstract class CubridTableBase extends GenericTableBase {
	
	private CubridUser owner;
	private CubridObjectContainer container;
	public CubridTableBase(CubridObjectContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
		super(container, tableName, tableType, dbResult);
		this.container = container;
		String owner_name;

		if (dbResult != null) {
			owner_name = GenericUtils.safeGetString(container.getCubridTableCache().tableObject, dbResult, CubridConstants.OWNER_NAME);
		} else {
			owner_name = getDataSource().getContainer().getConnectionConfiguration().getUserName().toUpperCase();
		}

		for(CubridUser cbOwner : getUsers()){
			if(cbOwner.getName().equals(owner_name)) {
				this.owner = cbOwner;
				break;
			}
		}
	}

	public Collection<? extends CubridUser> getUsers() {
		try {
			return container.getDataSource().getCubridUsers(null);
		} catch (DBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Nullable
	@Property(viewable = true, editable = true, updatable = true, listProvider = OwnerListProvider.class, order = 2)
	public CubridUser getOwner() {
		return owner;
	}

	public void setOwner(CubridUser owner) {
		this.owner = owner;
	}

	public static class OwnerListProvider implements IPropertyValueListProvider<CubridTableBase> {

		@Override
		public boolean allowCustomValue() {
			return false;
		}

		@Override
		public Object[] getPossibleValues(CubridTableBase object) {
			return object.getUsers().toArray();
		}
	}

}