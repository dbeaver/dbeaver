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

package org.jkiss.dbeaver.ui.controls.resultset.plaintext;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextPrintOptions;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPAdaptable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.UIFonts;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.StyledTextFindReplaceTarget;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Empty presentation.
 * Used when RSV has no results (initially).
 */
public class PlainTextPresentation extends AbstractPresentation implements IResultSetDisplayFormatProvider, DBPAdaptable {

    public static final int FIRST_ROW_LINE = 2;

    private StyledText text;
    private DBDAttributeBinding curAttribute;
    private StyledTextFindReplaceTarget findReplaceTarget;
    public boolean activated;
    private Color curLineColor;

    private int[] colWidths;
    private int startOffset;
    private StyleRange curLineRange;
    private int totalRows = 0;
    private String curSelection;

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);

        UIUtils.createHorizontalLine(parent);
        text = new StyledText(parent, SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        text.setBlockSelection(true);
        text.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_IBEAM));
        text.setMargins(4, 4, 4, 4);
        text.setForeground(UIStyles.getDefaultTextForeground());
        text.setBackground(UIStyles.getDefaultTextBackground());
        text.setTabs(controller.getPreferenceStore().getInt(ResultSetPreferences.RESULT_TEXT_TAB_SIZE));
        text.setTabStops(null);
        text.setFont(UIUtils.getMonospaceFont());
        text.setLayoutData(new GridData(GridData.FILL_BOTH));
        text.addCaretListener(event -> onCursorChange(event.caretOffset));
        text.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                curSelection = text.getSelectionText();
                fireSelectionChanged(new PlainTextSelectionImpl());
            }
        });

        final ScrollBar verticalBar = text.getVerticalBar();
        verticalBar.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (verticalBar.getSelection() + verticalBar.getPageIncrement() >= verticalBar.getMaximum()) {
                    if (controller.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT) &&
                        !controller.isRecordMode() &&
                        controller.isHasMoreData()) {
                        controller.readNextSegment();
                    }
                }
            }
        });
        findReplaceTarget = new StyledTextFindReplaceTarget(text);
        TextEditorUtils.enableHostEditorKeyBindingsSupport(controller.getSite(), text);

        applyCurrentThemeSettings();

        registerContextMenu();
        activateTextKeyBindings(controller, text);
        trackPresentationControl();
    }

    @Override
    protected void applyThemeSettings(ITheme currentTheme) {
        text.setFont(BaseThemeSettings.instance.monospaceFont);
        if (UIStyles.isDarkHighContrastTheme()) {
            text.setBackground(UIStyles.getDefaultWidgetBackground());
            text.setForeground(UIUtils.COLOR_WHITE);
            curLineColor = UIUtils.COLOR_GREEN_CONTRAST;
        } else {
            curLineColor = ResultSetThemeSettings.instance.backgroundOdd;
        }
    }

    private void onCursorChange(int offset) {
        ResultSetModel model = controller.getModel();
        DBPPreferenceStore prefs = controller.getPreferenceStore();
        int lineNum = text.getLineAtOffset(offset);
        int lineOffset = text.getOffsetAtLine(lineNum);
        int horizontalOffset = offset - lineOffset;

        int lineCount = text.getLineCount();

        boolean delimLeading = getController().getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING);
        boolean delimTop = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_TOP);

        int rowNum = lineNum - FIRST_ROW_LINE - (delimTop ? 1 : 0) ; //First 2 lines is header + 1 if top delimiter turned on
        if (controller.isRecordMode()) {
            if (rowNum < 0) {
                rowNum = 0;
            }
            if (rowNum >= 0 && rowNum < model.getVisibleAttributeCount()) {
                curAttribute = model.getVisibleAttribute(rowNum);
            }
        } else {
            int colNum = 0;
            int horOffsetBegin = 0, horOffsetEnd = startOffset;

            if (delimLeading) horOffsetEnd++;
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
                    text.setStyleRanges(new StyleRange[]{curLineRange});
                    text.redraw();
                }
            }

            if (lineNum == lineCount - 1 &&
                controller.isHasMoreData() &&
                controller.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT)) {
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
        if (controller.isRecordMode()) {
            printRecord();
        } else {
            printGrid(append);
        }
    }

    private void printGrid(boolean append) {
        StringBuilder grid = new StringBuilder(512);

        PlainTextFormatter formatter = new PlainTextFormatter(getController().getPreferenceStore());
        ResultSetModel model = controller.getModel();
        totalRows = formatter.printGrid(grid, model);
        colWidths = formatter.getColWidths();
        startOffset = formatter.getStartOffset();

        final int topIndex = text.getTopIndex();
        final int horizontalIndex = text.getHorizontalIndex();
        final int caretOffset = text.getCaretOffset();

        text.setText(grid.toString());

        if (append) {
            // Restore scroll and caret position
            text.setTopIndex(topIndex);
            text.setHorizontalIndex(horizontalIndex);
            text.setCaretOffset(caretOffset);
        }
    }


    private void printRecord() {
        PlainTextFormatter formatter = new PlainTextFormatter(getController().getPreferenceStore());
        StringBuilder grid = new StringBuilder(512);
        formatter.printRecord(grid, controller.getModel(), controller.getCurrentRow());

        text.setText(grid.toString());
    }

    @NotNull
    @Override
    public String getFontId() {
        return UIFonts.DBEAVER_FONTS_MONOSPACE;
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
        text.setSelection(0);
        text.setBlockSelectionBounds(new Rectangle(0, 0, 0, 0));
        curSelection = null;
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

    @NotNull
    @Override
    public Map<Transfer, Object> copySelection(ResultSetCopySettings settings) {
        return Collections.singletonMap(
            TextTransfer.getInstance(),
            text.getSelectionText());
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
    protected void performHorizontalScroll(int scrollCount) {
        ScrollBar hsb = text.getHorizontalBar();
        if (hsb != null && hsb.isVisible()) {
            int curPosition = text.getHorizontalPixel();
            int pageIncrement = UIUtils.getFontHeight(text.getFont()) * 10;
            if (scrollCount > 0) {
                if (curPosition > 0) {
                    curPosition -= pageIncrement;
                }
            } else {
                curPosition += pageIncrement;
            }
            if (curPosition < 0) curPosition = 0;
            text.setHorizontalPixel(curPosition);
            //text.setHorizontalIndex();
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

    @Override
    public DBDDisplayFormat getDefaultDisplayFormat() {
        return DBDDisplayFormat.safeValueOf(controller.getPreferenceStore().getString(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT));
    }

    @Override
    public void setDefaultDisplayFormat(DBDDisplayFormat displayFormat) {
        controller.getPreferenceStore().setValue(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT, displayFormat.name());
    }

    private class PlainTextSelectionImpl implements IResultSetSelection {

        @Nullable
        @Override
        public Object getFirstElement()
        {
            return curSelection;
        }

        @NotNull
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
                Collections.emptyList() :
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
        public List<DBDAttributeBinding> getSelectedAttributes() {
            if (curAttribute == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(curAttribute);
        }

        @NotNull
        @Override
        public List<ResultSetRow> getSelectedRows()
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
