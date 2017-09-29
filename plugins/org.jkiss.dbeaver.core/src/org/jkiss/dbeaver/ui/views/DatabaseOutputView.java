/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class DatabaseOutputView extends ViewPart
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.databaseOutput";

    private StyledText text;
    private PrintWriter writer;
    private Map<String, StringBuilder> outputCache = new HashMap<>();

    @Override
    public void createPartControl(Composite parent)
    {
        text = new StyledText(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
    }

    @Override
    public void setFocus()
    {
        text.setFocus();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        return null;
    }

}
