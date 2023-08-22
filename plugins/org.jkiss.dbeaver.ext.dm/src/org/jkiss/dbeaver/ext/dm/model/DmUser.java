package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

/**
 * DM User
 * 
 * @author caosw
 *
 */
public class DmUser extends DmGrantee implements DBAUser, DBSObjectLazy<DmDataSource> {

	private static final Log log = Log.getLog(DmUser.class);

	private long id;
	private String name;
	private String externalName;
	private String status;
	private Timestamp createDate;
	private Timestamp lockDate;
	private Timestamp expiryDate;
	private Object defaultTablespace;
	private Object tempTablespace;
	private String profile;
	private String consumerGroup;
	private transient String password;

	public DmUser(DmDataSource dataSource) {
		super(dataSource);
	}

	public DmUser(DmDataSource dataSource, long id, String name) {
		super(dataSource);
		this.id = id;
		this.name = name;
	}

	public DmUser(DmDataSource dataSource, ResultSet resultSet) {
		super(dataSource);
		this.id = JDBCUtils.safeGetLong(resultSet, "USER_ID");
		this.name = JDBCUtils.safeGetString(resultSet, "USERNAME");
		this.externalName = JDBCUtils.safeGetString(resultSet, "EXTERNAL_NAME");
		this.status = JDBCUtils.safeGetString(resultSet, "ACCOUNT_STATUS");
		this.createDate = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");
		this.lockDate = JDBCUtils.safeGetTimestamp(resultSet, "LOCK_DATE");
		this.expiryDate = JDBCUtils.safeGetTimestamp(resultSet, "EXPIRY_DATE");
		this.defaultTablespace = JDBCUtils.safeGetString(resultSet, "DEFAULT_TABLESPACE");
		this.tempTablespace = JDBCUtils.safeGetString(resultSet, "TEMPORARY_TABLESPACE");
		this.profile = JDBCUtils.safeGetString(resultSet, "PROFILE");
		this.consumerGroup = JDBCUtils.safeGetString(resultSet, "INITIAL_RSRC_CONSUMER_GROUP");
	}

	@Property(order = 1)
	public long getId() {
		return id;
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 2)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Property(order = 3)
	public String getExternalName() {
		return externalName;
	}

	@Property(viewable = true, order = 4)
	public String getStatus() {
		return status;
	}

	@Property(viewable = true, order = 5)
	public Timestamp getCreateDate() {
		return createDate;
	}

	@Property(order = 6)
	public Timestamp getLockDate() {
		return lockDate;
	}

	@Property(order = 7)
	public Timestamp getExpiryDate() {
		return expiryDate;
	}

	@Property(order = 8)
	@LazyProperty(cacheValidator = DmTablespace.TablespaceReferenceValidator.class)
	public Object getDefaultTablespace(DBRProgressMonitor monitor) throws DBException {
		return DmTablespace.resolveTablespaceReference(monitor, this, "defaultTablespace");
	}

	@Property(order = 9)
	@LazyProperty(cacheValidator = DmTablespace.TablespaceReferenceValidator.class)
	public Object getTempTablespace(DBRProgressMonitor monitor) throws DBException {
		return DmTablespace.resolveTablespaceReference(monitor, this, "tempTablespace");
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		if ("defaultTablespace".equals(propertyId)) {
			return defaultTablespace;
		} else if ("tempTablespace".equals(propertyId)) {
			return tempTablespace;
		} else if ("profile".equals(propertyId)) {
			return profile;
		} else {
			return null;
		}
	}

	@Property(order = 10)
	public String getProfile(DBRProgressMonitor monitor) throws DBException {
		return profile;
	}

	@Property(order = 11)
	public String getConsumerGroup() {
		return consumerGroup;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	@Association
	public Collection<DmPrivRole> getRolePrivs(DBRProgressMonitor monitor) throws DBException {
		return rolePrivCache.getAllObjects(monitor, this);
	}

	@Nullable
	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		return super.refreshObject(monitor);
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}

}
