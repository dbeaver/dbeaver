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

package org.jkiss.dbeaver.ui.controls.resultset.plaintext;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextPrintOptions;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.StyledTextFindReplaceTarget;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Empty presentation.
 * Used when RSV has no results (initially).
 */
public class PlainTextPresentation extends AbstractPresentation implements IAdaptable {

    public static final int FIRST_ROW_LINE = 2;

    private StyledText text;
    private DBDAttributeBinding curAttribute;
    private StyledTextFindReplaceTarget findReplaceTarget;
    public boolean activated;
    private Color curLineColor;

    private int[] colWidths;
    private StyleRange curLineRange;
    private int totalRows = 0;
    private String curSelection;
    private Font monoFont;

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);

        UIUtils.createHorizontalLine(parent);
        text = new StyledText(parent, SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        text.setBlockSelection(true);
        text.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_IBEAM));
        text.setMargins(4, 4, 4, 4);
        text.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        text.setLayoutData(new GridData(GridData.FILL_BOTH));
        text.addCaretListener(event -> onCursorChange(event.caretOffset));
        text.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                curSelection = text.getSelectionText();
                fireSelectionChanged(new PlainTextSelectionImpl());
            }
        });
        text.addDisposeListener(e -> {
            dispose();
        });

        final ScrollBar verticalBar = text.getVerticalBar();
        verticalBar.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (verticalBar.getSelection() + verticalBar.getPageIncrement() >= verticalBar.getMaximum()) {
                    if (controller.getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT) &&
                        !controller.isRecordMode() &&
                        controller.isHasMoreData()) {
                        controller.readNextSegment();
                    }
                }
            }
        });
        findReplaceTarget = new StyledTextFindReplaceTarget(text);
        UIUtils.enableHostEditorKeyBindingsSupport(controller.getSite(), text);

        applyThemeSettings();

        registerContextMenu();
        activateTextKeyBindings(controller, text);
        trackPresentationControl();
    }

    @Override
    public void dispose() {
        if (monoFont != null) {
            UIUtils.dispose(monoFont);
            monoFont = null;
        }
        super.dispose();
    }

    @Override
    protected void applyThemeSettings() {
        IThemeManager themeManager = controller.getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
        curLineColor = themeManager.getCurrentTheme().getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_ODD_BACK);

        ITheme currentTheme = themeManager.getCurrentTheme();
        Font rsFont = currentTheme.getFontRegistry().get(ThemeConstants.FONT_SQL_RESULT_SET);
        if (rsFont != null) {
            int fontHeight = rsFont.getFontData()[0].getHeight();
            Font font = JFaceResources.getFont(JFaceResources.TEXT_FONT);

            FontData[] fontData = font.getFontData();
            fontData[0].setHeight(fontHeight);
            Font newFont = new Font(font.getDevice(), fontData[0]);

            this.text.setFont(newFont);

            if (monoFont != null) {
                UIUtils.dispose(monoFont);
            }
            monoFont = newFont;

        }
    }

    private void onCursorChange(int offset) {
        ResultSetModel model = controller.getModel();

        int lineNum = text.getLineAtOffset(offset);
        int lineOffset = text.getOffsetAtLine(lineNum);
        int horizontalOffset = offset - lineOffset;

        int lineCount = text.getLineCount();

        int rowNum = lineNum - FIRST_ROW_LINE; //First 2 lines is header
        if (controller.isRecordMode()) {
            if (rowNum < 0) {
                rowNum = 0;
            }
            if (rowNum >= 0 && rowNum < model.getVisibleAttributeCount()) {
                curAttribute = model.getVisibleAttribute(rowNum);
            }
        } else {
            int colNum = 0;
            int horOffsetBegin = 0, horOffsetEnd = 0;
            for (int i = 0; i < colWidths.length; i++) {
                horOffsetBegin = horOffsetEnd;
                horOffsetEnd += colWidths[i] + 1;
                if (horizontalOffset < horOffsetEnd) {
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

            {
                // Highlight row
                if (curLineRange == null || curLineRange.start != lineOffset + horOffsetBegin) {
                    curLineRange = new StyleRange(
                        lineOffset + horOffsetBegin,
                        horOffsetEnd - horOffsetBegin - 1,
                        null,
                        curLineColor);
                    DBeaverUI.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            text.setStyleRanges(new StyleRange[]{curLineRange});
                        }
                    });
                }
            }

            if (lineNum == lineCount - 1 &&
                controller.isHasMoreData() &&
                controller.getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT)) {
                controller.readNextSegment();
            }
        }
        fireSelectionChanged(new PlainTextSelectionImpl());
    }

    @Override
    public Control getControl() {
        return text;
    }

    @Override
    public void refreshData(boolean refreshMetadata, boolean append, boolean keepState) {
        colWidths = null;
        if (controller.isRecordMode()) {
            printRecord();
        } else {
            printGrid(append);
        }
    }

    private void printGrid(boolean append) {
        int maxColumnSize = getController().getPreferenceStore().getInt(DBeaverPreferences.RESULT_TEXT_MAX_COLUMN_SIZE);
        DBDDisplayFormat displayFormat = DBDDisplayFormat.safeValueOf(getController().getPreferenceStore().getString(DBeaverPreferences.RESULT_TEXT_VALUE_FORMAT));

        StringBuilder grid = new StringBuilder(512);
        ResultSetModel model = controller.getModel();
        List<DBDAttributeBinding> attrs = model.getVisibleAttributes();

        List<ResultSetRow> allRows = model.getAllRows();
        if (colWidths == null) {
            // Calculate column widths
            colWidths = new int[attrs.size()];

            for (int i = 0; i < attrs.size(); i++) {
                DBDAttributeBinding attr = attrs.get(i);
                colWidths[i] = getAttributeName(attr).length();
                for (ResultSetRow row : allRows) {
                    String displayString = getCellString(model, attr, row, displayFormat);
                    colWidths[i] = Math.max(colWidths[i], displayString.length());
                }
            }
            for (int i = 0; i < colWidths.length; i++) {
                colWidths[i]++;
                if (colWidths[i] > maxColumnSize) {
                    colWidths[i] = maxColumnSize;
                }
            }
        }

        if (!append) {
            // Print header
            for (int i = 0; i < attrs.size(); i++) {
                DBDAttributeBinding attr = attrs.get(i);
                String attrName = getAttributeName(attr);
                grid.append(attrName);
                for (int k = colWidths[i] - attrName.length(); k > 0; k--) {
                    grid.append(" ");
                }
                grid.append("|");
            }
            grid.append("\n");

            // Print divider
            // Print header
            for (int i = 0; i < attrs.size(); i++) {
                for (int k = colWidths[i]; k > 0; k--) {
                    grid.append("-");
                }
                grid.append("|");
            }
            grid.append("\n");
        }

        // Print rows
        int firstRow = append ? totalRows : 0;
        if (append) {
            grid.append("\n");
        }
        for (int i = firstRow; i < allRows.size(); i++) {
            ResultSetRow row = allRows.get(i);
            for (int k = 0; k < attrs.size(); k++) {
                DBDAttributeBinding attr = attrs.get(k);
                String displayString = getCellString(model, attr, row, displayFormat);
                if (displayString.length() >= colWidths[k] - 1) {
                    displayString = CommonUtils.truncateString(displayString, colWidths[k] - 1);
                }
                grid.append(displayString);
                for (int j = colWidths[k] - displayString.length(); j > 0; j--) {
                    grid.append(" ");
                }
                grid.append("|");
            }
            grid.append("\n");
        }
        grid.setLength(grid.length() - 1); // cut last line feed

        if (append) {
            text.append(grid.toString());
        } else {
            text.setText(grid.toString());
        }

        totalRows = allRows.size();
    }

    private static String getAttributeName(DBDAttributeBinding attr) {
        if (CommonUtils.isEmpty(attr.getLabel())) {
            return attr.getName();
        } else {
            return attr.getLabel();
        }
    }

    private String getCellString(ResultSetModel model, DBDAttributeBinding attr, ResultSetRow row, DBDDisplayFormat displayFormat) {
        String displayString = attr.getValueHandler().getValueDisplayString(attr, model.getCellValue(attr, row), displayFormat);
        return TextUtils.getSingleLineString(displayString);
    }

    private void printRecord() {
        DBDDisplayFormat displayFormat = DBDDisplayFormat.safeValueOf(getController().getPreferenceStore().getString(DBeaverPreferences.RESULT_TEXT_VALUE_FORMAT));

        StringBuilder grid = new StringBuilder(512);
        ResultSetModel model = controller.getModel();
        List<DBDAttributeBinding> attrs = model.getVisibleAttributes();
        String[] values = new String[attrs.size()];
        ResultSetRow currentRow = controller.getCurrentRow();

        // Calculate column widths
        int nameWidth = 4, valueWidth = 5;
        for (int i = 0; i < attrs.size(); i++) {
            DBDAttributeBinding attr = attrs.get(i);
            nameWidth = Math.max(nameWidth, getAttributeName(attr).length());
            values[i] = attr.getValueHandler().getValueDisplayString(attr, model.getCellValue(attr, currentRow), displayFormat);
            valueWidth = Math.max(valueWidth, values[i].length());
        }

        // Header
        grid.append("Name");
        for (int j = nameWidth - 4; j > 0; j--) {
            grid.append(" ");
        }
        grid.append("|Value\n");
        for (int j = 0; j < nameWidth; j++) grid.append("-");
        grid.append("|");
        for (int j = 0; j < valueWidth; j++) grid.append("-");
        grid.append("\n");

        // Values
        for (int i = 0; i < attrs.size(); i++) {
            DBDAttributeBinding attr = attrs.get(i);
            String name = getAttributeName(attr);
            grid.append(name);
            for (int j = nameWidth - name.length(); j > 0; j--) {
                grid.append(" ");
            }
            grid.append("|");
            grid.append(values[i]);
            grid.append("\n");
        }
        grid.setLength(grid.length() - 1); // cut last line feed
        text.setText(grid.toString());
    }

    @Override
    public void formatData(boolean refreshData) {
        //controller.refreshData(null);
    }

    @Override
    public void clearMetaData() {
        colWidths = null;
        curLineRange = null;
        totalRows = 0;
    }

    @Override
    public void updateValueView() {

    }

    @Override
    public void fillMenu(@NotNull IMenuManager menu) {

    }

    @Override
    public void changeMode(boolean recordMode) {

    }

    @Override
    public void scrollToRow(@NotNull RowPosition position) {
        if (controller.isRecordMode()) {
            super.scrollToRow(position);
        } else {
            int caretOffset = text.getCaretOffset();
            if (caretOffset < 0) caretOffset = 0;
            int lineNum = text.getLineAtOffset(caretOffset);
            if (lineNum < FIRST_ROW_LINE) {
                lineNum = FIRST_ROW_LINE;
            }
            int lineOffset = text.getOffsetAtLine(lineNum);
            int xOffset = caretOffset - lineOffset;
            int totalLines = text.getLineCount();
            switch (position) {
                case FIRST:
                    lineNum = FIRST_ROW_LINE;
                    break;
                case PREVIOUS:
                    lineNum--;
                    break;
                case NEXT:
                    lineNum++;
                    break;
                case LAST:
                    lineNum = totalLines - 1;
                    break;
                case CURRENT:
                    lineNum = controller.getCurrentRow().getVisualNumber() + FIRST_ROW_LINE;
                    break;
            }
            if (lineNum < FIRST_ROW_LINE || lineNum >= totalLines) {
                return;
            }
            int newOffset = text.getOffsetAtLine(lineNum);
            newOffset += xOffset;
            text.setCaretOffset(newOffset);
            //text.setSelection(newOffset, 0);
            text.showSelection();
        }
    }

    @Nullable
    @Override
    public DBDAttributeBinding getCurrentAttribute() {
        return curAttribute;
    }

    @Nullable
    @Override
    public String copySelectionToString(ResultSetCopySettings settings) {
        return text.getSelectionText();
    }

    private static PrinterData fgPrinterData= null;

    @Override
    public void printResultSet() {
        final Shell shell = getControl().getShell();
        StyledTextPrintOptions options = new StyledTextPrintOptions();
        options.printTextFontStyle = true;
        options.printTextForeground = true;

        if (Printer.getPrinterList().length == 0) {
            UIUtils.showMessageBox(shell, "No printers", "Printers not found", SWT.ICON_ERROR);
            return;
        }

        final PrintDialog dialog = new PrintDialog(shell, SWT.PRIMARY_MODAL);
        dialog.setPrinterData(fgPrinterData);
        final PrinterData data = dialog.open();

        if (data != null) {
            final Printer printer = new Printer(data);
            final Runnable styledTextPrinter = text.print(printer, options);
            new Thread("Printing") { //$NON-NLS-1$
                public void run() {
                    styledTextPrinter.run();
                    printer.dispose();
                }
            }.start();

			/*
             * FIXME:
			 * 	Should copy the printer data to avoid threading issues,
			 *	but this is currently not possible, see http://bugs.eclipse.org/297957
			 */
            fgPrinterData = data;
            fgPrinterData.startPage = 1;
            fgPrinterData.endPage = 1;
            fgPrinterData.scope = PrinterData.ALL_PAGES;
            fgPrinterData.copyCount = 1;
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IFindReplaceTarget.class) {
            return adapter.cast(findReplaceTarget);
        }
        return null;
    }

    @Override
    public ISelection getSelection() {
        return new PlainTextSelectionImpl();
    }

    private class PlainTextSelectionImpl implements IResultSetSelection {

        @Nullable
        @Override
        public Object getFirstElement()
        {
            return curSelection;
        }

        @Override
        public Iterator<String> iterator()
        {
            return toList().iterator();
        }

        @Override
        public int size()
        {
            return curSelection == null ? 0 : 1;
        }

        @Override
        public Object[] toArray()
        {
            return curSelection == null ?
                new Object[0] :
                new Object[] { curSelection };
        }

        @Override
        public List<String> toList()
        {
            return curSelection == null ?
                Collections.<String>emptyList() :
                Collections.singletonList(curSelection);
        }

        @Override
        public boolean isEmpty()
        {
            return false;
        }

        @NotNull
        @Override
        public IResultSetController getController()
        {
            return controller;
        }

        @NotNull
        @Override
        public Collection<DBDAttributeBinding> getSelectedAttributes() {
            if (curAttribute == null) {
                return Collections.emptyList();
            }
            return Collections.singleton(curAttribute);
        }

        @NotNull
        @Override
        public Collection<ResultSetRow> getSelectedRows()
        {
            ResultSetRow currentRow = controller.getCurrentRow();
            if (currentRow == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(currentRow);
        }

        @Override
        public DBDAttributeBinding getElementAttribute(Object element) {
            return curAttribute;
        }

        @Override
        public ResultSetRow getElementRow(Object element) {
            return getController().getCurrentRow();
        }
    }

}
