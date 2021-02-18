/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

/**
 * Boolean rendering utils
 */
public class BooleanRenderer {

    public enum Style {
        CHECKBOX(true, "Checkboxes", "Unicode checkbox symbols", String.valueOf(CHAR_BOOL_NULL), String.valueOf(CHAR_BOOL_TRUE), String.valueOf(CHAR_BOOL_FALSE)),
        TEXTBOX(true, "Textboxes", "ASCII checkbox symbols", DBConstants.NULL_VALUE_LABEL, "[v]", "[  ]"),
        ICON("Icons", "Checkbox icons"),
        TRUE_FALSE(true, "True/False", "Textual representation", DBConstants.NULL_VALUE_LABEL, String.valueOf(true), String.valueOf(false)),
        YES_NO(true, "Yes/No", "Localized textual representation", DBConstants.NULL_VALUE_LABEL, "yes", "no");

        public boolean isText() {
            return text;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public DBPImage getImage(Boolean value) {
            if (value == null) {
                return UIIcon.CHECK_QUEST;
            } else if (value) {
                return UIIcon.CHECK_ON;
            } else {
                return UIIcon.CHECK_OFF;
            }
        }

        public String getText(Boolean value) {
            return text ? String.valueOf(value == null ? textNull : (value ? textTrue : textFalse)) : "";
        }

        private final boolean text;
        private final String displayName;
        private final String description;
        private final String textNull, textTrue, textFalse;

        Style(String displayName, String description) {
            this(false, displayName, description, null, null, null);
        }

        Style(boolean text,String displayName, String description, String textNull, String textTrue, String textFalse) {
            this.text = text;
            this.displayName = displayName;
            this.description = description;
            this.textNull = textNull;
            this.textTrue = textTrue;
            this.textFalse = textFalse;
        }
    }

    public static final char CHAR_BOOL_FALSE = 0x2610;
    public static final char CHAR_BOOL_TRUE = 0x2611;
    public static final char CHAR_BOOL_NULL = 0x2612;

    private static final String PREF_NAME_BOOLEAN_STYLE = "ui.render.boolean.style"; //$NON-NLS-1$

    private static Image imageCheckboxEnabledOn;
    private static Image imageCheckboxEnabledOff;
    private static Image imageCheckboxDisabledOn;
    private static Image imageCheckboxDisabledOff;

    public static Style getDefaultStyle() {
        return CommonUtils.valueOf(
            Style.class,
            DBWorkbench.getPlatform().getPreferenceStore().getString(PREF_NAME_BOOLEAN_STYLE),
            Style.TEXTBOX);
    }

    public static void setDefaultStyle(Style style) {
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_NAME_BOOLEAN_STYLE, style.name());
    }

    public static Image getImageCheckboxEnabledOn()
    {
        if (imageCheckboxEnabledOn == null) {
            initImages();
        }
        return imageCheckboxEnabledOn;
    }

    public static Image getImageCheckboxEnabledOff()
    {
        if (imageCheckboxEnabledOff == null) {
            initImages();
        }
        return imageCheckboxEnabledOff;
    }

    public static Image getImageCheckboxDisabledOn()
    {
        if (imageCheckboxDisabledOn == null) {
            initImages();
        }
        return imageCheckboxDisabledOn;
    }

    public static Image getImageCheckboxDisabledOff()
    {
        if (imageCheckboxDisabledOff == null) {
            initImages();
        }
        return imageCheckboxDisabledOff;
    }

    private static synchronized void initImages()
    {
        // Capture checkbox image - only for windows
        // There could be hard-to-understand problems in Linux
        /*if (!DBeaverCore.getInstance().getLocalSystem().isWindows())*/ {
        imageCheckboxEnabledOff = DBeaverIcons.getImage(UIIcon.CHECK_OFF);
        imageCheckboxEnabledOn = DBeaverIcons.getImage(UIIcon.CHECK_ON);
        imageCheckboxDisabledOn = makeDisableImage(DBeaverIcons.getImage(UIIcon.CHECK_ON));
        imageCheckboxDisabledOff = makeDisableImage(DBeaverIcons.getImage(UIIcon.CHECK_OFF));
    }

/*
        final Shell shell = DBeaverUI.getActiveWorkbenchShell();
        Button checkBox = new Button(shell, SWT.CHECK);
        checkBox.setVisible(true);
        final Color borderColor = shell.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        checkBox.setBackground(borderColor);
        Point checkboxSize = checkBox.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        checkBox.setBounds(0, 0, checkboxSize.x, checkboxSize.y);
        try {
            checkBox.setSelection(false);
            imageCheckboxEnabledOff = captureWidget(checkBox, borderColor, DBeaverIcons.getImage(UIIcon.CHECK_OFF));
            checkBox.setSelection(true);
            imageCheckboxEnabledOn = captureWidget(checkBox, borderColor, DBeaverIcons.getImage(UIIcon.CHECK_ON));
            checkBox.setEnabled(false);
            imageCheckboxDisabledOn = captureWidget(checkBox, borderColor, DBeaverIcons.getImage(UIIcon.CHECK_ON));
            checkBox.setSelection(false);
            imageCheckboxDisabledOff = captureWidget(checkBox, borderColor, DBeaverIcons.getImage(UIIcon.CHECK_OFF));
        } finally {
            UIUtils.dispose(checkBox);
        }
*/
    }

    private static Image makeDisableImage(Image image) {
        return new Image(image.getDevice(), image, SWT.IMAGE_GRAY);
    }

}
