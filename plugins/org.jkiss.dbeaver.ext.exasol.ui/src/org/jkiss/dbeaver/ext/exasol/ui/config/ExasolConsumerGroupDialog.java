/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2019 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolConsumerGroup;
import org.jkiss.dbeaver.ext.exasol.ui.internal.ExasolMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.util.Locale;


public class ExasolConsumerGroupDialog extends BaseDialog {

    private static final Log log = Log.getLog(ExasolConsumerGroupDialog.class);

    private String name = "";
    private BigDecimal cpuWeight = null;
    private String comment = "";
    private ExasolConsumerGroup group;

    private Text precedenceText;
    private Text userRamLimitText;
    private Text groupRamLimitText;
    private Text sessionRamLimitText;

    public ExasolConsumerGroupDialog(Shell parentShell, ExasolConsumerGroup group) {
        super(parentShell, ExasolMessages.dialog_create_consumer_group, null);
        this.group = group;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        final Composite composite = super.createDialogArea(parent);

        final Composite group = new Composite(composite, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 250;
        gd.verticalIndent = 0;
        gd.horizontalIndent = 0;
        group.setLayoutData(gd);
        group.setLayout(new GridLayout(2, false));
        final Text nameText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_group_name, "");
        final Text cpuWeightText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_group_cpu_weight, "");
        precedenceText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_precedence, "");
        userRamLimitText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_group_user_limit, "");
        groupRamLimitText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_group_group_limit, "");
        sessionRamLimitText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_group_session_limit, "");
        cpuWeightText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        userRamLimitText.addVerifyListener(UIUtils.getUnsignedLongOrEmptyTextVerifyListener(userRamLimitText));
        groupRamLimitText.addVerifyListener(UIUtils.getUnsignedLongOrEmptyTextVerifyListener(groupRamLimitText));
        sessionRamLimitText.addVerifyListener(UIUtils.getUnsignedLongOrEmptyTextVerifyListener(sessionRamLimitText));
        precedenceText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));

        final Text commentText = UIUtils.createLabelText(group, ExasolMessages.dialog_priority_group_description, "");

        ModifyListener mod = e -> {
            name = nameText.getText();
            try {
                cpuWeight = new BigDecimal(cpuWeightText.getText());
            } catch (NumberFormatException ex) {
                log.debug("Can't format to number " + cpuWeightText.getText());
            }
            comment = commentText.getText();

            // enable/disable OK button
            Button button = getButton(IDialogConstants.OK_ID);
            if (button != null) {
                if (name.isEmpty() | cpuWeight == null) {
                    button.setEnabled(false);
                } else {
                    button.setEnabled(true);
                }
            }
        };

        nameText.addModifyListener(mod);
        commentText.addModifyListener(mod);
        cpuWeightText.addModifyListener(mod);

        return composite;
    }

    public String getName() {
        return name;
    }


    private int getCpuWeight() {
        return cpuWeight.intValue();
    }

    private String getComment() {
        return comment;
    }

    private BigDecimal getUserRamLimit() {
        String text = userRamLimitText.getText();
        return CommonUtils.isNotEmpty(text) ? new BigDecimal(text) : null;
    }

    private BigDecimal getGroupRamLimit() {
        String text = groupRamLimitText.getText();
        return CommonUtils.isNotEmpty(text) ? new BigDecimal(text) : null;
    }

    private BigDecimal getSessionRamLimit() {
        String text = sessionRamLimitText.getText();
        return CommonUtils.isNotEmpty(text) ? new BigDecimal(text) : null;
    }

    private Integer getPrecedence() {
        String text = precedenceText.getText();
        return CommonUtils.isNotEmpty(text) ? CommonUtils.toInt(text) : null;
    }

    @Override
    protected void okPressed() {
        this.group.setCpuWeight(getCpuWeight());
        this.group.setDescription(getComment());
        this.group.setGroupRamLimit(getGroupRamLimit());
        this.group.setName(getName());
        this.group.setPrecedence(getPrecedence());
        this.group.setSessionRamLimit(getSessionRamLimit());
        this.group.setUserRamLimit(getUserRamLimit());
        super.okPressed();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        Button button = getButton(IDialogConstants.OK_ID);
        if (button != null) {
            button.setEnabled(false);
        }
    }

}
