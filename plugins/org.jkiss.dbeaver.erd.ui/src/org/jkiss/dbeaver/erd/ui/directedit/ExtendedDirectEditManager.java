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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.erd.ui.directedit;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.erd.ui.figures.NoteFigure;

/**
 * A generic DirectEdit manager to be used for labels which includes validation
 * functionality by adding the ICellEditorValidator on startup
 */
public class ExtendedDirectEditManager extends DirectEditManager {

    private VerifyListener verifyListener;
    private final IFigure figure;
    private String originalValue;
    private boolean committing = false;
    private final ICellEditorValidator validator;

    /**
     * Creates a new ActivityDirectEditManager with the given attributes.
     *
     * @param source     the source EditPart
     * @param editorType type of editor
     * @param locator    the CellEditorLocator
     */
    public ExtendedDirectEditManager(GraphicalEditPart source, Class<? extends CellEditor> editorType, CellEditorLocator locator,
                                     IFigure figure, ICellEditorValidator validator) {
        super(source, editorType, locator);
        this.figure = figure;
        this.originalValue = getFigureText(figure);
        this.validator = validator;
    }

    protected IFigure getCellEditorFrame() {
        IFigure cellEditorFrame = super.getCellEditorFrame();
        if (cellEditorFrame.getBorder() != null) {
            //cellEditorFrame.setBorder(null);
        }
        return cellEditorFrame;
    }


    private static String getFigureText(IFigure figure) {
        if (figure instanceof Label) {
            return ((Label) figure).getText();
        } else if (figure instanceof NoteFigure) {
            return ((NoteFigure) figure).getText();
        } else {
            return "???";
        }
    }

    @Override
    protected void initCellEditor() {

        Text text = (Text) getCellEditor().getControl();

        //add the verifyListener to apply changes to the control size
        verifyListener = new VerifyListener() {

            /**
             * Changes the size of the editor control to reflect the changed
             * text
             */
            @Override
            public void verifyText(VerifyEvent event) {
                Text text = (Text) getCellEditor().getControl();
                String oldText = text.getText();
                String leftText = oldText.substring(0, event.start);
                String rightText = oldText.substring(event.end);
                GC gc = new GC(text);

                //String s = leftText + event.text + rightText;

                Point size = gc.textExtent(leftText + event.text + rightText);

                gc.dispose();
                if (size.x != 0)
                    size = text.computeSize(size.x, SWT.DEFAULT);
                else {
                    //just make it square
                    size.x = size.y;
                }
                getCellEditor().getControl().setSize(size.x, size.y);
            }

        };
        text.addVerifyListener(verifyListener);

        //set the initial value of the
        originalValue = getFigureText(this.figure);
        getCellEditor().setValue(originalValue);
        getCellEditor().setValidator(validator);

        text.selectAll();
    }

    /**
     * Commits the current value of the cell editor by getting a {@link Command}
     * from the source edit part and executing it via the {@link CommandStack}.
     * Finally, {@link #bringDown()}is called to perform and necessary cleanup.
     */
    @Override
    protected void commit() {

        if (committing)
            return;

        committing = true;

        try {

            //we set the cell editor control to invisible to remove any
            // possible flicker
            //getCellEditor().getControl().setVisible(false);
            if (isDirty()) {
                CommandStack stack = getEditPart().getViewer().getEditDomain().getCommandStack();
                EditPolicy editPolicy = getEditPart().getEditPolicy(EditPolicy.DIRECT_EDIT_ROLE);
                Command command;
                if (editPolicy != null) {
                    command = editPolicy.getCommand(getDirectEditRequest());
                } else {
                    command = getEditPart().getCommand(getDirectEditRequest());
                }
                if (command != null && command.canExecute()) {
                    stack.execute(command);
                }
            }
        } finally {
            bringDown();
            committing = false;
        }
    }

    /**
     * Need to override so as to remove the verify listener
     */
    @Override
    protected void unhookListeners() {
        super.unhookListeners();
        Text text = (Text) getCellEditor().getControl();
        text.removeVerifyListener(verifyListener);
        verifyListener = null;
    }

}