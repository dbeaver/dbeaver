/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.binary;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.ui.editors.binary.internal.BinaryEditorMessages;

/**
 * Status line component of the editor. Displays the current position and the insert/overwrite status.
 */
public class HexStatusLine extends Composite {


    private static final String TEXT_INSERT = BinaryEditorMessages.editor_binary_hex_status_line_text_insert;
    private static final String TEXT_OVERWRITE = BinaryEditorMessages.editor_binary_hex_status_line_text_ovewrite;

    private Label position = null;
    private Label value = null;
    private Label insertMode = null;


    /**
     * Create a status line part
     *
     * @param parent            parent in the widget hierarchy
     * @param style             not used
     * @param withLeftSeparator so it can be put besides other status items (for plugin)
     */
    public HexStatusLine(Composite parent, int style, boolean withLeftSeparator)
    {
        super(parent, style);
        initialize(withLeftSeparator);
    }


    private void initialize(boolean withSeparator)
    {
        GridLayout statusLayout = new GridLayout();
        statusLayout.numColumns = withSeparator ? 6 : 5;
        statusLayout.marginHeight = 0;
        setLayout(statusLayout);

        if (withSeparator) {
            GridData separator1GridData = new GridData();
            separator1GridData.grabExcessVerticalSpace = true;
            separator1GridData.verticalAlignment = SWT.FILL;
            Label separator1 = new Label(this, SWT.SEPARATOR);
            separator1.setLayoutData(separator1GridData);
        }

        GC gc = new GC(this);
        FontMetrics fontMetrics = gc.getFontMetrics();

        position = new Label(this, SWT.SHADOW_NONE);
        GridData gridData1 = new GridData(/*SWT.DEFAULT*/
                                          (11 + 10 + 12 + 3 + 10 + 12) * fontMetrics.getAverageCharWidth(),
                                          SWT.DEFAULT);
        position.setLayoutData(gridData1);

        GridData separator23GridData = new GridData();
        separator23GridData.grabExcessVerticalSpace = true;
        separator23GridData.verticalAlignment = SWT.FILL;
        Label separator2 = new Label(this, SWT.SEPARATOR);
        separator2.setLayoutData(separator23GridData);

        value = new Label(this, SWT.SHADOW_NONE);
        GridData gridData2 = new GridData(/*SWT.DEFAULT*/
                                          (7 + 3 + 9 + 2 + 9 + 8 + 6) * fontMetrics.getAverageCharWidth(), SWT.DEFAULT);
        value.setLayoutData(gridData2);

        // From Eclipse 3.1's GridData javadoc:
        // NOTE: Do not reuse GridData objects. Every control in a Composite that is managed by a
        // GridLayout must have a unique GridData
        GridData separator3GridData = new GridData();
        separator3GridData.grabExcessVerticalSpace = true;
        separator3GridData.verticalAlignment = SWT.FILL;
        Label separator3 = new Label(this, SWT.SEPARATOR);
        separator3.setLayoutData(separator3GridData);

        insertMode = new Label(this, SWT.SHADOW_NONE);
        GridData gridData3 = new GridData(/*SWT.DEFAULT*/
                                          (TEXT_OVERWRITE.length() + 2) * fontMetrics.getAverageCharWidth(),
                                          SWT.DEFAULT);
        insertMode.setLayoutData(gridData3);
        gc.dispose();
    }


    /**
     * Update the insert mode status. Can be "Insert" or "Overwrite"
     *
     * @param insert true will display "Insert"
     */
    public void updateInsertModeText(boolean insert)
    {
        if (isDisposed() || insertMode.isDisposed()) return;

        insertMode.setText(insert ? TEXT_INSERT : TEXT_OVERWRITE);
    }

    /**
     * Update the position status and value.
     */
    public void updatePositionValueText(long pos, byte val)
    {
        updatePositionText(pos);
        updateValueText(val);
    }

    /**
     * Update the selection status and value.
     */
    public void updateSelectionValueText(long[] sel, byte val)
    {
        updateSelectionText(sel);
        updateValueText(val);
    }

    /**
     * Update the position status. Displays its decimal and hex value.
     */
    public void updatePositionText(long pos)
    {
        if (isDisposed() || position.isDisposed()) return;

        String posText = BinaryEditorMessages.editor_binary_hex_status_line_offset + pos + " (dec) = " + Long.toHexString(pos) + " (binary)";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
//	String posText = String.format("Offset: %1$d (dec) = %1$X (binary)", pos);
        position.setText(posText);
        //position.pack(true);
    }

    /**
     * Update the value. Displays its decimal, hex and binary value
     *
     * @param val value to display
     */
    public void updateValueText(byte val)
    {
        if (isDisposed() || position.isDisposed()) return;

        String valBinText = "0000000" + Long.toBinaryString(val); //$NON-NLS-1$
        String valText = BinaryEditorMessages.editor_binary_hex_status_line_value + val + " (dec) = " + Integer.toHexString(0x0ff & val) + " (binary) = " +  //$NON-NLS-2$ //$NON-NLS-3$
            valBinText.substring(valBinText.length() - 8) + " (bin)"; //$NON-NLS-1$
//	String valText = String.format("Value: %1$d (dec) = %1$X (binary) = %2$s (bin)", val, valBinText.substring(valBinText.length()-8));
        value.setText(valText);
        //value.pack(true);
    }

    /**
     * Update the selection status. Displays its decimal and hex values for start and end selection
     *
     * @param sel selection array to display: [0] = start, [1] = end
     */
    public void updateSelectionText(long[] sel)
    {
        if (isDisposed() || position.isDisposed()) return;

        String selText = BinaryEditorMessages.editor_binary_hex_status_line_selection + sel[0] + " (0x" + Long.toHexString(sel[0]) + ") - " + sel[1] +  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            " (0x" + Long.toHexString(sel[1]) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
//	String selText = String.format("Selection: %1$d (0x%1$X) - %2$d (0x%2$X)", sel[0], sel[1]);
        position.setText(selText);
        //position.pack(true);
    }

}
