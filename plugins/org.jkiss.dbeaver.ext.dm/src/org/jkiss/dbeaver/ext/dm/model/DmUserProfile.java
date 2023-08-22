package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class DmUserProfile extends DmGlobalObject {

	private static final Log log = Log.getLog(DmUserProfile.class);

	private String name;
	private List<ProfileResource> resources;

	public DmUserProfile(DmDataSource dataSource, ResultSet resultSet) {
	        super(dataSource, resultSet != null);
	        this.name = JDBCUtils.safeGetString(resultSet, "PROFILE");
	    }

	@NotNull
	@Override
	@Property(viewable = true, order = 1)
	public String getName() {
		return name;
	}

	@Association
	public Collection<ProfileResource> getResources(DBRProgressMonitor monitor) throws DBException {
		return resources;
	}

	boolean isResourcesCached() {
		return resources != null;
	}

	void setResources(List<ProfileResource> resources) {
		this.resources = resources;
	}

	/**
	 * ProfileResource
	 */
	public static class ProfileResource extends DmObject<DmUserProfile> {
		private static final Log log = Log.getLog(ProfileResource.class);

		private String type;
		private String limit;

		public ProfileResource(DmUserProfile profile, ResultSet resultSet) {
			super(profile, JDBCUtils.safeGetString(resultSet, "RESOURCE_NAME"), true);
			this.type = JDBCUtils.safeGetString(resultSet, "RESOURCE_TYPE");
			this.limit = JDBCUtils.safeGetString(resultSet, "LIMIT");
		}

		@NotNull
		@Override
		@Property(viewable = true, order = 1)
		public String getName() {
			return super.getName();
		}

		@Property(viewable = true, order = 2)
		public String getType() {
			return type;
		}

		@Property(viewable = true, order = 3)
		public String getLimit() {
			return limit;
		}
	}
}
