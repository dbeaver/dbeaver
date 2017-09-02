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

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.TextFlow;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.model.ERDNote;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Figure used to represent a note
 *
 * @author Serge Rider
 */
public class NoteFigure extends FlowPage {

    private TextFlow textFlow;

    public NoteFigure(ERDNote note)
    {
        //super(note.getObject());
        textFlow = new TextFlow(note.getObject());
        add(textFlow);

        setBackgroundColor(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_NOTE_BACKGROUND));
        setForegroundColor(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_NOTE_FOREGROUND));
        setOpaque(true);
        setBorder(new CompoundBorder(
            new LineBorder(UIUtils.getColorRegistry().get(ERDConstants.COLOR_ERD_ATTR_FOREGROUND), 1),
            new MarginBorder(5)
        ));
    }

    public void setText(String text)
    {
        textFlow.setText(text);
    }

    @Override
    public Dimension getPreferredSize(int width, int h)
    {
        return textFlow.getPreferredSize(width, h);//super.getPreferredSize(width, h);
    }

    @Override
    public void setPreferredSize(Dimension size)
    {
        textFlow.setSize(size);
        textFlow.setPreferredSize(size);
        super.setPreferredSize(size);
    }

}