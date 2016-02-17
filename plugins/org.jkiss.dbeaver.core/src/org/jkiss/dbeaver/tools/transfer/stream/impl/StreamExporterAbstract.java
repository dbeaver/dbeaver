/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.tools.transfer.stream.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;

/**
 * Abstract Exporter
 */
public abstract class StreamExporterAbstract implements IStreamDataExporter {

    private IStreamDataExporterSite site;

    public IStreamDataExporterSite getSite()
    {
        return site;
    }

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        this.site = site;
    }

    @Override
    public void dispose()
    {
        // do nothing
    }

    protected String getValueDisplayString(
        DBDAttributeBinding column,
        Object value)
    {
        final DBDValueHandler valueHandler = column.getValueHandler();
        return valueHandler.getValueDisplayString(column, value, getSite().getExportFormat());
    }


}