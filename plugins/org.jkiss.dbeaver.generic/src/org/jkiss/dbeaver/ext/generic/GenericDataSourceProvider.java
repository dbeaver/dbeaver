/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.DBPProperty;
import org.jkiss.dbeaver.model.DBPPropertyGroup;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.PropertyDescriptor;
import org.jkiss.dbeaver.registry.PropertyGroupDescriptor;

import java.util.Arrays;
import java.util.List;

public class GenericDataSourceProvider extends JDBCDataSourceProvider {

    public GenericDataSourceProvider()
    {
    }

    public long getFeatures()
    {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @Override
    public List<? extends DBPPropertyGroup> getDriverProperties(DBPDriver driver) throws DBException
    {
        PropertyGroupDescriptor paramsGroup = new PropertyGroupDescriptor("Parameters", "Custom driver parameters");
        paramsGroup.addProperty(new PropertyDescriptor(
            paramsGroup,
            GenericConstants.PARAM_META_CASE,
            "Metadata case",
            "Metadata identifiers' case",
            DBPProperty.PropertyType.STRING,
            false,
            GenericConstants.MetaCase.NONE.name(),
            new String[] {GenericConstants.MetaCase.NONE.name(), GenericConstants.MetaCase.LOWER.name(), GenericConstants.MetaCase.UPPER.name()} ));
        paramsGroup.addProperty(new PropertyDescriptor(
            paramsGroup,
            GenericConstants.PARAM_SHUTDOWN_URL_PARAM,
            "Shutdown parameter",
            "Database shutdown URL parameter",
            DBPProperty.PropertyType.STRING,
            false,
            "",
            null));

        PropertyGroupDescriptor queriesGroup = new PropertyGroupDescriptor("Queries", "Custom driver queries");
        queriesGroup.addProperty(new PropertyDescriptor(
            queriesGroup,
            GenericConstants.PARAM_QUERY_GET_ACTIVE_DB,
            "Get active database",
            "Query to obtain active database name",
            DBPProperty.PropertyType.STRING,
            false,
            "",
            null));
        queriesGroup.addProperty(new PropertyDescriptor(
            queriesGroup,
            GenericConstants.PARAM_QUERY_GET_ACTIVE_DB,
            "Set active database",
            "Query to change active database",
            DBPProperty.PropertyType.STRING,
            false,
            "",
            null));

        return Arrays.asList(paramsGroup, queriesGroup);
    }

    public DBPDataSource openDataSource(
        DBRProgressMonitor monitor,
        DBSDataSourceContainer container)
        throws DBException
    {
        return new GenericDataSource(container);
    }

}
