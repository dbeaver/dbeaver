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

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.part;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

/**
 * Figure with customizable background color
 */
public interface ICustomizablePart {

    boolean getCustomTransparency();

    void setCustomTransparency(boolean transparency);

    int getCustomBorderWidth();

    void setCustomBorderWidth(int borderWidth);

    Color getCustomBackgroundColor();

    void setCustomBackgroundColor(Color color);

    Color getCustomForegroundColor();

    void setCustomForegroundColor(Color color);

    Font getCustomFont();

    void setCustomFont(Font color);

}
