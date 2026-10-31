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

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ui.editors.erd.figures;

import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.TextFlow;
import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.erd.ERDNote;
import org.jkiss.dbeaver.ui.editors.erd.ERDUIConstants;
import org.jkiss.dbeaver.ui.editors.erd.editor.ERDThemeSettings;

/**
 * Figure used to represent a note
 *
 * @author Serge Rider
 */
public class NoteFigure extends FlowPage {

    private final TextFlow textFlow;
    private Color themeBackground;
    private Color themeForeground;

    public NoteFigure(@NotNull ERDNote note) {
        //super(note.getObject());
        textFlow = new TextFlow(note.getObject());
        add(textFlow);

        setBackgroundColor(ERDThemeSettings.instance.noteBackground);
        setForegroundColor(ERDThemeSettings.instance.noteForeground);
        setOpaque(true);
        setBorder(new CompoundBorder(
            new LineBorder(ERDThemeSettings.instance.linesForeground, ERDUIConstants.DEFAULT_NOTE_BORDER_WIDTH),
            new MarginBorder(5)
        ));
        themeBackground = getBackgroundColor();
        themeForeground = getForegroundColor();
    }

    public void refreshTheme() {
        if (getBackgroundColor() == themeBackground) {
            setBackgroundColor(ERDThemeSettings.instance.noteBackground);
        }
        if (getForegroundColor() == themeForeground) {
            setForegroundColor(ERDThemeSettings.instance.noteForeground);
        }
        themeBackground = ERDThemeSettings.instance.noteBackground;
        themeForeground = ERDThemeSettings.instance.noteForeground;
        if (getBorder() instanceof CompoundBorder border && border.getOuterBorder() instanceof LineBorder lineBorder) {
            lineBorder.setColor(ERDThemeSettings.instance.linesForeground);
        }
    }

    @NotNull
    public TextFlow getTextFlow() {
        return textFlow;
    }

    public String getText() {
        return textFlow.getText();
    }

    public void setText(String text) {
        textFlow.setText(text);
    }

    @Override
    public Dimension getPreferredSize(int width, int h) {
        // Return current size if it is bigger than text (means it was changed manually)
        Dimension currentSize = getSize();
        Dimension textPrefSize = textFlow.getPreferredSize(width, h);
        if (currentSize.width >= textPrefSize.width && currentSize.height >= textPrefSize.height) {
            return currentSize;
        }
        return textPrefSize;
    }

    @Override
    public void setPreferredSize(Dimension size) {
        textFlow.setSize(size);
        textFlow.setPreferredSize(size);
        super.setPreferredSize(size);
    }

}
