/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.ui;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.exasol.ExasolMessages;
import org.jkiss.dbeaver.ext.exasol.model.ExasolConsumerGroup;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;


public class ExasolConsumerGroupDialog extends BaseDialog {

    private String name = "";
    private BigDecimal cpuWeight = null;
    private BigDecimal userRamLimit = null;
	private BigDecimal groupRamLimit = null;
    private BigDecimal sessionRamLimit = null;
    private BigDecimal precedence = null;
    private String comment = "";

    public ExasolConsumerGroupDialog(Shell parentShell, ExasolConsumerGroup group) {
        super(parentShell, ExasolMessages.dialog_create_consumer_group, null);
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
        group.setLayout(new GridLayout(2, true));
        group.setLayout(new GridLayout(2, false));
        final Text nameText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_group_name, "");
        final Text cpuWeightText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_group_cpu_weight, "");
        final Text precedenceText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_precedence, "");
        final Text userRamLimitText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_group_user_limit, "");
        final Text groupRamLimitText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_group_group_limit, "");
        final Text sessionRamLimitText = UIUtils.createLabelText(group, ExasolMessages.dialog_consumer_group_session_limit, "");
        cpuWeightText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        userRamLimitText.addVerifyListener(UIUtils.getLongVerifyListener(userRamLimitText));
        groupRamLimitText.addVerifyListener(UIUtils.getLongVerifyListener(groupRamLimitText));
        sessionRamLimitText.addVerifyListener(UIUtils.getLongVerifyListener(sessionRamLimitText));
        precedenceText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        
        Map<Text, BigDecimal> limits = new HashMap<Text, BigDecimal>();
        limits.put(cpuWeightText, cpuWeight);
        limits.put(sessionRamLimitText, sessionRamLimit);
        limits.put(groupRamLimitText, groupRamLimit);
        limits.put(userRamLimitText, userRamLimit);
        limits.put(precedenceText, precedence);

        final Text commentText = UIUtils.createLabelText(group, ExasolMessages.dialog_priority_group_description, "");

        ModifyListener mod = new ModifyListener() {
            @SuppressWarnings("unused")
			@Override
            public void modifyText(ModifyEvent e) {
                name = nameText.getText();
                try {
                	cpuWeight = new BigDecimal(cpuWeightText.getText());
                } catch (NumberFormatException ex) {
                }
                comment = commentText.getText();
                
                for (Entry<Text, BigDecimal> entry  : limits.entrySet()) {
                	String text = entry.getKey().getText();
                	BigDecimal value = entry.getValue();
                    if (! ("".compareTo(text) ==0) )
                    {
                    	try {
    						value = new BigDecimal(text);
    					} catch (NumberFormatException e2) {
    					}
                    }
				}
                
                //enable/disable OK button   
                if (name.isEmpty() | cpuWeight == null) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                } else {
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
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


    public int getCpuWeight() {
        return cpuWeight.intValue();
    }

    public String getComment() {
        return comment;
    }

    public BigDecimal getUserRamLimit() {
		return userRamLimit;
	}

	public BigDecimal getGroupRamLimit() {
		return groupRamLimit;
	}

	public BigDecimal getSessionRamLimit() {
		return sessionRamLimit;
	}
	
	public Integer getPrecedence() {
		return Integer.valueOf(precedence.intValue());
	}


    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

}
