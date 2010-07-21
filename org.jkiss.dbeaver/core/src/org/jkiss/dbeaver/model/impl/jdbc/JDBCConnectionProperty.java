package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.DBPConnectionProperty;

import java.sql.DriverPropertyInfo;

/**
 * JDBCConnectionProperty
 */
public class JDBCConnectionProperty implements DBPConnectionProperty {

    private DriverPropertyInfo info;

    public JDBCConnectionProperty(DriverPropertyInfo info) {
        this.info = info;
    }

    public String getName() {
        return info.name;
    }

    public String getDescription() {
        return info.description;
    }

    public boolean isRequired() {
        return info.required;
    }

    public String getValue() {
        return info.value;
    }

    public String[] getChoices() {
        return info.choices;
    }
}
