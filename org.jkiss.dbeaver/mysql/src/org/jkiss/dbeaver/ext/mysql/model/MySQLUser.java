/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

/**
 * MySQLUser
 */
public class MySQLUser implements DBAUser
{
    static final Log log = LogFactory.getLog(MySQLUser.class);

    private MySQLDataSource dataSource;
    private String username;
    private String host;
    private String passwordHash;

    private Map<String, Boolean> globalPrivileges;
    private List<String> catalogPrivNames;
    private Map<String, Map<String, Boolean>> catalogPrivileges;

    private String sslType;
    private byte[] sslCipher;
    private byte[] x509Issuer;
    private byte[] x509Subject;

    private int maxQuestions;
    private int maxUpdates;
    private int maxConnections;
    private int maxUserConnections;

    public MySQLUser(MySQLDataSource dataSource, ResultSet resultSet) {
        this.dataSource = dataSource;
        this.username = JDBCUtils.safeGetString(resultSet, "user");
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

        this.globalPrivileges = MySQLUtils.collectPrivileges(
            MySQLUtils.collectPrivilegeNames(resultSet),
            resultSet);
    }

    //@Property(name = "User name", viewable = true, order = 1)
    public String getName() {
        return username + "@" + host;
    }

    public String getUserName() {
        return username;
    }

    public String getDescription() {
        return null;
    }

    public DBSObject getParentObject() {
        return dataSource;
    }

    public JDBCDataSource getDataSource() {
        return dataSource;
    }

    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException {
        return false;
    }

    @Property(name = "Host mask", viewable = true, order = 2)
    public String getHost() {
        return host;
    }

    void setHost(String host) {
        this.host = host;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Map<String, Boolean> getGlobalPrivileges() {
        return globalPrivileges;
    }

    public List<String> getCatalogPrivNames(DBRProgressMonitor monitor)
        throws DBException
    {
        getCatalogPrivileges(monitor);
        return catalogPrivNames;
    }

    public Map<String, Map<String, Boolean>> getCatalogPrivileges(DBRProgressMonitor monitor)
        throws DBException
    {
        if (catalogPrivileges != null) {
            return catalogPrivileges;
        }
        JDBCExecutionContext context = getDataSource().openContext(monitor, "Read catalog privileges");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement("SELECT * FROM mysql.db WHERE User=?");
            try {
                dbStat.setString(1, getUserName());
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    if (catalogPrivNames == null) {
                        catalogPrivNames = MySQLUtils.collectPrivilegeNames(dbResult);
                    }
                    Map<String, Map<String, Boolean>> privs = new TreeMap<String, Map<String, Boolean>>();
                    while (dbResult.next()) {
                        privs.put(
                            dbResult.getString("db"),
                            MySQLUtils.collectPrivileges(catalogPrivNames, dbResult));
                    }
                    catalogPrivileges = privs;
                    return catalogPrivileges;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        }
        catch (SQLException e) {
            throw new DBException(e);
        }
        finally {
            context.close();
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

    void setMaxQuestions(int maxQuestions) {
        this.maxQuestions = maxQuestions;
    }

    public int getMaxUpdates() {
        return maxUpdates;
    }

    void setMaxUpdates(int maxUpdates) {
        this.maxUpdates = maxUpdates;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxUserConnections() {
        return maxUserConnections;
    }

    void setMaxUserConnections(int maxUserConnections) {
        this.maxUserConnections = maxUserConnections;
    }

}