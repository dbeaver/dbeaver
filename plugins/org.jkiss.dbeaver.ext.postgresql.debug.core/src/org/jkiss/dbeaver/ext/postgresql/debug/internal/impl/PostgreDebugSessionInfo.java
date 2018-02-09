/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ext.postgresql.debug.internal.impl;

import java.util.HashMap;
import java.util.Map;

import org.jkiss.dbeaver.debug.DBGSessionInfo;

@SuppressWarnings("nls")
public class PostgreDebugSessionInfo implements DBGSessionInfo {

    public static final String QUERY_PROP = "query";

    public static final String STATE_PROP = "state";

    public static final String APP_PROP = "application";

    public static final String USER_PROP = "user";

    public static final String PID = "pid";

    public static final String CREATE_LISTEN = "CREATE LISTEN";

    final int pid;
    final String user;
    final String application;
    final String state;
    final String query;

    public PostgreDebugSessionInfo(int pid, String user, String application, String state, String query) {
        super();
        this.pid = pid;
        this.user = user;
        this.application = application;
        this.state = state;
        this.query = query;
    }

    @Override
    public Integer getID() {
        return pid;
    }
    
    @Override
    public String getTitle() {
        return getApplication();
    }

    public int getPid() {
        return pid;
    }

    public String getUser() {
        return user;
    }

    public String getApplication() {
        return application;
    }

    public String getState() {
        return state;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PID, pid);
        map.put(USER_PROP, user);
        map.put(APP_PROP, application);
        map.put(STATE_PROP, state);
        map.put(QUERY_PROP, query);
        return map;
    }

    @Override
    public String toString() {

        return "pid:" + String.valueOf(pid) + ", user: " + user + ", application: `" + application + "`, state: "
                + state + ", query: " + query.replace('\n', '\\');
    }

}
