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
package org.jkiss.dbeaver.ext.mimer.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.jkiss.dbeaver.ext.generic.views.GenericConnectionPage;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;

/**
 * Mimer SQL connection wizard page. Reuses {@link GenericConnectionPage} as-is for
 * Host/Port/Database/auth, adding a "Mimer SQL" extra page (see
 * {@link MimerConnectionSettingsPage}: TCP/IP-vs-Local protocol, PROGRAM security)
 * alongside the usual Driver properties page.
 *
 * @author Mimer Information Technology
 */
public class MimerConnectionPage extends GenericConnectionPage {

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[]{
            new MimerConnectionSettingsPage(),
            new DriverPropertiesDialogPage(this)
        };
    }
}
