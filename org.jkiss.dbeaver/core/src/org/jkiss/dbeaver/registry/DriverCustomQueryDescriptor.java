/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPDriverCustomQuery;

/**
 * DriverCustomQueryDescriptor
 */
public class DriverCustomQueryDescriptor implements DBPDriverCustomQuery
{

    static final Log log = LogFactory.getLog(DriverCustomQueryDescriptor.class);

    private String name;
    private String query;

    public DriverCustomQueryDescriptor(IConfigurationElement config)
    {
        this.name = config.getAttribute("label");
        if (this.name == null) {
            this.name = "#";
        }
        this.query = config.getValue();
    }

    public String getName()
    {
        return name;
    }

    public String getQuery()
    {
        return query;
    }

}