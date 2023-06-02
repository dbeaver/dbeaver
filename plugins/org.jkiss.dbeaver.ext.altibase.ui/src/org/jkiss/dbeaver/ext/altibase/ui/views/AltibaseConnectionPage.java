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
package org.jkiss.dbeaver.ext.altibase.ui.views;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.generic.views.GenericConnectionPage;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Altibase ConnectionPage
 */
public class AltibaseConnectionPage extends GenericConnectionPage
{
    private final Image LOGO_ALTIBASE;

    public AltibaseConnectionPage() {
    	LOGO_ALTIBASE = createImage("icons/altibase_logo_wide.png");
    }
    
    @Override
    public void dispose()
    {
        super.dispose();
        UIUtils.dispose(LOGO_ALTIBASE);
    }
    
    @Override
    public Image getImage() {
        return LOGO_ALTIBASE;
    }
}