/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.exasol.model.security;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.ExasolUserType;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;


public class ExasolUser extends ExasolGrantee
		implements DBAUser,  DBPSaveableObject, DBPNamedObject2, DBPRefreshableObject {


	private String userName;
	private String description;
	private String dn;
	private String password;
	private Timestamp created;
	private String kerberosPrincipal;
	private String passwordState;
	private String passwordStateChanged;
	private Timestamp passwordExpiry; 
	private BigDecimal passwordExpiryDays;
	private BigDecimal passwordExpiryGrace;
	private String passwordExpiryPolicy;
	private BigDecimal failedLoginAttempts;
	private Boolean locked;
	private ExasolUserType type;
	
	
	public ExasolUser(ExasolDataSource dataSource, ResultSet resultSet)
	{
		super(dataSource, resultSet);
		if (resultSet != null) {
			this.userName = JDBCUtils.safeGetString(resultSet, "USER_NAME");
			this.description = JDBCUtils.safeGetString(resultSet,
					"USER_COMMENT");
			this.dn = JDBCUtils.safeGetString(resultSet, "DISTINGUISHED_NAME");
			this.password = JDBCUtils.safeGetString(resultSet, "PASSWORD");
			this.created = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");
			this.kerberosPrincipal = JDBCUtils.safeGetString(resultSet, "KERBEROS_PRINCIPAL");
			this.passwordState = JDBCUtils.safeGetString(resultSet, "PASSWORD_STATE");
			this.passwordStateChanged = JDBCUtils.safeGetString(resultSet, "PASSWORD_STATE_CHANGED");
			this.passwordExpiry = JDBCUtils.safeGetTimestamp(resultSet, "PASSWORD_EXPIRY");
			this.passwordExpiryDays = JDBCUtils.safeGetBigDecimal(resultSet, "PASSWORD_EXPIRY_DAYS");
			this.passwordExpiryGrace = JDBCUtils.safeGetBigDecimal(resultSet, "PASSWORD_GRACE_DAYS");
			this.passwordExpiryPolicy = JDBCUtils.safeGetString(resultSet, "PASSWORD_EXPIRY_POLICY");
			this.failedLoginAttempts = JDBCUtils.safeGetBigDecimal(resultSet, "FAILED_LOGIN_ATTEMPTS");
			
			if (CommonUtils.isEmpty(kerberosPrincipal) & CommonUtils.isEmpty(dn))
			{
				this.type = ExasolUserType.LOCAL;
			} else if (CommonUtils.isEmpty(kerberosPrincipal))
			{
				this.type = ExasolUserType.LDAP;
			} else {
				this.type = ExasolUserType.KERBEROS;
			}
			
			if (this.passwordState != null && this.passwordState.equals("EXPIRED"))
			{
				this.locked = true;
			} else {
				this.locked = false;
			}
			
		} else {
			this.userName = "user";
			this.description = "";
			this.dn = "";
			this.password = "";
			this.created = null;
			this.kerberosPrincipal = "";
			this.type = ExasolUserType.LOCAL;
					
		}
		
	}
	
	@Property(viewable = true, updatable=true, editable=true, order = 35)
	public String getKerberosPrincipal() {
		return kerberosPrincipal;
	}

	public void setKerberosPrincipal(String kerberosPrincipal) {
		this.kerberosPrincipal = kerberosPrincipal;
		this.password = null;
		this.dn = null;
		this.type = ExasolUserType.KERBEROS;
	}

	@Property(viewable = true, order = 50)
	public String getPasswordState() {
		return passwordState;
	}

	public void setPasswordState(String passwordState) {
		this.passwordState = passwordState;
	}

	@Property(viewable = true, order = 60)
	public String getPasswordExpiryPolicy() {
		return passwordExpiryPolicy;
	}

	public void setPasswordExpiryPolicy(String passwordExpiryPolicy) {
		this.passwordExpiryPolicy = passwordExpiryPolicy;
	}

	public String getUserName() {
		return userName;
	}

	@Property(viewable = true, order = 70)
	public String getPasswordStateChanged() {
		return passwordStateChanged;
	}

	@Property(viewable = true, order = 80)
	public Timestamp getPasswordExpiry() {
		return passwordExpiry;
	}

	@Property(viewable = true, order = 90)
	public BigDecimal getPasswordExpiryDays() {
		return passwordExpiryDays;
	}

	@Property(viewable = true, order = 100)
	public BigDecimal getPasswordExpiryGrace() {
		return passwordExpiryGrace;
	}

	@Property(viewable = true, order = 110)
	public BigDecimal getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	@Property(viewable = true, updatable=true, editable=true, order = 120)
	public Boolean getLocked() {
		return locked;
	}

	public void setLocked(Boolean locked) {
		this.locked = locked;
	}
	

	public ExasolUser(ExasolDataSource datasource, String name, String description, String dn, String password, String kerberosPrincipal, ExasolUserType type)
	{
		super(datasource, false);
		this.userName = name;
		this.description = description;
		this.dn = dn;
		this.password = password;
		this.kerberosPrincipal = kerberosPrincipal;
		this.type =  type;
	}

	@Override
	@Property(viewable = true, updatable=true, editable=true, multiline = true, order = 150)
	public String getDescription()
	{
		return this.description;
	}
	
	@Property(viewable = true, editable=true, updatable=true, order = 20)
	public String getPassword()
	{
		return this.password;
	}

	@Property(viewable = true, editable=true, updatable=true, order = 30)
	public String getDn()
	{
		return this.dn;
	}

	@Property(viewable = true, order = 50)
	public Timestamp getCreated()
	{
		return this.created;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 1)
	public String getName()
	{
		return this.userName;
	}

	@Override
	public void setName(String newName)
	{
		this.userName = newName;
	}
	
	public void setPassword(String newPassword)
	{
		this.password = newPassword;
		this.dn = null;
		this.kerberosPrincipal = null;
		this.type = ExasolUserType.LOCAL;
	}
	
	public void setDN(String dn)
	{
		this.dn = dn;
		this.password = null;
		this.kerberosPrincipal = null;
		this.type = ExasolUserType.LDAP;
	}
	
	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor)
			throws DBException
	{
		return this;
	}
	
	public ExasolUserType getType()
	{
		return type;
	}
	
	@Override
	public String toString()
	{
		return "User " + getName();
	}

}
