package org.jkiss.dbeaver.ext.dm.tools;

import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.dm.tasks.DmImportSettings;
import org.jkiss.dbeaver.tasks.nativetool.NativeToolUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

public class DmImportWizardPageSettings extends DmWizardPageSettings<DmImportWizard>{

	 private TextWithOpenFile inputFileText; //导入文件
	 private Text inputLogText; //导入记录日志
	 private Button data;
	 private Button index;
	    private Button constraint;
	    private Button trigger;
	    private Button grants;	 
	    private Button ignore;	
	    private Button compile;	
	    private Button fastLoad;	
	    private Button indexFirst;	
	    private Button fldrOrder;	
	    private Button logWrite;
	    private Button replaceTable;
	 private Text originalName;//原模式名
	DmImportWizardPageSettings(DmImportWizard wizard) {
		super(wizard, "DM 导入数据");
		setTitle("DM 还原数据");
		setDescription("还原DM 备份文件");
	}

	@Override
	public void createControl(Composite parent) {
		// TODO Auto-generated method stub
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        SelectionListener changeListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateState();
            }
        };
        Listener updateListener = event -> updateState();
        
        Group settingsGroup = UIUtils.createControlGroup(composite, "导入配置", 4, GridData.FILL_HORIZONTAL, 0);
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
        ignore = UIUtils.createCheckbox(settingsGroup, "遇到错误继续", wizard.getSettings().isIgnore());
        ignore.addSelectionListener(changeListener);
        compile = UIUtils.createCheckbox(settingsGroup, "编译", wizard.getSettings().isCompile());
        compile.addSelectionListener(changeListener);
        fastLoad = UIUtils.createCheckbox(settingsGroup, "快速装载", wizard.getSettings().isFastLoad());
        fastLoad.addSelectionListener(changeListener);
        logWrite = UIUtils.createCheckbox(settingsGroup, "日志实时写入", wizard.getSettings().isLogWrite());
        logWrite.addSelectionListener(changeListener);
        indexFirst = UIUtils.createCheckbox(settingsGroup, "导入时先创建索引", wizard.getSettings().isIndexFirst());
        indexFirst.addSelectionListener(changeListener);
        fldrOrder = UIUtils.createCheckbox(settingsGroup, "按顺序导入数据", wizard.getSettings().isFldrOrder());
        fldrOrder.addSelectionListener(changeListener);
        replaceTable = UIUtils.createCheckbox(settingsGroup, "覆盖导入", wizard.getSettings().isReplaceTable());
        replaceTable.addSelectionListener(changeListener);
        
        Group schemaGroup = UIUtils.createControlGroup(composite, "模式映射", 2, GridData.FILL_HORIZONTAL, 0);
        originalName = UIUtils.createLabelText(schemaGroup, "模式映射(只需填写原模式名)", ""); // 不加入默认值 wizard.getSettings().getOriginalName()
        
        Group inputGroup = UIUtils.createControlGroup(composite, "输入目录", 2, GridData.FILL_HORIZONTAL, 0);
        
        UIUtils.createControlLabel(inputGroup, "文件目录");
        inputFileText = new TextWithOpenFile(inputGroup, "选择备份文件", new String[] {"*.dmp","*"});

        inputFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputFileText.getTextControl().addListener(SWT.Modify, updateListener); 
        
        inputLogText = UIUtils.createLabelText(inputGroup, "日志文件名", wizard.getSettings().getInputLogFilePattern());
        UIUtils.setContentProposalToolTip(inputLogText, "Input log name pattern",
            NativeToolUtils.VARIABLE_HOST,
            NativeToolUtils.VARIABLE_DATABASE,
            NativeToolUtils.VARIABLE_TABLE,
            NativeToolUtils.VARIABLE_DATE,
            NativeToolUtils.VARIABLE_TIMESTAMP);
        ContentAssistUtils.installContentProposal(
        		inputLogText,
            new TextContentAdapter(),
            new SimpleContentProposalProvider(new String[] {
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_HOST),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_DATABASE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_TABLE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_DATE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_TIMESTAMP),
                }
            ));
       
        createExtraArgsInput(inputGroup);

        inputLogText.addModifyListener(e -> wizard.getSettings().setInputLogFilePattern(inputLogText.getText()));
        originalName.addModifyListener(e -> wizard.getSettings().setOriginalName(originalName.getText()));
        
        Composite extraGroup = UIUtils.createComposite(composite, 2);
        createSecurityGroup(extraGroup);//创建安全页面
        //wizard.createTaskSaveGroup(extraGroup); //创建保存任务界面
        setControl(composite);
	}
	
    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && !CommonUtils.isEmpty(wizard.getSettings().getInputFile());
    }
    
    
    @Override
    public void saveState() {
       DmImportSettings settings = wizard.getSettings();
       settings.setInputFile(inputFileText.getText());
       settings.setInputLogFilePattern(inputLogText.getText());
       settings.setData(data.getSelection());
       settings.setIndex(index.getSelection());
       settings.setConstraint(constraint.getSelection());
       settings.setTrigger(trigger.getSelection());
       settings.setGrants(grants.getSelection());
       settings.setLogWrite(logWrite.getSelection());
       settings.setFastLoad(fastLoad.getSelection());
       settings.setIgnore(ignore.getSelection());
       settings.setCompile(compile.getSelection());
       settings.setIndexFirst(indexFirst.getSelection());
       settings.setFldrOrder(fldrOrder.getSelection());
       settings.setOriginalName(originalName.getText());
       settings.setReplaceTable(replaceTable.getSelection());
    }

    @Override
    protected void updateState()
    {
        saveState();
        getContainer().updateButtons();
    }
    
}
