/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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