package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.prop.DBPProperty;
import org.jkiss.dbeaver.model.prop.DBPPropertyGroup;

import java.sql.DriverPropertyInfo;

/**
 * JDBCConnectionProperty
 */
public class JDBCConnectionProperty implements DBPProperty {

    private DBPPropertyGroup group;
    private DriverPropertyInfo info;

    public JDBCConnectionProperty(DBPPropertyGroup group, DriverPropertyInfo info) {
        this.group = group;
        this.info = info;
    }

    public DBPPropertyGroup getGroup() {
        return group;
    }

    public String getName() {
        return info.name;
    }

    public String getDescription() {
        return info.description;
    }

    public PropertyType getType() {
        return PropertyType.STRING;
    }

    public boolean isRequired() {
        return info.required;
    }

    public String getDefaultValue() {
        return info.value;
    }

    public String[] getValidValues() {
        return info.choices;
    }
}
