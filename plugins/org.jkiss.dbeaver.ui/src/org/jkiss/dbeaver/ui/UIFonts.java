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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.resource.JFaceResources;


public class UIFonts {

    public static class DBeaver {
        /**
         * Monospace font
         */
        public static final String MONOSPACE_FONT = "org.jkiss.dbeaver.dbeaver.ui.fonts.monospace";

        /**
         * Main font
         */
        public static final String MAIN_FONT = "org.jkiss.dbeaver.dbeaver.ui.fonts.main";
    }

    public class Eclipse {

        /**
         * Compare text font
         */
        public static final String COMPARE_TEXT_FONT = "org.eclipse.compare.contentmergeviewer.TextMergeViewer";

        /**
         * Detail pane text font
         */
        public static final String DETAIL_PANE_TEXT_FONT = "org.eclipse.debug.ui.DetailPaneFont";

        /**
         * Memory view table font
         */
        public static final String MEMORY_VIEW_TABLE_FONT = "org.eclipse.debug.ui.MemoryViewTableFont";

        /**
         * Variable text font
         */
        public static final String VARIABLE_TEXT_FONT = "org.eclipse.debug.ui.VariableTextFont";

        /**
         * Console font
         */
        public static final String CONSOLE_FONT = "org.eclipse.debug.ui.consoleFont";

        /**
         * Part title font
         */
        public static final String PART_TITLE_FONT = "org.eclipse.ui.workbench.TAB_TEXT_FONT";

        /**
         * Tree and Table font for views
         */
        public static final String TREE_AND_TABLE_FONT_FOR_VIEWS = "org.eclipse.ui.workbench.TREE_TABLE_FONT";

        /**
         * Header Font
         */
        public static final String HEADER_FONT = "org.eclipse.jface.headerfont";

        /**
         * Text Font
         */
        public static final String TEXT_FONT = "org.eclipse.jface.textfont";

        /**
         * Text Editor Block Selection Font
         */
        public static final String TEXT_EDITOR_BLOCK_SELECTION_FONT
            = "org.eclipse.ui.workbench.texteditor.blockSelectionModeFont";

        /**
         * Banner font
         */
        public static final String BANNER_FONT = JFaceResources.BANNER_FONT;

        /**
         * Dialog font
         */
        public static final String DIALOG_FONT = JFaceResources.DIALOG_FONT;
    }
}
