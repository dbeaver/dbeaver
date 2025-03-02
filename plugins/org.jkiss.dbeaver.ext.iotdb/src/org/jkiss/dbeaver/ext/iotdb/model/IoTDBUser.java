package org.jkiss.dbeaver.ext.iotdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class IoTDBUser implements DBAUser, DBARole, DBPRefreshableObject, DBPSaveableObject, DBPQualifiedObject {

    private static final Log log = Log.getLog(IoTDBUser.class);

    private final IoTDBDataSource dataSource;
    private String userName;
    private String host;
    private boolean persisted;
    private List<IoTDBGrant> grants;

    public IoTDBUser(IoTDBDataSource dataSource, ResultSet resultSet) {
        this.dataSource = dataSource;
        if (resultSet != null) {
            try {
                this.persisted = true;
                this.userName = resultSet.getString("User");
                this.host = "localhost";
            } catch (Exception e) {
                log.error("Error loading user", e);
            }
        }
        else {
            this.persisted = false;
            this.userName = "";
            this.host = "";
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<IoTDBGrant> getGrants(DBRProgressMonitor monitor) throws DBException {
        if (this.grants != null) {
            return this.grants;
        }
        if (!isPersisted()) {
            this.grants = new ArrayList<>();
            return this.grants;
        }

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Grants")) {
            String sql = String.format("list privileges of user %s", userName);
            try (JDBCStatement stmt = session.createStatement()) {
                try (JDBCResultSet rs = stmt.executeQuery(sql)) {
                    List<IoTDBGrant> grants = new ArrayList<>();
                    while (rs.next()) {
                        List<IoTDBPrivilege> privileges = new ArrayList<>();
                        if (rs.getBoolean("GrantOption")) {
                            privileges.add(new IoTDBPrivilege(dataSource, rs.getString("Privileges")));
                            String role = rs.getString("Role");
                            String scope = rs.getString("Scope");
                            grants.add(new IoTDBGrant(this, privileges, role, scope));
                        }
                    }
                    this.grants = grants;
                    return this.grants;
                }
            }
        } catch (Exception e) {
            log.error("Error loading privileges", e);
            throw new DBDatabaseException(e, this.getDataSource());
        }
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext dbpEvaluationContext) {
        return "";
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor dbrProgressMonitor) throws DBException {
        grants = null;
        return null;
    }

    @Override
    public void setPersisted(boolean b) {
        this.persisted = b;
        DBUtils.fireObjectUpdate(this);
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return userName;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }
}
