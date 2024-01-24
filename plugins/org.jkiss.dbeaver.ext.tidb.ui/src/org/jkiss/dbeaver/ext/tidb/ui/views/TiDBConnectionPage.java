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

package org.jkiss.dbeaver.ext.tidb.ui.views;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.mysql.ui.views.MySQLConnectionPage;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * TiDBConnectionPage
 */
public class TiDBConnectionPage extends MySQLConnectionPage {
    private final Image LOGO_TIDB;

    public TiDBConnectionPage() {
        LOGO_TIDB = createImage("icons/tidb_logo.png");
    }

    @Override
    public void dispose()
    {
        super.dispose();
        UIUtils.dispose(LOGO_TIDB);
    }

    @Override
    public Image getImage() {
        return LOGO_TIDB;
    }
}
