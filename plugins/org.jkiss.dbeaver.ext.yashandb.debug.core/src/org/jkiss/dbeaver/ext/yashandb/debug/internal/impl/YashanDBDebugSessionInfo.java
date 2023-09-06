
package org.jkiss.dbeaver.ext.yashandb.debug.internal.impl;

import org.jkiss.dbeaver.debug.DBGSessionInfo;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("nls")
public class YashanDBDebugSessionInfo implements DBGSessionInfo {

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

    public YashanDBDebugSessionInfo(int pid, String user, String application, String state, String query) {
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
