package org.jkiss.dbeaver.ext.exasol.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolRole implements DBARole {

    private static final Log log = Log.getLog(ExasolRole.class);

    private String name;
    private String description;
    private ExasolDataSource dataSource;
    private Boolean adminOption;

    public ExasolRole(ExasolDataSource dataSource, ResultSet resultSet) {
        this.name = JDBCUtils.safeGetString(resultSet, "ROLE_NAME");
        this.description = JDBCUtils.safeGetStringTrimmed(resultSet, "ROLE_COMMENT");
        adminOption = JDBCUtils.safeGetBoolean(resultSet, "ADMIN_OPTION");
        
        this.dataSource = dataSource;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 10)
    public String getDescription()
    {
        return description;
    }

	@Override
	public DBSObject getParentObject() {
		// TODO Auto-generated method stub
		return dataSource.getContainer();
	}

	@Override
	public DBPDataSource getDataSource() {
		return dataSource;
	}

	@Override
	public boolean isPersisted() {
		return true;
	}

}

	

