package org.jkiss.dbeaver.ext.iotdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

public class IoTDBDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(IoTDBDataSource.class);

    private List<IoTDBUser> users;

    public IoTDBDataSource(DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container, @NotNull GenericMetaModel metaModel)
            throws DBException {
        super(monitor, container, metaModel, new GenericSQLDialect());
    }

    /**
     * Get the list of users
     * @param monitor progress monitor
     * @return List of IoTDBUser
     * @throws DBException if an error occurs
     */
    public List<IoTDBUser> getUsers(DBRProgressMonitor monitor) throws DBException {
        if (users == null) {
            users = loadUsers(monitor);
        }
        return users;
    }

    /**
     * Load users from the database
     * @param monitor progress monitor
     * @return List of IoTDBUser
     * @throws DBException if an error occurs
     */
    private List<IoTDBUser> loadUsers(DBRProgressMonitor monitor) throws DBException {

        List<IoTDBUser> userList = new ArrayList<>();
        String currentUserName = null;
        boolean hasManageUserPrivilege = false;

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Show Current User & Check Privileges")) {
            String sql = "show current_user";
            try (JDBCStatement stmt = session.createStatement()) {
                try (JDBCResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        currentUserName = rs.getString("CurrentUser");
                        IoTDBUser user = new IoTDBUser(this, currentUserName);
                        userList.add(user);
                    }

                    sql = "list privileges of user " + currentUserName;
                    try (JDBCStatement stmt2 = session.createStatement()) {
                        try (JDBCResultSet rs2 = stmt2.executeQuery(sql)) {
                            while (rs2.next()) {
                                if (rs2.getString("Privileges").equals("MANAGE_USER")) {
                                    hasManageUserPrivilege = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error showing current user or checking privileges", e);
            throw new DBDatabaseException(e, this);
        }

        if (!hasManageUserPrivilege) {
            return userList;
        }

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Users")) {
            String sql = "list user";
            try (JDBCStatement stmt = session.createStatement()) {
                try (JDBCResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        String tmpUserName = rs.getString("User");
                        if (tmpUserName.equals(currentUserName)) {
                            continue;
                        }
                        IoTDBUser user = new IoTDBUser(this, tmpUserName);
                        userList.add(user);
                    }
                    return userList;
                }
            }
        } catch (Exception e) {
            log.error("Error loading users", e);
            throw new DBDatabaseException(e, this);
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
            throws DBException {
        super.refreshObject(monitor);

        this.users = loadUsers(monitor);
        return this;
    }
}
