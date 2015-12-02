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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;

import java.util.*;

/**
 * Connection configuration.
 */
public class DBPConnectionConfiguration implements DBPObject
{
    private String hostName;
    private String hostPort;
    private String serverName;
    private String databaseName;
    private String userName;
    private String userPassword;
    private String url;
    private String clientHomeId;
    @NotNull
    private final Map<Object, Object> properties;
    @NotNull
    private final Map<DBPConnectionEventType, DBRShellCommand> events;
    @NotNull
    private final List<DBWHandlerConfiguration> handlers;
    private final DBPConnectionBootstrap bootstrap;
    private DBPConnectionType connectionType;
    private String connectionColor;

    public DBPConnectionConfiguration()
    {
        this.connectionType = DBPConnectionType.DEFAULT_TYPE;
        this.properties = new HashMap<>();
        this.events = new HashMap<>();
        this.handlers = new ArrayList<>();
        this.bootstrap = new DBPConnectionBootstrap();
    }

    public DBPConnectionConfiguration(@NotNull DBPConnectionConfiguration info)
    {
        this.hostName = info.hostName;
        this.hostPort = info.hostPort;
        this.serverName = info.serverName;
        this.databaseName = info.databaseName;
        this.userName = info.userName;
        this.userPassword = info.userPassword;
        this.url = info.url;
        this.clientHomeId = info.clientHomeId;
        this.connectionType = info.connectionType;
        this.properties = new HashMap<>(info.properties);
        this.events = new HashMap<>(info.events.size());
        for (Map.Entry<DBPConnectionEventType, DBRShellCommand> entry : info.events.entrySet()) {
            this.events.put(entry.getKey(), new DBRShellCommand(entry.getValue()));
        }
        this.handlers = new ArrayList<>(info.handlers.size());
        for (DBWHandlerConfiguration handler : info.handlers) {
            this.handlers.add(new DBWHandlerConfiguration(handler));
        }
        this.bootstrap = new DBPConnectionBootstrap(info.bootstrap);
    }

    public String getClientHomeId()
    {
        return clientHomeId;
    }

    public void setClientHomeId(String clientHomeId)
    {
        this.clientHomeId = clientHomeId;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

    public String getHostPort()
    {
        return hostPort;
    }

    public void setHostPort(String hostPort)
    {
        this.hostPort = hostPort;
    }

    public String getServerName()
    {
        return serverName;
    }

    public void setServerName(String serverName)
    {
        this.serverName = serverName;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserPassword()
    {
        return userPassword;
    }

    public void setUserPassword(@Nullable String userPassword)
    {
        this.userPassword = userPassword;
    }

    public Object getProperty(Object name)
    {
        return properties.get(name);
    }

    public void setProperty(Object name, Object value)
    {
        properties.put(name, value);
    }

    @NotNull
    public Map<Object, Object> getProperties()
    {
        return properties;
    }

    public void setProperties(@NotNull Map<Object, Object> properties)
    {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    public DBRShellCommand getEvent(DBPConnectionEventType eventType)
    {
        return events.get(eventType);
    }

    public void setEvent(DBPConnectionEventType eventType, DBRShellCommand command)
    {
        if (command == null) {
            events.remove(eventType);
        } else {
            events.put(eventType, command);
        }
    }

    public DBPConnectionEventType[] getDeclaredEvents()
    {
        Set<DBPConnectionEventType> eventTypes = events.keySet();
        return eventTypes.toArray(new DBPConnectionEventType[eventTypes.size()]);
    }

    public List<DBWHandlerConfiguration> getDeclaredHandlers()
    {
        return handlers;
    }

    public void setHandlers(@NotNull List<DBWHandlerConfiguration> handlers)
    {
        this.handlers.clear();
        this.handlers.addAll(handlers);
    }

    public void addHandler(DBWHandlerConfiguration handler)
    {
        for (int i = 0; i < handlers.size(); i++) {
            if (handlers.get(i).getId().equals(handler.getId())) {
                return;
            }
        }
        this.handlers.add(handler);
    }

    @Nullable
    public DBWHandlerConfiguration getHandler(String id)
    {
        for (DBWHandlerConfiguration handler : handlers) {
            if (handler.getId().equals(id)) {
                return handler;
            }
        }
        return null;
    }

    public DBPConnectionType getConnectionType()
    {
        return connectionType;
    }

    public void setConnectionType(DBPConnectionType connectionType)
    {
        this.connectionType = connectionType;
    }

    /**
     * Color in RGB format
     * @return RGB color or null
     */
    public String getConnectionColor()
    {
        return connectionColor;
    }

    public void setConnectionColor(String color)
    {
        this.connectionColor = color;
    }

    @NotNull
    public DBPConnectionBootstrap getBootstrap() {
        return bootstrap;
    }
}
