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
package org.jkiss.dbeaver.ext.ui.eula;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class EULAHandler extends AbstractHandler {
    private static final String EULA_PATH = Platform.getProduct().getProperty("licenseFile");
    private static final Log log = Log.getLog(EULAHandler.class);

    public static void showEula(IWorkbenchWindow window) {
        String eula = getPackageEula();
        showEulaDialog(window, eula);
    }

    private static void showEulaDialog(IWorkbenchWindow window, String eula) {
        EULADialog eulaDialog = new EULADialog(window.getShell(), eula);
        eulaDialog.open();

    }

    private static String getPackageEula() {
        StringBuilder eula = new StringBuilder();
        URL url;
        try {
            url = FileLocator.find(new URL(EULA_PATH));
        } catch (MalformedURLException e) {
            log.error(e);
            return null;
        }
        if (url != null) {
            try (BufferedReader fileReader = new BufferedReader(new FileReader(url.getFile()))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    eula.append(line).append('\n');
                }
            } catch (IOException e) {
                log.error(e);
                return null;
            }
            return eula.toString();
        } else {
            return null;
        }
    }



    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        showEula(HandlerUtil.getActiveWorkbenchWindow(event));
        return null;
    }
}
