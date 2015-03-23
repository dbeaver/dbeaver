/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.controls.resultset.plaintext;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.StyledTextFindReplaceTarget;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;

import java.util.List;

/**
 * Empty presentation.
 * Used when RSV has no results (initially).
 */
public class PlainTextPresentation implements IResultSetPresentation, IAdaptable {

    public static final int FIRST_ROW_LINE = 2;
    private IResultSetController controller;
    private StyledText text;
    private int[] colWidths;
    private DBDAttributeBinding curAttribute;
    private StyledTextFindReplaceTarget findReplaceTarget;

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        this.controller = controller;

        UIUtils.createHorizontalLine(parent);
        text = new StyledText(parent, SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        text.setBlockSelection(true);
        text.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_IBEAM));
        text.setMargins(4, 4, 4, 4);
        text.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        text.setLayoutData(new GridData(GridData.FILL_BOTH));
        text.addCaretListener(new CaretListener() {
            @Override
            public void caretMoved(CaretEvent event) {
                onCursorChange(event.caretOffset);
            }
        });
        findReplaceTarget = new StyledTextFindReplaceTarget(text);

        // Register context menu
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(text);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                controller.fillContextMenu(
                    manager,
                    curAttribute,
                    controller.getCurrentRow());
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        text.setMenu(menu);
        controller.getSite().registerContextMenu(menuMgr, null);
    }

    private void onCursorChange(int offset) {
        ResultSetModel model = controller.getModel();

        int lineNum = text.getLineAtOffset(offset);
        int lineOffset = offset - text.getOffsetAtLine(lineNum);

        int rowNum = lineNum - FIRST_ROW_LINE; //First 2 lines is header
        int colNum = 0;
        int tmpWidth = 0;
        for (int i = 0; i < colWidths.length; i++) {
            tmpWidth += colWidths[i];
            if (lineOffset < tmpWidth) {
                colNum = i;
                break;
            }
        }
        if (rowNum < 0 && model.getRowCount() > 0) {
            rowNum = 0;
        }
        if (rowNum >= 0 && rowNum < model.getRowCount() && colNum >= 0 && colNum < model.getVisibleAttributeCount()) {
            controller.setCurrentRow(model.getRow(rowNum));
            curAttribute = model.getVisibleAttribute(colNum);
        }
        controller.updateEditControls();
    }

    @Override
    public Control getControl() {
        return text;
    }

    @Override
    public void refreshData(boolean refreshMetadata) {
        StringBuilder grid = new StringBuilder(512);
        ResultSetModel model = controller.getModel();
        List<DBDAttributeBinding> attrs = model.getVisibleAttributes();
        colWidths = new int[attrs.size()];

        for (int i = 0; i < attrs.size(); i++) {
            DBDAttributeBinding attr = attrs.get(i);
            colWidths[i] = attr.getName().length();
            for (ResultSetRow row : model.getAllRows()) {
                String displayString = attr.getValueHandler().getValueDisplayString(attr, model.getCellValue(attr, row), DBDDisplayFormat.EDIT);
                colWidths[i] = Math.max(colWidths[i], displayString.length());
            }
        }
        for (int i = 0; i < colWidths.length; i++) {
            colWidths[i]++;
        }

        // Print header
        for (int i = 0; i < attrs.size(); i++) {
            DBDAttributeBinding attr = attrs.get(i);
            String attrName = attr.getName();
            grid.append(attrName);
            for (int k = colWidths[i] - attrName.length(); k >0 ; k--) {
                grid.append(" ");
            }
            grid.append("|");
        }
        grid.append("\n");

        // Print divider
        // Print header
        for (int i = 0; i < attrs.size(); i++) {
            for (int k = colWidths[i]; k >0 ; k--) {
                grid.append("-");
            }
            grid.append("|");
        }
        grid.append("\n");

        // Print rows
        for (ResultSetRow row : model.getAllRows()) {
            for (int i = 0; i < attrs.size(); i++) {
                DBDAttributeBinding attr = attrs.get(i);
                String displayString = attr.getValueHandler().getValueDisplayString(attr, model.getCellValue(attr, row), DBDDisplayFormat.EDIT);
                grid.append(displayString);
                for (int k = colWidths[i] - displayString.length(); k >0 ; k--) {
                    grid.append(" ");
                }
                grid.append("|");
            }
            grid.append("\n");
        }
        grid.setLength(grid.length() - 1); // cut last line fe
        text.setText(grid.toString());
    }

    @Override
    public void formatData(boolean refreshData) {

    }

    @Override
    public void clearData() {
        colWidths = new int[0];
    }

    @Override
    public void updateValueView() {

    }

    @Override
    public void fillToolbar(@NotNull IToolBarManager toolBar) {

    }

    @Override
    public void fillMenu(@NotNull IMenuManager menu) {

    }

    @Override
    public void changeMode(boolean recordMode) {

    }

    @Override
    public void scrollToRow(@NotNull RowPosition position) {
        int caretOffset = text.getCaretOffset();
        if (caretOffset < 0) caretOffset = 0;
        int lineNum = text.getLineAtOffset(caretOffset);
        if (lineNum < FIRST_ROW_LINE) {
            lineNum = FIRST_ROW_LINE;
        }
        int totalLines = text.getLineCount();
        switch (position) {
            case FIRST:     lineNum = FIRST_ROW_LINE; break;
            case PREVIOUS:  lineNum--; break;
            case NEXT:      lineNum++; break;
            case LAST:      lineNum = totalLines - 1; break;
        }
        if (lineNum < FIRST_ROW_LINE || lineNum >= totalLines) {
            return;
        }
        int newOffset = text.getOffsetAtLine(lineNum);
        text.setCaretOffset(newOffset);
        //text.setSelection(newOffset, 0);
        text.showSelection();
    }

    @Nullable
    @Override
    public DBDAttributeBinding getCurrentAttribute() {
        return curAttribute;
    }

    @Nullable
    @Override
    public String copySelectionToString(boolean copyHeader, boolean copyRowNumbers, boolean cut, String delimiter, DBDDisplayFormat format) {
        return text.getSelectionText();
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == IFindReplaceTarget.class) {
            return findReplaceTarget;
        }
        return null;
    }
}
