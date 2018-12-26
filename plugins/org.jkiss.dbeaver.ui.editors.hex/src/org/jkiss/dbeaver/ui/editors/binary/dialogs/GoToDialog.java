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
package org.jkiss.dbeaver.ui.editors.binary.dialogs;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.editors.binary.HexManager;
import org.jkiss.dbeaver.ui.editors.binary.internal.BinaryEditorMessages;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Go to dialog. Remembers previous state.
 *
 * @author Jordi
 */
public class GoToDialog extends Dialog {

    private static final Pattern patternDecDigits = Pattern.compile("[0-9]+"); //$NON-NLS-1$
    private static final Pattern patternHexDigits = Pattern.compile("[0-9a-fA-F]+"); //$NON-NLS-1$

    private Shell dialogShell = null;  //  @jve:decl-index=0:visual-constraint="100,50"
    private Button hexRadioButton = null;
    private Button decRadioButton = null;
    private Button showButton = null;
    private Button gotoButton = null;
    private Composite textComposite = null;
    private Text text = null;
    private Label label = null;
    private Label label2 = null;
    private long finalResult = -1L;
    private long buttonPressed = 0;

    private boolean lastHexButtonSelected = true;
    private String lastLocationText = "";
    private long limit = -1L;
    private long tempResult = -1L;

    private final SelectionAdapter defaultSelectionAdapter = new SelectionAdapter() {
        @Override
        public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
        {
            text.setFocus();
        }
    };

    public GoToDialog(Shell aShell)
    {
        super(aShell);
    }


    /**
     * This method initializes composite
     */
    private void createRadixPanel()
    {
        RowLayout rowLayout1 = new RowLayout();
//	rowLayout1.marginHeight = 5;
        rowLayout1.marginTop = 2;
        rowLayout1.marginBottom = 2;
        //rowLayout1.marginWidth = 5;
        rowLayout1.type = SWT.VERTICAL;
        Composite composite = new Composite(textComposite, SWT.NONE);
        composite.setLayout(rowLayout1);

        SelectionAdapter hexTextSelectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
            {
                text.setText(text.getText());  // generate event
                lastHexButtonSelected = e.widget == hexRadioButton;
            }
        };

        // Besides the crashes: the user always knows which number is entering, don't need any automatic
        // conversion. What does sometimes happen is one enters the right number and the wrong binary or dec was
        // selected. In that case automatic conversion is the wrong thing to do and very annoying.
        hexRadioButton = new Button(composite, SWT.RADIO);
        hexRadioButton.setText("Hex"); //$NON-NLS-1$
        hexRadioButton.addSelectionListener(defaultSelectionAdapter);
        hexRadioButton.addSelectionListener(hexTextSelectionAdapter);

        decRadioButton = new Button(composite, SWT.RADIO);
        decRadioButton.setText("Dec"); //$NON-NLS-1$
        decRadioButton.addSelectionListener(defaultSelectionAdapter);
        decRadioButton.addSelectionListener(hexTextSelectionAdapter);//decTextSelectionAdapter);
    }

    /**
     * Save the result and close dialog
     */
    private void saveResultAndClose()
    {
        lastLocationText = text.getText();
        finalResult = tempResult;
        dialogShell.close();
    }

    public long getButtonPressed()
    {
        return buttonPressed;
    }

    /**
     * This method initializes composite2
     */
    private void createButtonsPanel()
    {
        RowLayout rowLayout1 = new RowLayout();
        rowLayout1.type = org.eclipse.swt.SWT.VERTICAL;
        rowLayout1.marginHeight = 10;
        rowLayout1.marginWidth = 10;
        rowLayout1.fill = true;

        Composite composite2 = new Composite(dialogShell, SWT.NONE);
        FormData formData = new FormData();
        formData.left = new FormAttachment(textComposite);
        formData.right = new FormAttachment(100);
        composite2.setLayoutData(formData);
        composite2.setLayout(rowLayout1);

        showButton = new Button(composite2, SWT.NONE);
        showButton.setText(BinaryEditorMessages.dialog_go_to_button_show_location);
        showButton.addSelectionListener(defaultSelectionAdapter);
        showButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
            {
                buttonPressed = 1;
                saveResultAndClose();
            }
        });

        gotoButton = new Button(composite2, SWT.NONE);
        gotoButton.setText(BinaryEditorMessages.dialog_go_to_button_go_to_location);
        gotoButton.addSelectionListener(defaultSelectionAdapter);
        gotoButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
            {
                buttonPressed = 2;
                saveResultAndClose();
            }
        });

        Button closeButton = new Button(composite2, SWT.NONE);
        closeButton.setText(BinaryEditorMessages.dialog_go_to_button_close);
        closeButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
            {
                dialogShell.close();
            }
        });

        dialogShell.setDefaultButton(showButton);
    }


    /**
     * This method initializes textComposite
     */
    private void createTextPanel()
    {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        textComposite = new Composite(dialogShell, SWT.NONE);
        textComposite.setLayout(gridLayout);
        createRadixPanel();
        text = new Text(textComposite, SWT.BORDER | SWT.SINGLE);
        text.setTextLimit(30);
        int columns = 35;
        GC gc = new GC(text);
        FontMetrics fm = gc.getFontMetrics();
        int width = columns * fm.getAverageCharWidth();
        gc.dispose();
        text.setLayoutData(new GridData(width, SWT.DEFAULT));
        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                String newText = text.getText();
                int radix = 10;
                Matcher numberMatcher;
                if (hexRadioButton.getSelection()) {
                    numberMatcher = patternHexDigits.matcher(newText);
                    radix = 16;
                } else {
                    numberMatcher = patternDecDigits.matcher(newText);
                }
                tempResult = -1;
                if (numberMatcher.matches())
                    tempResult = Long.parseLong(newText, radix);

                if (tempResult >= 0L && tempResult <= limit) {
                    showButton.setEnabled(true);
                    gotoButton.setEnabled(true);
                    label2.setText(""); //$NON-NLS-1$
                } else {
                    showButton.setEnabled(false);
                    gotoButton.setEnabled(false);
                    if ("".equals(newText)) //$NON-NLS-1$
                        label2.setText(""); //$NON-NLS-1$
                    else if (tempResult < 0)
                        label2.setText(BinaryEditorMessages.dialog_go_to_label_not_number);
                    else
                        label2.setText(BinaryEditorMessages.dialog_go_to_label_out_of_range);
                }
            }
        });
        FormData formData = new FormData();
        formData.top = new FormAttachment(label);
        textComposite.setLayoutData(formData);
    }


    /**
     * This method initializes dialogShell
     */
    private void createDialogShell()
    {
        dialogShell = new Shell(getParent(), SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
        dialogShell.setText(BinaryEditorMessages.dialog_go_to_title);
        FormLayout formLayout = new FormLayout();
        formLayout.marginHeight = 3;
        formLayout.marginWidth = 3;
        dialogShell.setLayout(formLayout);
        label = new Label(dialogShell, SWT.NONE);
        FormData formData = new FormData();
        formData.left = new FormAttachment(0, 5);
        formData.right = new FormAttachment(100);
        label.setLayoutData(formData);
        createTextPanel();
        createButtonsPanel();
        label2 = new Label(dialogShell, SWT.CENTER);
        FormData formData2 = new FormData();
        formData2.left = new FormAttachment(0);
        formData2.right = new FormAttachment(100);
        formData2.top = new FormAttachment(textComposite);
        formData2.bottom = new FormAttachment(100, -10);
        label2.setLayoutData(formData2);
    }


    public long open(long aLimit)
    {
        limit = aLimit;
        finalResult = -1L;
        buttonPressed = 0;
        if (dialogShell == null || dialogShell.isDisposed()) {
            createDialogShell();
        }

        dialogShell.pack();
        HexManager.reduceDistance(getParent(), dialogShell);
        if (lastHexButtonSelected) {
            hexRadioButton.setSelection(true);
        } else {
            decRadioButton.setSelection(true);
        }
        label.setText(
            NLS.bind(BinaryEditorMessages.dialog_go_to_label_enter_location_number, limit, Long.toHexString(limit)));
        text.setText(lastLocationText);
        text.selectAll();
        text.setFocus();
        dialogShell.open();
        Display display = getParent().getDisplay();
        while (!dialogShell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }

        return finalResult;
    }
}
