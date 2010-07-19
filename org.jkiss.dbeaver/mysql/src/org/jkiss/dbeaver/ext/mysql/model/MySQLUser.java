/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.meta.AbstractTrigger;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.DBSCatalog;
import org.jkiss.dbeaver.model.struct.DBSManipulationType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

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

    private Map<String, Boolean> globalPrivileges = new TreeMap<String, Boolean>();

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

        // Now collect all privileges columns
        try {
            ResultSetMetaData rsMetaData = resultSet.getMetaData();
            int colCount = rsMetaData.getColumnCount();
            for (int i = 0; i < colCount; i++) {
                String colName = rsMetaData.getColumnName(i + 1);
                if (colName.toLowerCase().endsWith("_priv")) {
                    globalPrivileges.put(colName.substring(0, colName.length() - 5), "Y".equals(JDBCUtils.safeGetString(resultSet, colName)));
                }
            }
        } catch (SQLException e) {
            log.debug(e);
        }
    }

    //@Property(name = "User name", viewable = true, order = 1)
    public String getName() {
        return username;
    }

    public String getDescription() {
        return null;
    }

    public DBSObject getParentObject() {
        return dataSource;
    }

    public DBPDataSource getDataSource() {
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