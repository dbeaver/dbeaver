package org.jkiss.dbeaver.ext.generic.model;

public class ManticoreSQLDialect extends GenericSQLDialect {
    public ManticoreSQLDialect() {
        super();
    }

    @Override
    public boolean supportsAliasInSelect() {
        return false;
    }
}
