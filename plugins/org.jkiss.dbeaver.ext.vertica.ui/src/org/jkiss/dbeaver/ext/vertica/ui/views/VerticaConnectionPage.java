/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.vertica.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.generic.views.GenericConnectionPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;

public class VerticaConnectionPage extends GenericConnectionPage {

    private final Image logoImage;

    public VerticaConnectionPage() {
        logoImage = createImage("icons/vertica_logo.png"); //$NON-NLS-1$
    }

    @Override
    public void dispose() {
        super.dispose();
        UIUtils.dispose(logoImage);
    }

    @Override
    public Image getImage() {
        return logoImage;
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[] {
            new VerticaConnectionPageAdvanced(),
            new DriverPropertiesDialogPage(this)
        };
    }
}
