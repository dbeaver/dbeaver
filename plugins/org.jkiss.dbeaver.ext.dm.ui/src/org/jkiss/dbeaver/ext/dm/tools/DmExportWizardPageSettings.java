/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.dm.tools;

import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.dm.tasks.DmExportSettings;
import org.jkiss.dbeaver.tasks.nativetool.NativeToolUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;


/**
 * 该类为在导出的数据库基础上进行相关配置，即进行相关配置的选择
 *  
 * 创建导出的配置页面
 * 
 * 该页面创建配置页面，并将配置相关内容存入DmExportSettings 配置类中
 * 
 * @author saorionesan
 *
 */
class DmExportWizardPageSettings extends DmWizardPageSettings<DmExportWizard> {

    private Text outputFolderText;
    private Text outputFileText;
    private Text outputLogText;
    
    private Button data;
    private Button index;
    private Button constraint;
    private Button trigger;
    private Button grants;
    private Button compress;
    private Button drop;
    private Button logWrite;
    private Button includeTableSpace;
    
    private int nums;
    

    DmExportWizardPageSettings(DmExportWizard wizard)
    {
        super(wizard, "导出");
        setTitle("导出配置");
        setDescription(("导出配置页面"));
    }

    @Override
    public boolean isPageComplete()
    {
//        return super.isPageComplete() && wizard.getSettings().getOutputFolder() != null;
        return super.isPageComplete() && new File(wizard.getSettings().getOutputFile()) != null;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        SelectionListener changeListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateState();
            }
        };

        Group settingsGroup = UIUtils.createControlGroup(composite, "导出配置", 3, GridData.FILL_HORIZONTAL, 0);
        data = UIUtils.createCheckbox(settingsGroup,"数据行", wizard.getSettings().isData());
        data.addSelectionListener(changeListener);
        index = UIUtils.createCheckbox(settingsGroup,"索引", wizard.getSettings().isIndex());
        index.addSelectionListener(changeListener);
        constraint = UIUtils.createCheckbox(settingsGroup, "约束", wizard.getSettings().isConstraint());
        constraint.addSelectionListener(changeListener);
        trigger = UIUtils.createCheckbox(settingsGroup, "触发器", wizard.getSettings().isTrigger());
        trigger.addSelectionListener(changeListener);
        grants = UIUtils.createCheckbox(settingsGroup, "权限", wizard.getSettings().isGrants());
        grants.addSelectionListener(changeListener);
        compress = UIUtils.createCheckbox(settingsGroup, "压缩", wizard.getSettings().isCompress());
        compress.addSelectionListener(changeListener);
        drop = UIUtils.createCheckbox(settingsGroup, "导出后删除", wizard.getSettings().isDrop());
        drop.addSelectionListener(changeListener);
        logWrite = UIUtils.createCheckbox(settingsGroup, "日志实时写入", wizard.getSettings().isLogWrite());
        logWrite.addSelectionListener(changeListener);
        includeTableSpace = UIUtils.createCheckbox(settingsGroup, "包含表空间", wizard.getSettings().isIncludeTableSpace());
        includeTableSpace.addSelectionListener(changeListener);

        
        Group outputGroup = UIUtils.createControlGroup(composite, "输出目录", 2, GridData.FILL_HORIZONTAL, 0);
        outputFolderText = DialogUtils.createOutputFolderChooser(outputGroup, "导出文件夹", e -> updateState());
        outputFileText = UIUtils.createLabelText(outputGroup, "导出文件名", wizard.getSettings().getOutputFile());
        UIUtils.setContentProposalToolTip(outputFileText, "Output file name pattern",
            NativeToolUtils.VARIABLE_HOST,
            NativeToolUtils.VARIABLE_DATABASE,
            NativeToolUtils.VARIABLE_TABLE,
            NativeToolUtils.VARIABLE_DATE,
            NativeToolUtils.VARIABLE_TIMESTAMP);
        ContentAssistUtils.installContentProposal(
            outputFileText,
            new TextContentAdapter(),
            new SimpleContentProposalProvider(new String[] {
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_HOST),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_DATABASE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_TABLE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_DATE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_TIMESTAMP),
                }
            ));
        
        outputLogText = UIUtils.createLabelText(outputGroup, "日志文件名", wizard.getSettings().getOutputLogFilePattern());
        UIUtils.setContentProposalToolTip(outputLogText, "Output log name pattern",
            NativeToolUtils.VARIABLE_HOST,
            NativeToolUtils.VARIABLE_DATABASE,
            NativeToolUtils.VARIABLE_TABLE,
            NativeToolUtils.VARIABLE_DATE,
            NativeToolUtils.VARIABLE_TIMESTAMP);
        ContentAssistUtils.installContentProposal(
        		outputLogText,
            new TextContentAdapter(),
            new SimpleContentProposalProvider(new String[] {
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_HOST),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_DATABASE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_TABLE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_DATE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_TIMESTAMP),
                }
            ));
        

        createExtraArgsInput(outputGroup);

        if (new File(wizard.getSettings().getOutputFile()) != null) {
//            outputFolderText.setText(wizard.getSettings().getOutputFolder().getAbsolutePath());
            outputFolderText.setText(wizard.getSettings().getOutputFile());
        }

        outputFileText.addModifyListener(e -> wizard.getSettings().setOutputFile(outputFileText.getText()));
        outputLogText.addModifyListener(e -> wizard.getSettings().setOutputLogFilePattern(outputLogText.getText()));
        
        Composite extraGroup = UIUtils.createComposite(composite, 2);
        createSecurityGroup(extraGroup);//创建安全页面
        //wizard.createTaskSaveGroup(extraGroup); //创建保存任务界面

        setControl(composite);
    }

    @Override
    public void saveState() {
        super.saveState();

        DmExportSettings settings = wizard.getSettings();

        String fileName = outputFolderText.getText();
//        wizard.getSettings().setOutputFolder(CommonUtils.isEmpty(fileName) ? null : new File(fileName));
        wizard.getSettings().setOutputFile(CommonUtils.isEmpty(fileName) ? null : fileName);
        settings.setOutputFile(outputFileText.getText());
        settings.setOutputLogFilePattern(outputLogText.getText());
        settings.setData(data.getSelection());
        settings.setIndex(index.getSelection());
        settings.setConstraint(constraint.getSelection());
        settings.setTrigger(trigger.getSelection());
        settings.setGrants(grants.getSelection());
        settings.setCompress(compress.getSelection());
        settings.setDrop(drop.getSelection());
        settings.setLogWrite(logWrite.getSelection());
        settings.setIncludeTableSpace(includeTableSpace.getSelection());
    }

    @Override
    protected void updateState()
    {
        saveState();
        getContainer().updateButtons();
    }

}
