package org.jkiss.dbeaver.ext.cubrid.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaModel;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DPIContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class CubridDataSource extends GenericDataSource implements CubridStructContainer{

	private final CubridMetaModel metaModel;
	private CubridObjectContainer structureContainer;
	private ArrayList<CubridUser> owners;
	
	public CubridDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, CubridMetaModel metaModel) throws DBException {
		super(monitor, container, metaModel, new CubridSQLDialect());
		this.metaModel = new CubridMetaModel();
    }
    
	@DPIContainer
	@NotNull
	@Override
	public CubridDataSource getDataSource() {
		return this;
	}
    
	@Override
	public CubridStructContainer getObject() {
		return this;
	}
    
	public Collection<CubridUser> getOwners() {
		return owners;
	}

	@Override
	public Collection<? extends CubridUser> getCubridUsers(DBRProgressMonitor monitor) throws DBException {
		return structureContainer == null ? null : structureContainer.getCubridUsers(monitor);
	}
	
	@NotNull
	public CubridMetaModel getMetaModel() {
		return metaModel;
	}
    
	@Override
	public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
		super.initialize(monitor);
		structureContainer = new DataSourceObjectContainer();
		owners = new ArrayList<CubridUser>();
		try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Get Owner")) {
			String sql = CubridConstants.OWNER_QUERY;
			JDBCPreparedStatement dbStat = session.prepareStatement(sql);
			ResultSet rs = dbStat.executeQuery();
			while(rs.next()) {
				CubridUser owner = new CubridUser(rs.getString("name"));
				owners.add(owner);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private class DataSourceObjectContainer extends CubridObjectContainer {

		protected DataSourceObjectContainer() {
			super(CubridDataSource.this);
		}

		@Override
		public CubridStructContainer getObject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public GenericCatalog getCatalog() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public GenericSchema getSchema() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Class<? extends DBSObject> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public DBSObject getParentObject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}
