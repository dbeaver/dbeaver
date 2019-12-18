/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.tasks.nativetool;

import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

public abstract class AbstractNativeToolSettings<BASE_OBJECT extends DBSObject> extends AbstractToolSettings<BASE_OBJECT> {

    private final String PROP_NAME_EXTRA_ARGS = "tools.wizard." + getClass().getSimpleName() + ".extraArgs";

    private DBPNativeClientLocation clientHome;
    private Writer logWriter = new PrintWriter(System.out, true);

    private String clientHomeName;

    private String toolUserName;
    private String toolUserPassword;
    private String extraCommandArgs;

    protected DBPNativeClientLocation findNativeClientHome(String clientHomeId) {
        return null;
    }

    public Writer getLogWriter() {
        return logWriter;
    }

    public void setLogWriter(Writer logWriter) {
        this.logWriter = logWriter;
    }

    public String getClientHomeName() {
        return clientHomeName;
    }

    public DBPNativeClientLocation getClientHome() {
        return clientHome;
    }

    public void setClientHome(DBPNativeClientLocation clientHome) {
        this.clientHome = clientHome;
        this.clientHomeName = clientHome == null ? null : clientHome.getName();
    }

    public String getToolUserName() {
        return toolUserName;
    }

    public void setToolUserName(String toolUserName) {
        this.toolUserName = toolUserName;
    }

    public String getToolUserPassword() {
        return toolUserPassword;
    }

    public void setToolUserPassword(String toolUserPassword) {
        this.toolUserPassword = toolUserPassword;
    }

    public String getExtraCommandArgs() {
        return extraCommandArgs;
    }

    public void setExtraCommandArgs(String extraCommandArgs) {
        this.extraCommandArgs = extraCommandArgs;
    }

    public void addExtraCommandArgs(List<String> cmd) {
        if (!CommonUtils.isEmptyTrimmed(extraCommandArgs)) {
            Collections.addAll(cmd, extraCommandArgs.split(" "));
        }
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) {
        super.loadSettings(runnableContext, preferenceStore);

        extraCommandArgs = preferenceStore.getString(PROP_NAME_EXTRA_ARGS);
        clientHomeName = preferenceStore.getString("clientHomeName");
        toolUserName  = preferenceStore.getString("toolUserName");
        toolUserPassword = preferenceStore.getString("toolUserPassword");
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) {
        super.saveSettings(runnableContext, preferenceStore);
        preferenceStore.setValue(PROP_NAME_EXTRA_ARGS, extraCommandArgs);
        if (clientHomeName != null) {
            preferenceStore.setValue("clientHomeName", clientHomeName);
        }
        if (!CommonUtils.isEmpty(toolUserName)) {
            preferenceStore.setValue("toolUserName", toolUserName);
        } else {
            preferenceStore.setToDefault("toolUserName");
        }
        if (!CommonUtils.isEmpty(toolUserPassword)) {
            preferenceStore.setValue("toolUserPassword", toolUserPassword);
        } else {
            preferenceStore.setToDefault("toolUserPassword");
        }
    }

}
