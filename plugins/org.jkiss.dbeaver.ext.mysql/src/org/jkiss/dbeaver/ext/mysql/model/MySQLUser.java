/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * MySQLUser
 */
public class MySQLUser implements DBAUser, DBARole, DBPRefreshableObject, DBPSaveableObject
{
    private static final Log log = Log.getLog(MySQLUser.class);

    private MySQLDataSource dataSource;
    private String userName;
    private String host;
    private String passwordHash;

    private String sslType;
    private byte[] sslCipher;
    private byte[] x509Issuer;
    private byte[] x509Subject;

    private int maxQuestions;
    private int maxUpdates;
    private int maxConnections;
    private int maxUserConnections;

    private List<MySQLGrant> grants;
    private boolean persisted;

    public MySQLUser(MySQLDataSource dataSource, ResultSet resultSet) {
        this.dataSource = dataSource;
        if (resultSet != null) {
            this.persisted = true;
            this.userName = JDBCUtils.safeGetString(resultSet, "user");
            this.host = JDBCUtils.safeGetString(resultSet, "host");
            this.passwordHash = JDBCUtils.safeGetString(resultSet, "password");

            this.sslType = JDBCUtils.safeGetString(resultSet, "ssl_type");
            this.sslCipher = JDBCUtils.safeGetBytes(resultSet, "ssl_cipher");
            this.x509Issuer = JDBCUtils.safeGetBytes(resultSet, "x509_issuer");
            this.x509Subject = JDBCUtils.safeGetBytes(resultSet, "x509_subject");

            this.maxQuestions = JDBCUtils.safeGetInt(resultSet, "max_questions");
            this.maxUpdates = JDBCUtils.safeGetInt(resultSet, "max_updates");
            this.maxConnections = JDBCUtils.safeGetInt(resultSet, "max_connections");
            this.maxUserConnections = JDBCUtils.safeGetInt(resultSet, "max_user_connections");
        } else {
            this.persisted = false;
            this.userName = "";
            this.host = "%";
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return userName + "@" + host;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getFullName() {
        return "'" + userName + "'@'" + host + "'";
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
        DBUtils.fireObjectUpdate(this);
    }

    @Property(viewable = true, order = 2)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void clearGrantsCache()
    {
        this.grants = null;
    }

    public List<MySQLGrant> getGrants(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.grants != null) {
            return this.grants;
        }
        if (!isPersisted()) {
            this.grants = new ArrayList<>();
            return this.grants;
        }

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read catalog privileges")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW GRANTS FOR " + getFullName())) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<MySQLGrant> grants = new ArrayList<>();
                    while (dbResult.next()) {
                        List<MySQLPrivilege> privileges = new ArrayList<>();
                        boolean allPrivilegesFlag = false;
                        boolean grantOption = false;
                        String catalog = null;
                        String table = null;

                        String grantString = CommonUtils.notEmpty(JDBCUtils.safeGetString(dbResult, 1)).trim().toUpperCase(Locale.ENGLISH);
                        if (grantString.endsWith(" WITH GRANT OPTION")) {
                            grantOption = true;//privileges.add(getDataSource().getPrivilege(monitor, MySQLPrivilege.GRANT_PRIVILEGE));
                        }
                        String privString;
                        Matcher matcher = MySQLGrant.TABLE_GRANT_PATTERN.matcher(grantString);
                        if (matcher.find()) {
                            privString = matcher.group(1);
                            catalog = matcher.group(2);
                            table = matcher.group(3);
                        } else {
                            matcher = MySQLGrant.GLOBAL_GRANT_PATTERN.matcher(grantString);
                            if (matcher.find()) {
                                privString = matcher.group(1);
                            } else {
                                log.warn("Can't parse GRANT string: " + grantString);
                                continue;
                            }
                        }
                        StringTokenizer st = new StringTokenizer(privString, ",");
                        while (st.hasMoreTokens()) {
                            String privName = st.nextToken().trim();
                            if (privName.equalsIgnoreCase(MySQLPrivilege.ALL_PRIVILEGES)) {
                                allPrivilegesFlag = true;
                                continue;
                            }
                            MySQLPrivilege priv = getDataSource().getPrivilege(monitor, privName);
                            if (priv == null) {
                                log.warn("Can't find privilege '" + privName + "'");
                            } else {
                                privileges.add(priv);
                            }
                        }

                        grants.add(
                            new MySQLGrant(
                                this,
                                privileges,
                                catalog,
                                table,
                                allPrivilegesFlag,
                                grantOption));
                    }
                    this.grants = grants;
                    return this.grants;
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, getDataSource());
        }
    }

    public String getSslType() {
        return sslType;
    }

    void setSslType(String sslType) {
        this.sslType = sslType;
    }

    public byte[] getSslCipher() {
        return sslCipher;
    }

    void setSslCipher(byte[] sslCipher) {
        this.sslCipher = sslCipher;
    }

    public byte[] getX509Issuer() {
        return x509Issuer;
    }

    void setX509Issuer(byte[] x509Issuer) {
        this.x509Issuer = x509Issuer;
    }

    public byte[] getX509Subject() {
        return x509Subject;
    }

    void setX509Subject(byte[] x509Subject) {
        this.x509Subject = x509Subject;
    }

    public int getMaxQuestions() {
        return maxQuestions;
    }

    public void setMaxQuestions(int maxQuestions) {
        this.maxQuestions = maxQuestions;
    }

    public int getMaxUpdates() {
        return maxUpdates;
    }

    public void setMaxUpdates(int maxUpdates) {
        this.maxUpdates = maxUpdates;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxUserConnections() {
        return maxUserConnections;
    }

    public void setMaxUserConnections(int maxUserConnections) {
        this.maxUserConnections = maxUserConnections;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        grants = null;
        return this;
    }
}
