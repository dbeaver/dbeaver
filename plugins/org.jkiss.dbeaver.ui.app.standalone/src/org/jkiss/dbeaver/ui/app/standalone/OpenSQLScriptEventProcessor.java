/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

public class OpenSQLScriptEventProcessor implements Listener {
    private static final Log log = Log.getLog(OpenSQLScriptEventProcessor.class);

    private final Collection<String> pathsOfFilesToOpen = new ArrayList<>(1);

    @Override
    public void handleEvent(Event event) {
        if (GeneralUtils.isMacOS() && event.text != null && event.text.endsWith(".sql")) {
            pathsOfFilesToOpen.add(event.text);
        }
    }

    void openFiles() {
        if (pathsOfFilesToOpen.isEmpty()) {
            return;
        }
        try {
            DBeaverApplication.getInstance().getInstanceServer().openExternalFiles(pathsOfFilesToOpen.toArray(new String[0]));
        } catch (RemoteException e) {
            log.warn("Unable to open sql scripts", e);
        }
        pathsOfFilesToOpen.clear();
    }
}
