package org.jkiss.dbeaver.ext.gaussdb.model;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerExtensionBase;

public class PostgreServerGaussDB extends PostgreServerExtensionBase {

    private boolean supportJobs;

    protected PostgreServerGaussDB(PostgreDataSource dataSource) {
        super(dataSource);
        this.supportJobs = false;
    }

    @Override
    public String getServerTypeName() {
        return "GaussDB";
    }

    @Override
    public boolean supportsNativeClient() {
        return false;
    }

    public boolean isSupportJobs() {
        return supportJobs;
    }

    public void setSupportJobs(boolean supportJobs) {
        this.supportJobs = supportJobs;
    }

    @Override
    public boolean supportsExtensions() {
        return false;
    }

    @Override
    public boolean supportsStoredProcedures() {
        return true;
    }

    @Override
    public PostgreDatabase.SchemaCache createSchemaCache(PostgreDatabase database) {
        return new GaussDBSchemaCache();
    }
}