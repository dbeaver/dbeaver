/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.nio.file.Path;


public class NavigatorHandlerShowInExplorer extends NavigatorHandlerObjectBase {
    private static final Log log = Log.getLog(NavigatorHandlerShowInExplorer.class);

    private static final String windowsAppLocalDataPackage = "DBeaverCorp.DBeaverCE_1b7tdvn0p0f9y";
    private static final String appDataRoamingPathString = System.getenv("AppData"); 
    private static final String localAppDataPathString = System.getenv("LOCALAPPDATA");
    private static final String userHomePathString = System.getProperty("user.home");
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IStructuredSelection structSelection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
        final Object element = structSelection.getFirstElement();
        if (element instanceof DBNResource) {
            final IResource resource = ((DBNResource) element).getResource();
            if (resource != null) {
                IPath location = resource.getLocation();
                if (location != null) {
                    String filePath = location.toString();
                    if (RuntimeUtils.isWindowsStoreApplication() && filePath.startsWith(appDataRoamingPathString)) {
                        filePath = makeVirtualizedPath(filePath);
                    }
                    ShellUtils.showInSystemExplorer(filePath);
                }
            }
        }
        return null;
    }

    private String makeVirtualizedPath(String filePath) {
        Path localAppDataPath = localAppDataPathString != null
            ? Path.of(localAppDataPathString) 
            : Path.of(userHomePathString, "AppData", "Local");
        
        Path virtualizedRoot = localAppDataPath.resolve("Packages").resolve(windowsAppLocalDataPackage).resolve("LocalCache").resolve("Roaming");
        Path remappedPath = virtualizedRoot.resolve(Path.of(appDataRoamingPathString).relativize(Path.of(filePath)));
        String resultPath = remappedPath.toString();
        
        log.warn("Remapping file path [" + filePath + "] to [" + resultPath + "]");
        return resultPath;
    }
}
