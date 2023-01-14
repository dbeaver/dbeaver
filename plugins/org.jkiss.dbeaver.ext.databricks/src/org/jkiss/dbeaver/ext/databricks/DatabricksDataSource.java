package org.jkiss.dbeaver.ext.databricks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class DatabricksDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(DatabricksDataSource.class);

    public DatabricksDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel) throws DBException {
        super(monitor, container, metaModel, new DatabricksSQLDialect());
    }



}
