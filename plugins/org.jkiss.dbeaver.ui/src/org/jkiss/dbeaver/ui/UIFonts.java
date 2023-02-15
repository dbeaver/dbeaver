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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.resource.JFaceResources;

public class UIFonts {

    // DBeaver fonts
    
    /**
     * Monospace font
     */
    public static String DBEAVER_FONTS_MONOSPACE =  "org.jkiss.dbeaver.dbeaver.ui.fonts.monospace";
    
    /**
     * Main font
     */
    public static String DBEAVER_FONTS_MAIN_FONT =  "org.jkiss.dbeaver.dbeaver.ui.fonts.main";
    
    /**
     * Diagram font
     */
    public static String DIAGRAM_FONT = "org.jkiss.dbeaver.erd.diagram.font";
    
    /**
     * Results grid font
     */
    public static String RESULTS_GRID_FONT = "org.jkiss.dbeaver.sql.resultset.font"; // TODO: refactoring
    
    // Eclipse fonts
    
    /**
     * Compare text font
     */
    public static String COMPARE_TEXT_FONT = "org.eclipse.compare.contentmergeviewer.TextMergeViewer";
    
    /**
     * Detail pane text font
     */
    public static String DETAIL_PANE_TEXT_FONT = "org.eclipse.debug.ui.DetailPaneFont";
    
    /**
     * Memory view table font
     */
    public static String MEMORY_VIEW_TABLE_FONT = "org.eclipse.debug.ui.MemoryViewTableFont";

    /**
     * Variable text font
     */
    public static String VARIABLE_TEXT_FONT = "org.eclipse.debug.ui.VariableTextFont";
 
    /**
     * Console font
     */
    public static String CONSOLE_FONT = "org.eclipse.debug.ui.consoleFont";

    /**
     * Part title font
     */
    public static String PART_TITLE_FONT = "org.eclipse.ui.workbench.TAB_TEXT_FONT";

    /**
     * Tree and Table font for views
     */
    public static String TREE_AND_TABLE_FONT_FOR_VIEWS = "org.eclipse.ui.workbench.TREE_TABLE_FONT";

    /**
     * Header Font
     */
    public static String HEADER_FONT = "org.eclipse.jface.headerfont";

    /**
     * Text Font
     */
    public static String TEXT_FONT = "org.eclipse.jface.textfont";

    /**
     * Text Editor Block Selection Font
     */
    public static String TEXT_EDITOR_BLOCK_SELECTION_FONT = "org.eclipse.ui.workbench.texteditor.blockSelectionModeFont";

    /**
     * Banner font
     */
    public static String BANNER_FONT = JFaceResources.BANNER_FONT;

    /**
     * Dialog font
     */
    public static String DIALOG_FONT = JFaceResources.DIALOG_FONT;
}
