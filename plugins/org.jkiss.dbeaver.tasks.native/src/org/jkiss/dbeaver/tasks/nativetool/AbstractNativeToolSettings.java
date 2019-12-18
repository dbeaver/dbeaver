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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;

public abstract class AbstractNativeToolSettings<BASE_OBJECT extends DBSObject> extends AbstractToolSettings<BASE_OBJECT> {

    private static final Log log = Log.getLog(AbstractNativeToolSettings.class);

    private final String PROP_NAME_EXTRA_ARGS = "tools.wizard." + getClass().getSimpleName() + ".extraArgs";

    private DBPNativeClientLocation clientHome;
    private DBPConnectionConfiguration connectionInfo;
    private String toolUserName;
    private String toolUserPassword;
    private String extraCommandArgs;

    public DBPNativeClientLocation getClientHome() {
        return clientHome;
    }

    public void setClientHome(DBPNativeClientLocation clientHome) {
        this.clientHome = clientHome;
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
        connectionInfo = getDataSourceContainer() == null ? null : getDataSourceContainer().getActualConnectionConfiguration();

        extraCommandArgs = preferenceStore.getString(PROP_NAME_EXTRA_ARGS);
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) {
        super.saveSettings(runnableContext, preferenceStore);
        preferenceStore.setValue(PROP_NAME_EXTRA_ARGS, extraCommandArgs);
    }

}
