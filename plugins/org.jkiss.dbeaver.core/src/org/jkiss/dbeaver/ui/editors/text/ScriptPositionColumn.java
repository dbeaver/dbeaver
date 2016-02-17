/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.text;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.JFaceTextUtil;
import org.eclipse.jface.text.source.AbstractRulerColumn;
import org.eclipse.jface.text.source.ILineRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.rulers.IContributedRulerColumn;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * The line number ruler contribution. Encapsulates a {@link org.eclipse.jface.text.source.LineNumberChangeRulerColumn} as a
 * contribution to the <code>rulerColumns</code> extension point.
 *
 * @since 3.3
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
                BaseTextEditor editor = (BaseTextEditor)getEditor();
                if (editor == null || editor.getTextViewer() == null) return Status.CANCEL_STATUS;
                StyledText textWidget = editor.getTextViewer().getTextWidget();
                if (textWidget == null || textWidget.isDisposed()) return Status.CANCEL_STATUS;
                int[] newCurrentLines = editor.getCurrentLines();
                if (!CommonUtils.equalObjects(newCurrentLines, currentLines) && textWidget.isVisible()) {
                    currentLines = newCurrentLines;
                    redraw();
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

}
