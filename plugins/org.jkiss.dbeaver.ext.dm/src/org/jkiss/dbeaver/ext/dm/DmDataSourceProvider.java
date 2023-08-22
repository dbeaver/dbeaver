package org.jkiss.dbeaver.ext.dm;

import java.util.ArrayList;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.resource.DBeaverNature;
import org.jkiss.utils.CommonUtils;

public class DmDataSourceProvider extends JDBCDataSourceProvider implements DBPNativeClientLocationManager{

	
	public DmDataSourceProvider() {

	}

	@Override
	public long getFeatures() {
		return FEATURE_SCHEMAS;
	}

	@Override
	public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
		StringBuilder url = new StringBuilder();
		url.append("jdbc:dm://").append(connectionInfo.getHostName());
		if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
			url.append(":").append(connectionInfo.getHostPort());
		}
		url.append("/");
		if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
			url.append(connectionInfo.getDatabaseName());
		}
		return url.toString();
	}

	@NotNull
	@Override
	public DBPDataSource openDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
			throws DBException {
		return new DmDataSource(monitor, container);
	}
	
	@Override
	public List<DBPNativeClientLocation> findLocalClientLocations() { //find 本地客户端
      List<DBPNativeClientLocation> list=new ArrayList<>(); // 注意 泛型不具有父子继承关系，即List<DBPNativeClientLocation> 和List<DmServerHome>不具有父子继承关系
      return list;
	}

	@Override
	public DBPNativeClientLocation getDefaultLocalClientLocation() { //默认本地客户端
		     return null;
	}

	@Override
	public String getProductName(DBPNativeClientLocation location) {
		// TODO Auto-generated method stub
		return "DM Client";
	}

	@Override
	public String getProductVersion(DBPNativeClientLocation location){
		// TODO Auto-generated method stub
		return "1.2.4";
	}

}
