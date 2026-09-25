/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * A login authorization on a Mimer SQL user - a way to log in as that user other than by
 * password. Currently the only kind Mimer SQL supports is {@code OS_USER}: log in as the OS user
 * named here, no password needed. A user can have several authorizations, one row each. {@link
 * #getAuthorizationType} exists as a real column, not hardcoded display text, purely so a
 * future second authorization kind - Mimer SQL's own docs use the general term "authorization",
 * not "OS user", for this concept - needs no model change here, only a new value.
 * <p>
 * {@code INFORMATION_SCHEMA.EXT_IDENTS} has a separate row per {@code (IDENT_NAME, IDENT_LOGIN)}
 * pair, not one row per ident with a single login column - the same {@code IDENT_NAME} appears
 * once per OS-user authorization. {@link MimerDataSource.UserCache}'s own listing query accounts
 * for this to avoid showing a user with multiple authorizations as duplicate rows in the Users
 * folder.
 *
 * @author Mimer Information Technology
 */
public class MimerUserAuthorization implements DBSObject, DBPNamedObject2, DBPSaveableObject {

    static final String TYPE_OS_USER = "OS_USER";

    private final MimerUser user;
    private String osUser;
    private String authorizationType;
    private boolean persisted;

    public MimerUserAuthorization(@NotNull MimerUser user, @NotNull JDBCResultSet dbResult) {
        this.user = user;
        this.osUser = JDBCUtils.safeGetString(dbResult, "IDENT_LOGIN");
        this.authorizationType = TYPE_OS_USER;
        this.persisted = true;
    }

    public MimerUserAuthorization(@NotNull MimerUser user, @NotNull String osUser) {
        this.user = user;
        this.osUser = osUser;
        this.authorizationType = TYPE_OS_USER;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return osUser;
    }

    @Override
    public void setName(String name) {
        this.osUser = name;
    }

    @Property(viewable = true, order = 2)
    public String getAuthorizationType() {
        return authorizationType;
    }

    public void setAuthorizationType(@NotNull String authorizationType) {
        this.authorizationType = authorizationType;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public DBSObject getParentObject() {
        return user;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return user.getDataSource();
    }

    @NotNull
    public MimerUser getUser() {
        return user;
    }

    /**
     * {@code ALTER IDENT "user" ADD OS_USER 'osUser'}.
     */
    @NotNull
    public String buildAddDDL() {
        return "ALTER IDENT \"" + user.getName() + "\" ADD " + authorizationType + " '" + osUser.replace("'", "''") + "'";
    }

    /**
     * {@code ALTER IDENT "user" DROP OS_USER 'osUser'}.
     */
    @NotNull
    public String buildDropDDL() {
        return "ALTER IDENT \"" + user.getName() + "\" DROP " + authorizationType + " '" + osUser.replace("'", "''") + "'";
    }
}
