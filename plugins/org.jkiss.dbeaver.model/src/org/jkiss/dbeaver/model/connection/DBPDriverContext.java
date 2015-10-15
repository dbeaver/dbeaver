/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.*;

/**
 * Driver resolve context
 */
public class DBPDriverContext implements AutoCloseable {

    static final Log log = Log.getLog(DBPDriverContext.class);

    private final DBRProgressMonitor monitor;
    private final Date initTime = new Date();
    private final Map<String, String> properties = new HashMap<>();
    private final Map<Class, AutoCloseable> infoMap = new HashMap<>();

    public DBPDriverContext(DBRProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public DBRProgressMonitor getMonitor() {
        return monitor;
    }

    public Date getInitTime() {
        return initTime;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @NotNull
    public <T extends AutoCloseable> T getInfo(Class<T> type) {
        AutoCloseable o = infoMap.get(type);
        if (o == null) {
            try {
                o = type.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Can't create context info " + type.getName(), e);
            }
            infoMap.put(type, o);
        }
        return type.cast(o);
    }

    @Override
    public void close() {
        for (AutoCloseable info : infoMap.values()) {
            try {
                info.close();
            } catch (Exception e) {
                log.error(e);
            }
        }
    }
}
