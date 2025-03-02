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
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Users")) {
            String sql = "list user";
            try (JDBCStatement stmt = session.createStatement()) {
                try (JDBCResultSet rs = stmt.executeQuery(sql)) {
                    List<IoTDBUser> userList = new ArrayList<>();
                    while (rs.next()) {
                        IoTDBUser user = new IoTDBUser(this, rs);
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
}
