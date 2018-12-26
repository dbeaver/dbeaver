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
package org.jkiss.dbeaver.ui.editors.text;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.source.AbstractRulerColumn;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.rulers.IContributedRulerColumn;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;

/**
 * Script position ruler contribution.
 */
public class ScriptPositionColumn extends AbstractRulerColumn implements IContributedRulerColumn {

    public static final String ID = "org.jkiss.dbeaver.ui.editors.columns.script.position"; //$NON-NLS-1$

    private RulerColumnDescriptor descriptor;
    private ITextEditor editor;
    private int[] currentLines;
    private volatile boolean visible = false;

    public ScriptPositionColumn()
    {
//        setHover(new SQLAnnotationHover());
    }

    @Override
    public RulerColumnDescriptor getDescriptor()
    {
        return descriptor;
    }

    @Override
    public void setDescriptor(RulerColumnDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    @Override
    public void setEditor(ITextEditor editor)
    {
        this.editor = editor;
    }

    @Override
    public ITextEditor getEditor()
    {
        return editor;
    }

    @Override
    public void columnCreated()
    {
        visible = true;
        new UIJob("Update script ruler") {
            {
                setSystem(true);
            }
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor)
            {
                if (DBWorkbench.getPlatform().isShuttingDown()) {
                    return Status.CANCEL_STATUS;
                }
                BaseTextEditor editor = (BaseTextEditor)getEditor();
                if (editor == null || editor.getTextViewer() == null) return Status.CANCEL_STATUS;
                StyledText textWidget = editor.getTextViewer().getTextWidget();
                if (textWidget == null || textWidget.isDisposed()) return Status.CANCEL_STATUS;
                if (textWidget.isVisible()) {
                    int[] newCurrentLines = editor.getCurrentLines();
                    if (!Arrays.equals(newCurrentLines, currentLines)) {
                        currentLines = newCurrentLines;
                        redraw();
                    }
                }
                if (visible) {
                    schedule(100);
                }
                return Status.OK_STATUS;
            }
        }.schedule(100);
    }

    @Override
    public void columnRemoved()
    {
        visible = false;
    }

    protected void paintLine(GC gc, int modelLine, int widgetLine, int linePixel, int lineHeight) {
        gc.setBackground(computeBackground(modelLine));
        gc.fillRectangle(0, linePixel, getWidth(), lineHeight);
        if (ArrayUtils.contains(currentLines, modelLine)) {
            gc.drawImage(DBeaverIcons.getImage(UIIcon.RULER_POSITION), 0, linePixel);
        }
    }

/*
    @Override
    protected void paint(GC gc, ILineRange lines)
    {
        ITextViewer textViewer = getParentRuler().getTextViewer();
        StyledText textWidget = textViewer.getTextWidget();
        final int firstLine= lines.getStartLine();
        final int lastLine= firstLine + lines.getNumberOfLines();
        for (int line= firstLine; line < lastLine; line++) {
            int modelLine= JFaceTextUtil.widgetLine2ModelLine(textViewer, line);
            if (modelLine == -1)
                continue;
            int linePixel= textWidget.getLinePixel(line);
            int lineHeight= textWidget.getLineHeight(textWidget.getOffsetAtLine(line));

            gc.setBackground(computeBackground(modelLine));
            gc.fillRectangle(0, linePixel, getWidth(), lineHeight);
            if (ArrayUtils.contains(currentLines, modelLine)) {
                gc.drawImage(DBeaverIcons.getImage(UIIcon.RULER_POSITION), 0, linePixel);
            }
        }
    }
*/

}
