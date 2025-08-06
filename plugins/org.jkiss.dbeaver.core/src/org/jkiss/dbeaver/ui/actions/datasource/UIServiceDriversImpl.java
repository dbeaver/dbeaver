/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.actions.datasource;

import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.ui.UIServiceDrivers;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverDownloadDialog;

/**
 * UIServiceDriversImpl
 */
public class UIServiceDriversImpl implements UIServiceDrivers {

    @Override
    public boolean downloadDriverFiles(DBRProgressMonitor monitor, DBPDriver driver, DBPDriverDependencies dependencies) {
        return new UITask<Boolean>() {
            @Override
            protected Boolean runTask() {
                return DriverDownloadDialog.downloadDriverFiles(null, driver, dependencies);
            }
        }.execute();
    }

}
