/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.editors.binary.BinaryTextFinder;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.ui.editors.binary.HexManager;
import org.jkiss.dbeaver.ui.editors.binary.internal.BinaryEditorMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Find/Replace dialog with hex/text, forward/backward, and ignore case options. Remembers previous
 * state, in case it has been closed by the user and reopened again.
 *
 * @author Jordi
 */
public class FindReplaceDialog extends Dialog {


    private static final Pattern patternHexDigits = Pattern.compile("[0-9a-fA-F]*"); //$NON-NLS-1$
    private static final String text1Replacement = BinaryEditorMessages.dialog_find_replace_1_replacement;
    private static final String textBackward = BinaryEditorMessages.dialog_find_replace_backward;
    private static final String textCancel = BinaryEditorMessages.dialog_find_replace_cancel;
    private static final String textClose = BinaryEditorMessages.dialog_find_replace_close;
    private static final String textDirection = BinaryEditorMessages.dialog_find_replace_direction;
    private static final String textError = BinaryEditorMessages.dialog_find_replace_error_;
    private static final String textFind = BinaryEditorMessages.dialog_find_replace_find;
    private static final String textFindLiteral = BinaryEditorMessages.dialog_find_replace_find_literal;
    private static final String textFindReplace = BinaryEditorMessages.dialog_find_replace_find_replace;
    private static final String textForward = BinaryEditorMessages.dialog_find_replace_forward;
    private static final String textFoundLiteral = BinaryEditorMessages.dialog_find_replace_found_literal;
    private static final String textHex = "Hex"; //$NON-NLS-1$
    private static final String textIgnoreCase = BinaryEditorMessages.dialog_find_replace_ignore_case;
    private static final String textLiteralNotFound = BinaryEditorMessages.dialog_find_replace_literal_not_found;
    private static final String textNewFind = BinaryEditorMessages.dialog_find_replace_new_find;
    private static final String textReplace = BinaryEditorMessages.dialog_find_replace_replace;
    private static final String textReplaceAll = BinaryEditorMessages.dialog_find_replace_replace_all;
    private static final String textReplaceFind = BinaryEditorMessages.dialog_find_replace_replace_find;
    private static final String textReplaceWith = BinaryEditorMessages.dialog_find_replace_replace_with;
    private static final String textReplacements = BinaryEditorMessages.dialog_find_replace_replacements;
    private static final String textSearching = BinaryEditorMessages.dialog_find_replace_searching;
    private static final String textStop = BinaryEditorMessages.dialog_find_replace_stop;
    private static final String textText = BinaryEditorMessages.dialog_find_replace_text;

    SelectionAdapter defaultSelectionAdapter = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
            if (lastIgnoreCase != checkBox.getSelection() ||
                lastForward != forwardRadioButton.getSelection() ||
                lastFindHexButtonSelected != findGroup.hexRadioButton.getSelection() ||
                lastReplaceHexButtonSelected != replaceGroup.hexRadioButton.getSelection()) {
                feedbackLabel.setText(""); //$NON-NLS-1$
            }
            lastFocused.textCombo.setFocus();
        }
    };

    private static final List<Object[]> findReplaceFindList = new ArrayList<>();
    private static final List<Object[]> findReplaceReplaceList = new ArrayList<>();

    private HexEditControl editControl = null;
    private TextHexInputGroup lastFocused = null;
    private boolean lastForward = true;
    private boolean lastFindHexButtonSelected = true;
    private boolean lastReplaceHexButtonSelected = true;
    private boolean lastIgnoreCase = false;
    private boolean searching = false;

    // visual components
    private Shell sShell = null;
    private TextHexInputGroup findGroup = null;
    private TextHexInputGroup replaceGroup = null;
    private Group directionGroup = null;
    private Button forwardRadioButton = null;
    private Button backwardRadioButton = null;
    private Button checkBox = null;
    private Composite findReplaceButtonsComposite = null;
    private Button findButton = null;
    private Button replaceFindButton = null;
    private Button replaceButton = null;
    private Button replaceAllButton = null;
    private Label feedbackLabel = null;
    private Composite progressComposite = null;
    private ProgressBar progressBar = null;
    private Button progressCancelButton = null;
    private Button closeButton = null;


    /**
     * Group with text/hex selector and text input
     */
    class TextHexInputGroup {
        private List<Object[]> items = null;  // list of tuples {String, Boolean}
        // visual components
        private Group group = null;
        private Composite composite = null;
        private Button hexRadioButton = null;
        private Button textRadioButton = null;
        private Combo textCombo = null;

        public TextHexInputGroup(List<Object[]> oldItems)
        {
            items = oldItems;
        }

        private void initialise()
        {
            group = new Group(sShell, SWT.NONE);
            GridLayout gridLayout = new GridLayout();
            gridLayout.numColumns = 2;
            group.setLayout(gridLayout);
            createComposite();
            textCombo = new Combo(group, SWT.BORDER);
            int columns = 35;
            GC gc = new GC(textCombo);
            FontMetrics fm = gc.getFontMetrics();
            int width = columns * fm.getAverageCharWidth();
            gc.dispose();
            textCombo.setLayoutData(new GridData(width, SWT.DEFAULT));
            textCombo.addVerifyListener(new VerifyListener() {
                @Override
                public void verifyText(org.eclipse.swt.events.VerifyEvent e)
                {
                    if (e.keyCode == 0) return;  // a list selection
                    if (hexRadioButton.getSelection()) {
                        Matcher numberMatcher = patternHexDigits.matcher(e.text);
                        e.doit = numberMatcher.matches();
                    }
                }
            });
            textCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    int index = textCombo.getSelectionIndex();
                    if (index < 0) return;

                    Boolean selection = (Boolean) (items == null ? null : (items.get(index))[1]);
                    if (selection != null) {
                        refreshHexOrText(selection);
                    }
                }
            });
            textCombo.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    feedbackLabel.setText(""); //$NON-NLS-1$
                    if (TextHexInputGroup.this == findGroup) {
                        enableDisableControls();
                    }
                }
            });
        }

        /**
         * This method initializes composite
         */
        private void createComposite()
        {
            RowLayout rowLayout1 = new RowLayout();
            rowLayout1.marginTop = 2;
            rowLayout1.marginBottom = 2;
            rowLayout1.type = SWT.VERTICAL;
            composite = new Composite(group, SWT.NONE);
            composite.setLayout(rowLayout1);
            hexRadioButton = new Button(composite, SWT.RADIO);
            hexRadioButton.setText(textHex);
            hexRadioButton.addSelectionListener(defaultSelectionAdapter);
            hexRadioButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
                {
                    Matcher numberMatcher = patternHexDigits.matcher(textCombo.getText());
                    if (!numberMatcher.matches())
                        textCombo.setText(""); //$NON-NLS-1$
                }
            });
            textRadioButton = new Button(composite, SWT.RADIO);
            textRadioButton.setText(textText);
            textRadioButton.addSelectionListener(defaultSelectionAdapter);
        }


        private void refreshCombo()
        {
            if (items == null) return;

            if (textCombo.getItemCount() > 0) {
                textCombo.remove(0, textCombo.getItemCount() - 1);
            }
            for (Object[] item : items) {
                String itemString = (String) item[0];
                textCombo.add(itemString);
            }
            if (!items.isEmpty()) {
                textCombo.setText((String) items.get(0)[0]);
            }
            selectText();
        }


        private void refreshHexOrText(boolean hex)
        {
            hexRadioButton.setSelection(hex);
            textRadioButton.setSelection(!hex);
        }


        private void rememberText()
        {
            String lastText = textCombo.getText();
            if ("".equals(lastText) || items == null) return; //$NON-NLS-1$

            for (Iterator<Object[]> iterator = items.iterator(); iterator.hasNext();) {
                String itemString = (String)iterator.next()[0];
                if (lastText.equals(itemString)) {
                    iterator.remove();
                }
            }
            items.add(0, new Object[]{lastText, hexRadioButton.getSelection()});
            refreshCombo();
        }


        private void selectText()
        {
            textCombo.setSelection(new Point(0, textCombo.getText().length()));
        }

        private void setEnabled(boolean enabled)
        {
            group.setEnabled(enabled);
            hexRadioButton.setEnabled(enabled);
            textRadioButton.setEnabled(enabled);
            textCombo.setEnabled(enabled);
        }
    }


    /**
     * Create find/replace dialog always on top of shell
     *
     * @param aShell where it is displayed
     */
    public FindReplaceDialog(Shell aShell)
    {
        super(aShell);
    }


    private void activateProgressBar()
    {
        Display.getCurrent().timerExec(500, new Runnable() {
            @Override
            public void run()
            {
                if (searching && !progressComposite.isDisposed()) {
                    progressComposite.setVisible(true);
                }
            }
        });
        long max = editControl.getContent().length();
        long min = editControl.getCaretPos();
        if (backwardRadioButton.getSelection()) {
            max = min;
            min = 0L;
        }
        int factor = 0;
        while (max > Integer.MAX_VALUE) {
            max = max >>> 1;
            min = min >>> 1;
            ++factor;
        }
        progressBar.setMaximum((int) max);
        progressBar.setMinimum((int) min);
        progressBar.setSelection(0);
        final int finalFactor = factor;
        Display.getCurrent().timerExec(1000, new Runnable() {
            @Override
            public void run()
            {
                if (!searching || progressBar.isDisposed()) return;

                int selection = 0;
                if (editControl.getFinder() != null) {
                    selection = (int) (editControl.getFinder().getSearchPosition() >>> finalFactor);
                    if (backwardRadioButton.getSelection()) {
                        selection = progressBar.getMaximum() - selection;
                    }
                }
                progressBar.setSelection(selection);
                Display.getCurrent().timerExec(1000, this);
            }
        });
    }


    /**
     * Open and display the dialog.
     */
    public void open()
    {
        if (sShell == null || sShell.isDisposed()) {
            createSShell();
        }
        sShell.pack();
        HexManager.reduceDistance(getParent(), sShell);
        findGroup.refreshCombo();
        long selectionLength = editControl.getSelection()[1] - editControl.getSelection()[0];
        if (selectionLength > 0L && selectionLength <= BinaryTextFinder.MAX_SEQUENCE_SIZE) {
            findGroup.refreshHexOrText(true);
            checkBox.setEnabled(false);
            StringBuilder selectedText = new StringBuilder();
            byte[] selection = new byte[(int) selectionLength];
            try {
                editControl.getContent().get(ByteBuffer.wrap(selection), editControl.getSelection()[0]);
            }
            catch (IOException e) {
                throw new IllegalStateException(e);
            }
            for (int i = 0; i < selectionLength; ++i) {
                selectedText.append(GeneralUtils.byteToHex[selection[i] & 0x0ff]);
            }
            findGroup.textCombo.setText(selectedText.toString());
            findGroup.selectText();
        } else {
            findGroup.refreshHexOrText(lastFindHexButtonSelected);
            checkBox.setEnabled(!lastFindHexButtonSelected);
        }

        replaceGroup.refreshHexOrText(lastReplaceHexButtonSelected);
        replaceGroup.refreshCombo();

        checkBox.setSelection(lastIgnoreCase);
        if (lastForward)
            forwardRadioButton.setSelection(true);
        else
            backwardRadioButton.setSelection(true);
        feedbackLabel.setText(textNewFind);

        lastFocused = findGroup;
        lastFocused.textCombo.setFocus();
        enableDisableControls();
        sShell.open();
    }


    /**
     * This method initializes find/replace buttons composite
     */
    private void createFindReplaceButtonsComposite()
    {
        GridLayout gridLayout = new GridLayout();
        gridLayout.marginHeight = 5;
        gridLayout.marginWidth = 0;
        gridLayout.numColumns = 2;
        gridLayout.makeColumnsEqualWidth = true;
        findReplaceButtonsComposite = new Composite(sShell, SWT.NONE);
        FormData formData = new FormData();
        formData.top = new FormAttachment(directionGroup);
        formData.left = new FormAttachment(0);
        formData.right = new FormAttachment(100);
        findReplaceButtonsComposite.setLayoutData(formData);
        findReplaceButtonsComposite.setLayout(gridLayout);

        findButton = new Button(findReplaceButtonsComposite, SWT.NONE);
        findButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        findButton.setText(textFind);
        findButton.addSelectionListener(defaultSelectionAdapter);
        findButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                doFind();
            }
        });
        replaceFindButton = new Button(findReplaceButtonsComposite, SWT.NONE);
        replaceFindButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        replaceFindButton.setText(textReplaceFind);
        replaceFindButton.addSelectionListener(defaultSelectionAdapter);
        replaceFindButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
            {
                doReplaceFind();
            }
        });
        replaceButton = new Button(findReplaceButtonsComposite, SWT.NONE);
        replaceButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        replaceButton.setText(textReplace);
        replaceButton.addSelectionListener(defaultSelectionAdapter);
        replaceButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
            {
                doReplace();
            }
        });
        replaceAllButton = new Button(findReplaceButtonsComposite, SWT.NONE);
        replaceAllButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        replaceAllButton.setText(textReplaceAll);
        replaceAllButton.addSelectionListener(defaultSelectionAdapter);
        replaceAllButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
            {
                doReplaceAll();
            }
        });
        sShell.setDefaultButton(findButton);
    }


    /**
     * This method initializes composite3
     */
    private void createIgnoreCaseComposite()
    {
        FillLayout fillLayout = new FillLayout();
        fillLayout.marginHeight = 10;
        fillLayout.marginWidth = 10;
        Composite ignoreCaseComposite = new Composite(sShell, SWT.NONE);
        ignoreCaseComposite.setLayout(fillLayout);
        checkBox = new Button(ignoreCaseComposite, SWT.CHECK);
        checkBox.setText(textIgnoreCase);
        checkBox.addSelectionListener(defaultSelectionAdapter);
        FormData formData = new FormData();
        formData.top = new FormAttachment(replaceGroup.group);
        formData.left = new FormAttachment(directionGroup);
        ignoreCaseComposite.setLayoutData(formData);
    }


    /**
     * This method initializes group1
     */
    private void createDirectionGroup()
    {
        RowLayout rowLayout = new RowLayout();
        rowLayout.fill = true;
        rowLayout.type = org.eclipse.swt.SWT.VERTICAL;
        directionGroup = new Group(sShell, SWT.NONE);
        directionGroup.setText(textDirection);
        FormData formData = new FormData();
        formData.top = new FormAttachment(replaceGroup.group);
        directionGroup.setLayoutData(formData);
        directionGroup.setLayout(rowLayout);
        forwardRadioButton = new Button(directionGroup, SWT.RADIO);
        forwardRadioButton.setText(textForward);
        forwardRadioButton.addSelectionListener(defaultSelectionAdapter);
        backwardRadioButton = new Button(directionGroup, SWT.RADIO);
        backwardRadioButton.setText(textBackward);
        backwardRadioButton.addSelectionListener(defaultSelectionAdapter);
    }


    /**
     * This method initializes sShell
     */
    private void createSShell()
    {
        sShell = new Shell(getParent(), SWT.MODELESS | SWT.DIALOG_TRIM);
        sShell.setText(textFindReplace);
        FormLayout formLayout = new FormLayout();
        formLayout.marginHeight = 5;
        formLayout.marginWidth = 5;
        formLayout.spacing = 5;
        sShell.setLayout(formLayout);
        sShell.addShellListener(new ShellAdapter() {
            @Override
            public void shellActivated(ShellEvent e)
            {
                enableDisableControls();
            }
        });

        if (findGroup == null) {
            findGroup = new TextHexInputGroup(findReplaceFindList);
        }
        findGroup.initialise();
        findGroup.group.setText(textFindLiteral);
        SelectionAdapter hexTextSelectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                checkBox.setEnabled(e.widget == findGroup.textRadioButton);
            }
        };
        findGroup.textRadioButton.addSelectionListener(hexTextSelectionAdapter);
        findGroup.hexRadioButton.addSelectionListener(hexTextSelectionAdapter);

        if (replaceGroup == null) {
            replaceGroup = new TextHexInputGroup(findReplaceReplaceList);
        }
        replaceGroup.initialise();
        replaceGroup.group.setText(textReplaceWith);
        FormData formData = new FormData();
        formData.top = new FormAttachment(findGroup.group);
        replaceGroup.group.setLayoutData(formData);

        createDirectionGroup();
        createIgnoreCaseComposite();
        createFindReplaceButtonsComposite();

        Composite feedbackComposite = new Composite(sShell, SWT.NONE);
        FormData formData2 = new FormData();
        formData2.top = new FormAttachment(findReplaceButtonsComposite);
        formData2.left = new FormAttachment(0);
        formData2.bottom = new FormAttachment(100);
        feedbackComposite.setLayoutData(formData2);
        FormLayout formLayout2 = new FormLayout();
//	formLayout2.spacing = 5;
        feedbackComposite.setLayout(formLayout2);

        feedbackLabel = new Label(feedbackComposite, SWT.CENTER);
        feedbackLabel.setText(textNewFind);
        FormData formData3 = new FormData();
        formData3.top = new FormAttachment(0);
        formData3.left = new FormAttachment(0);
        formData3.right = new FormAttachment(100);
        feedbackLabel.setLayoutData(formData3);

        progressComposite = new Composite(feedbackComposite, SWT.NONE);
        FormLayout formLayout3 = new FormLayout();
        formLayout3.spacing = 5;
        progressComposite.setLayout(formLayout3);
        FormData formData4 = new FormData();
        formData4.top = new FormAttachment(feedbackLabel);
        formData4.bottom = new FormAttachment(100);
        formData4.left = new FormAttachment(0);
        formData4.right = new FormAttachment(100);
        progressComposite.setLayoutData(formData4);
//progressComposite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));

        progressBar = new ProgressBar(progressComposite, SWT.NONE);
        FormData formData5 = new FormData();
        formData5.bottom = new FormAttachment(100);
        formData5.left = new FormAttachment(0);
        formData5.height = progressBar.computeSize(SWT.DEFAULT, SWT.DEFAULT, false).y;
        progressBar.setLayoutData(formData5);

        progressCancelButton = new Button(progressComposite, SWT.NONE);
        progressCancelButton.setText(textCancel);
        FormData formData6 = new FormData();
        formData6.right = new FormAttachment(100);
        progressCancelButton.setLayoutData(formData6);
        formData5.right = new FormAttachment(progressCancelButton);
        progressCancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                editControl.stopSearching();
            }
        });
        progressComposite.setVisible(false);

        closeButton = new Button(sShell, SWT.NONE);
        closeButton.setText(textClose);
        FormData formData1 = new FormData();
        formData1.right = new FormAttachment(100);
        formData1.bottom = new FormAttachment(100);
        closeButton.setLayoutData(formData1);
        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                sShell.close();
            }
        });

        formData2.right = new FormAttachment(closeButton);

        sShell.addListener(SWT.Close, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                editControl.stopSearching();
            }
        });
    }

    private void doReplace()
    {
        replace();
        enableDisableControls();
        feedbackLabel.setText(""); //$NON-NLS-1$
    }

    private void doReplaceFind()
    {
        replace();
        doFind();
    }

    private void doFind()
    {
        prepareToRun();
        progressCancelButton.setText(textCancel);
        String message = textLiteralNotFound;
        String literal = findGroup.textCombo.getText();
        if (editControl != null && literal.length() > 0) {
            try {
                if (editControl.findAndSelect(literal, findGroup.hexRadioButton.getSelection(),
                                           forwardRadioButton.getSelection(), checkBox.getSelection()))
                    message = textFoundLiteral;
            }
            catch (IOException e) {
                message = textError + e;
            }
        }
        endOfRun(message);
    }


    private void doReplaceAll()
    {
        prepareToRun();
        progressCancelButton.setText(textStop);
        String message = textLiteralNotFound;
        String literal = findGroup.textCombo.getText();
        if (editControl != null && literal.length() > 0) {
            try {
                int replacements = editControl.replaceAll(literal, findGroup.hexRadioButton.getSelection(),
                                                       forwardRadioButton.getSelection(), checkBox.getSelection(),
                                                       replaceGroup.textCombo.getText(),
                                                       replaceGroup.hexRadioButton.getSelection());
                message = replacements + textReplacements;
                if (replacements == 1) {
                    message = text1Replacement;
                }
            }
            catch (IOException e) {
                message = textError + e;
            }
        }
        endOfRun(message);
    }


    private void enableDisableControls()
    {
        findGroup.setEnabled(!searching);
        replaceGroup.setEnabled(!searching);

        directionGroup.setEnabled(!searching);
        forwardRadioButton.setEnabled(!searching);
        backwardRadioButton.setEnabled(!searching);

        checkBox.setEnabled(!searching);

        findButton.setEnabled(!searching);
        replaceFindButton.setEnabled(!searching);
        replaceButton.setEnabled(!searching);
        replaceAllButton.setEnabled(!searching);

        closeButton.setEnabled(!searching);
//		getParent().setEnabled(enableButtons);
        if (searching) {
            return;
        }

        boolean somethingToFind = findGroup.textCombo.getText().length() > 0;
        findButton.setEnabled(somethingToFind);
        replaceAllButton.setEnabled(somethingToFind);
        long selectionLength = 0L;
        if (editControl != null) {
            selectionLength = editControl.getSelection()[1] - editControl.getSelection()[0];
        }
        replaceFindButton.setEnabled(selectionLength > 0L && somethingToFind);
        replaceButton.setEnabled(selectionLength > 0L);
    }


    private void endOfRun(String message)
    {
        searching = false;
        if (progressComposite.isDisposed()) return;

        progressComposite.setVisible(false);
        feedbackLabel.setText(message);
        enableDisableControls();
    }


    private void prepareToRun()
    {
        searching = true;
        lastFindHexButtonSelected = findGroup.hexRadioButton.getSelection();
        lastReplaceHexButtonSelected = replaceGroup.hexRadioButton.getSelection();
        replaceGroup.rememberText();
        findGroup.rememberText();
        lastForward = forwardRadioButton.getSelection();
        lastIgnoreCase = checkBox.getSelection();
        feedbackLabel.setText(textSearching);
        enableDisableControls();
        activateProgressBar();
    }


    private void replace()
    {
        editControl.replace(replaceGroup.textCombo.getText(), replaceGroup.hexRadioButton.getSelection());
    }

    /**
     * Set the target editor to search
     *
     * @param aTarget with data to search
     */
    public void setTarget(HexEditControl aTarget)
    {
        editControl = aTarget;
    }
}
