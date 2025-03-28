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
package org.jkiss.dbeaver.erd.ui.editor;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.ui.ThemeColor;
import org.jkiss.dbeaver.ui.ThemeFont;
import org.jkiss.dbeaver.ui.ThemeListener;

/**
 * Theme settings
 */
public class ERDThemeSettings extends ThemeListener {

    @ThemeFont(ERDUIConstants.PROP_DIAGRAM_FONT)
    public volatile Font diagramFont;
    @ThemeFont(value = ERDUIConstants.PROP_DIAGRAM_FONT, bold = true)
    public volatile Font diagramFontBold;
    @ThemeFont(ERDUIConstants.PROP_DIAGRAM_NOTATION_LABEL_FONT)
    public volatile Font notationLabelFont;

    @ThemeColor(ERDUIConstants.COLOR_ERD_DIAGRAM_BACKGROUND)
    public volatile Color diagramBackground;
    @ThemeColor(ERDUIConstants.COLOR_ERD_ENTITY_PRIMARY_BACKGROUND)
    public volatile Color entityPrimaryBackground;
    @ThemeColor(ERDUIConstants.COLOR_ERD_ENTITY_ASSOCIATION_BACKGROUND)
    public volatile Color entityAssociationBackground;
    @ThemeColor(ERDUIConstants.COLOR_ERD_ENTITY_REGULAR_BACKGROUND)
    public volatile Color entityRegularBackground;
    @ThemeColor(ERDUIConstants.COLOR_ERD_ENTITY_NAME_FOREGROUND)
    public volatile Color entityNameForeground;
    @ThemeColor(ERDUIConstants.COLOR_ERD_LINES_FOREGROUND)
    public volatile Color linesForeground;
    @ThemeColor(ERDUIConstants.COLOR_ERD_ATTR_BACKGROUND)
    public volatile Color attrBackground;
    @ThemeColor(ERDUIConstants.COLOR_ERD_ATTR_FOREGROUND)
    public volatile Color attrForeground;
    @ThemeColor(ERDUIConstants.COLOR_ERD_SEARCH_HIGHLIGHTING)
    public volatile Color searchHighlightColor;
    @ThemeColor(ERDUIConstants.COLOR_ERD_FK_HIGHLIGHTING)
    public volatile Color fkHighlightColor;
    @ThemeColor(ERDUIConstants.COLOR_ERD_NOTE_BACKGROUND)
    public volatile Color noteBackground;
    @ThemeColor(ERDUIConstants.COLOR_ERD_NOTE_FOREGROUND)
    public volatile Color noteForeground;

    @ThemeColor(ERDUIConstants.COLOR_ERD_BORDERS_COLOR_1)
    public volatile Color borderColor1;
    @ThemeColor(ERDUIConstants.COLOR_ERD_BORDERS_COLOR_2)
    public volatile Color borderColor2;
    @ThemeColor(ERDUIConstants.COLOR_ERD_BORDERS_COLOR_3)
    public volatile Color borderColor3;
    @ThemeColor(ERDUIConstants.COLOR_ERD_BORDERS_COLOR_4)
    public volatile Color borderColor4;
    @ThemeColor(ERDUIConstants.COLOR_ERD_BORDERS_COLOR_5)
    public volatile Color borderColor5;
    @ThemeColor(ERDUIConstants.COLOR_ERD_BORDERS_COLOR_6)
    public volatile Color borderColor6;
    @ThemeColor(ERDUIConstants.COLOR_ERD_BORDERS_COLOR_7)
    public volatile Color borderColor7;

    @ThemeColor(ERDUIConstants.COLOR_ERD_HEADER_COLOR_1)
    public volatile Color headerColor1;
    @ThemeColor(ERDUIConstants.COLOR_ERD_HEADER_COLOR_2)
    public volatile Color headerColor2;
    @ThemeColor(ERDUIConstants.COLOR_ERD_HEADER_COLOR_3)
    public volatile Color headerColor3;
    @ThemeColor(ERDUIConstants.COLOR_ERD_HEADER_COLOR_4)
    public volatile Color headerColor4;
    @ThemeColor(ERDUIConstants.COLOR_ERD_HEADER_COLOR_5)
    public volatile Color headerColor5;
    @ThemeColor(ERDUIConstants.COLOR_ERD_HEADER_COLOR_6)
    public volatile Color headerColor6;
    @ThemeColor(ERDUIConstants.COLOR_ERD_HEADER_COLOR_7)
    public volatile Color headerColor7;

    public static final ERDThemeSettings instance = new ERDThemeSettings();
}
