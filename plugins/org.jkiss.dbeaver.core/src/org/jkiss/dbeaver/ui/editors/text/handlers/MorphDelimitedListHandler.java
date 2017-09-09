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
package org.jkiss.dbeaver.ui.editors.text.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public final class MorphDelimitedListHandler extends AbstractTextHandler {

    @Override
    public Object execute(ExecutionEvent executionEvent) throws ExecutionException {
        Shell activeShell = HandlerUtil.getActiveShell(executionEvent);
        BaseTextEditor textEditor = BaseTextEditor.getTextEditor(HandlerUtil.getActiveEditor(executionEvent));
        if (textEditor != null) {
            IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
            ISelectionProvider provider = textEditor.getSelectionProvider();
            if (provider != null) {
                ISelection selection = provider.getSelection();
                if (selection instanceof ITextSelection) {
                    ITextSelection textSelection = (ITextSelection) selection;
                    if (textSelection.getLength() <= 0) {
                        UIUtils.showMessageBox(activeShell, "Morph text", "Text selection is empty. You need to select some text to morph", SWT.ICON_INFORMATION);
                        return null;
                    }
                    String formattedText = morphText(activeShell, textSelection.getText());
                    if (formattedText != null) {
                        try {
                            document.replace(textSelection.getOffset(), textSelection.getLength(), formattedText);
                        } catch (BadLocationException e) {
                            DBeaverUI.getInstance().showError("Morph text", "Error replacing text", e);
                        }
                    }
                }
            }
        }

        return null;
    }

    private String morphText(Shell activeShell, String text) {
        ConfigDialog configDialog = new ConfigDialog(activeShell);
        if (configDialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        List<String> tokens = new ArrayList<>();
        MorphDelimitedListSettings settings = configDialog.morphSettings;
        String sourceDelimiter = settings.getSourceDelimiter();

        // Fix line feed
        if (sourceDelimiter.contains("\n") && !sourceDelimiter.contains("\r")) {
            sourceDelimiter += "\r";
        }

        StringTokenizer st = new StringTokenizer(text, sourceDelimiter);
        while (st.hasMoreTokens()) {
            tokens.add(st.nextToken());
        }
        StringBuilder buf = new StringBuilder();
        if (!CommonUtils.isEmpty(settings.getLeadingText())) {
            buf.append(settings.getLeadingText());
        }
        int lastLineFeed = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (!CommonUtils.isEmpty(settings.getQuoteString())) {
                buf.append(settings.getQuoteString()).append(token).append(settings.getQuoteString());
                lastLineFeed += settings.getQuoteString().length() * 2 + token.length();
            } else {
                buf.append(token);
                lastLineFeed += token.length();
            }
            if (i < tokens.size() - 1) {
                buf.append(settings.getTargetDelimiter());
                lastLineFeed += settings.getTargetDelimiter().length();

                if (settings.wrapLine > 0) {
                    int nextTokenLength = tokens.get(i + 1).length();
                    if (!CommonUtils.isEmpty(settings.getQuoteString())) {
                        nextTokenLength += settings.getQuoteString().length() * 2;
                    }
                    if (lastLineFeed + nextTokenLength > settings.wrapLine) {
                        buf.append("\n");
                        lastLineFeed = 0;
                    }
                }
            }
        }

        if (!CommonUtils.isEmpty(settings.getTrailingText())) {
            buf.append(settings.getTrailingText());
        }

        return buf.toString();
    }

    public static class MorphDelimitedListSettings {
        private String sourceDelimiter;
        private String targetDelimiter;
        private String quoteString;
        private int wrapLine;
        private String leadingText;
        private String trailingText;

        public MorphDelimitedListSettings() {
        }

        public String getSourceDelimiter() {
            return sourceDelimiter;
        }

        public void setSourceDelimiter(String sourceDelimiter) {
            this.sourceDelimiter = sourceDelimiter;
        }

        public String getTargetDelimiter() {
            return targetDelimiter;
        }

        public void setTargetDelimiter(String targetDelimiter) {
            this.targetDelimiter = targetDelimiter;
        }

        public String getQuoteString() {
            return quoteString;
        }

        public void setQuoteString(String quoteString) {
            this.quoteString = quoteString;
        }

        public int getWrapLine() {
            return wrapLine;
        }

        public void setWrapLine(int wrapLine) {
            this.wrapLine = wrapLine;
        }

        public String getLeadingText() {
            return leadingText;
        }

        public void setLeadingText(String leadingText) {
            this.leadingText = leadingText;
        }

        public String getTrailingText() {
            return trailingText;
        }

        public void setTrailingText(String trailingText) {
            this.trailingText = trailingText;
        }
    }

    public static class ConfigDialog extends Dialog {

        static final String PARAM_SOURCE_DELIMITER = "sourceDelimiter";
        static final String PARAM_TARGET_DELIMITER = "targetDelimiter";
        static final String PARAM_QUOTE_STRING = "quoteString";
        static final String PARAM_WRAP_LINE = "wrapLine";
        static final String PARAM_LEADING_TEXT = "leadingText";
        static final String PARAM_TRAILING_TEXT = "trailingText";

        protected final IDialogSettings settings;

        private Combo sourceDelimCombo;

        private Combo targetDelimCombo;
        private Combo quoteStringCombo;
        private Spinner wrapLineAtColumn;
        private Text leadingText;
        private Text trailingText;

        protected MorphDelimitedListSettings morphSettings;

        protected ConfigDialog(Shell shell)
        {
            super(shell);
            settings = UIUtils.getDialogSettings("MorphDelimitedListConfigDialog");
            morphSettings = new MorphDelimitedListSettings();
            morphSettings.setSourceDelimiter("\t\n,");
            morphSettings.setTargetDelimiter(",");
            morphSettings.setQuoteString("\"");
            morphSettings.setWrapLine(80);
            if (settings.get(PARAM_SOURCE_DELIMITER) != null) {
                morphSettings.setSourceDelimiter(settings.get(PARAM_SOURCE_DELIMITER));
            }
            if (settings.get(PARAM_TARGET_DELIMITER) != null) {
                morphSettings.setTargetDelimiter(settings.get(PARAM_TARGET_DELIMITER));
            }
            if (settings.get(PARAM_QUOTE_STRING) != null) {
                morphSettings.setQuoteString(settings.get(PARAM_QUOTE_STRING));
            }
            if (settings.get(PARAM_WRAP_LINE) != null) {
                try {
                    morphSettings.setWrapLine(Integer.parseInt(settings.get(PARAM_WRAP_LINE)));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            if (settings.get(PARAM_LEADING_TEXT) != null) {
                morphSettings.setLeadingText(settings.get(PARAM_LEADING_TEXT));
            }
            if (settings.get(PARAM_TRAILING_TEXT) != null) {
                morphSettings.setTrailingText(settings.get(PARAM_TRAILING_TEXT));
            }
        }

        @Override
        protected IDialogSettings getDialogBoundsSettings() {
            return settings;
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText("Delimited text options");
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            Composite group = (Composite)super.createDialogArea(parent);
            ((GridLayout)group.getLayout()).numColumns = 1;

            int textWidthHint = UIUtils.getFontHeight(parent) * 10;

            {
                Group sourceGroup = UIUtils.createControlGroup(group, "Source", 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
                sourceDelimCombo = UIUtils.createDelimiterCombo(sourceGroup, "Column Delimiter", new String[] {"\n", "\t", ";", ","}, morphSettings.getSourceDelimiter(), true);
                ((GridData) sourceDelimCombo.getLayoutData()).widthHint = textWidthHint;
            }
            {
                Group targetGroup = UIUtils.createControlGroup(group, "Target", 2, GridData.FILL_BOTH, SWT.DEFAULT);
                targetDelimCombo = UIUtils.createDelimiterCombo(targetGroup, "Result delimiter", new String[] {"\n", "\t", ";", ","}, morphSettings.getTargetDelimiter(), false);
                quoteStringCombo = UIUtils.createDelimiterCombo(targetGroup, "String quote character", new String[] {"\"", "'"}, morphSettings.getQuoteString(), false);
                wrapLineAtColumn = UIUtils.createLabelSpinner(targetGroup, "Wrap line at column", "Inserts line feeds after spcified number of characters. Zero means no wrap.", morphSettings.getWrapLine(), 0, Integer.MAX_VALUE);
                leadingText = UIUtils.createLabelText(targetGroup, "Leading text", morphSettings.getLeadingText(), SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
                ((GridData) leadingText.getLayoutData()).widthHint = textWidthHint;
                ((GridData) leadingText.getLayoutData()).verticalAlignment = GridData.FILL;
                ((GridData) leadingText.getLayoutData()).grabExcessVerticalSpace = true;
                trailingText = UIUtils.createLabelText(targetGroup, "Trailing text", morphSettings.getTrailingText(), SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
                ((GridData) trailingText.getLayoutData()).widthHint = textWidthHint;
                ((GridData) trailingText.getLayoutData()).verticalAlignment = GridData.FILL;
                ((GridData) trailingText.getLayoutData()).grabExcessVerticalSpace = true;
            }


            return group;
        }

        @Override
        protected void okPressed() {
            morphSettings.setSourceDelimiter(CommonUtils.unescapeDisplayString(sourceDelimCombo.getText()));
            morphSettings.setTargetDelimiter(CommonUtils.unescapeDisplayString(targetDelimCombo.getText()));
            morphSettings.setQuoteString(CommonUtils.unescapeDisplayString(quoteStringCombo.getText()));
            morphSettings.setWrapLine(wrapLineAtColumn.getSelection());
            morphSettings.setLeadingText(leadingText.getText());
            morphSettings.setTrailingText(trailingText.getText());

            settings.put(PARAM_SOURCE_DELIMITER, morphSettings.getSourceDelimiter());
            settings.put(PARAM_TARGET_DELIMITER, morphSettings.getTargetDelimiter());
            settings.put(PARAM_QUOTE_STRING, morphSettings.getQuoteString());
            settings.put(PARAM_WRAP_LINE, morphSettings.getWrapLine());
            settings.put(PARAM_LEADING_TEXT, morphSettings.getLeadingText());
            settings.put(PARAM_TRAILING_TEXT, morphSettings.getTrailingText());
            super.okPressed();
        }
    }

}