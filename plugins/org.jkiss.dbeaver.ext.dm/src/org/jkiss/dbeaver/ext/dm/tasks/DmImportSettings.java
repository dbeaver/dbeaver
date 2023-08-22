package org.jkiss.dbeaver.ext.dm.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.utils.CommonUtils;

public class DmImportSettings  extends AbstractImportExportSettings<DBSObject>{

	private static final Log log = Log.getLog(DmImportSettings.class);
	
    private boolean data = true; //数据行 设置为true默认选中
    private boolean index = true; // 索引
    private boolean constraint = true; //约束
    private boolean trigger = true; //触发器
    private boolean grants =true; //权限导出
    private boolean ignore=true; // 忽略错误
    private boolean compile=true; //编译
    private boolean fastLoad=true; // 快速装载
    private boolean indexFirst=false; //导入时先创建索引
    private boolean fldrOrder=false; // 按顺序导入数据
    private boolean logWrite; //日志实时写入
    
    private boolean replaceTable;//替换已存在的表
    
    private boolean modifyBase; //修改基础属性
    
    private String inputFile; //导入的文件
    
    private String originalName; // 原模式名
    
    private String inputLogFilePattern; //日志文件
        
    private int modifyNums;//修改的数量

	public boolean isData() {
		return data;
	}

	public void setData(boolean data) {
		this.data = data;
	}

	public boolean isIndex() {
		return index;
	}

	public void setIndex(boolean index) {
		this.index = index;
	}

	public boolean isConstraint() {
		return constraint;
	}

	public void setConstraint(boolean constraint) {
		this.constraint = constraint;
	}

	public boolean isTrigger() {
		return trigger;
	}

	public void setTrigger(boolean trigger) {
		this.trigger = trigger;
	}

	public boolean isGrants() {
		return grants;
	}

	public void setGrants(boolean grants) {
		this.grants = grants;
	}

	public boolean isIgnore() {
		return ignore;
	}

	public void setIgnore(boolean ignore) {
		this.ignore = ignore;
	}

	public boolean isCompile() {
		return compile;
	}

	public void setCompile(boolean compile) {
		this.compile = compile;
	}

	public boolean isFastLoad() {
		return fastLoad;
	}

	public void setFastLoad(boolean fastLoad) {
		this.fastLoad = fastLoad;
	}

	public boolean isIndexFirst() {
		return indexFirst;
	}

	public void setIndexFirst(boolean indexFirst) {
		this.indexFirst = indexFirst;
	}

	public boolean isFldrOrder() {
		return fldrOrder;
	}

	public void setFldrOrder(boolean fldrOrder) {
		this.fldrOrder = fldrOrder;
	}

	public String getInputFile() {
		return inputFile;
	}

	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}


	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public int getModifyNums() {
		return modifyNums;
	}

	public void setModifyNums(int modifyNums) {
		this.modifyNums = modifyNums;
	}

	public boolean isLogWrite() {
		return logWrite;
	}

	public void setLogWrite(boolean logWrite) {
		this.logWrite = logWrite;
	}

	public String getInputLogFilePattern() {
		return inputLogFilePattern;
	}

	public void setInputLogFilePattern(String inputLogFilePattern) {
		this.inputLogFilePattern = inputLogFilePattern;
	}

	public boolean isModifyBase() {
		return modifyBase;
	}

	public void setModifyBase(boolean modifyBase) {
		this.modifyBase = modifyBase;
	}

	public boolean isReplaceTable() {
		return replaceTable;
	}

	public void setReplaceTable(boolean replaceTable) {
		this.replaceTable = replaceTable;
	}

	@Override // 该方法为获取配置相关
	public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
		// TODO Auto-generated method stub
		super.loadSettings(runnableContext, store);
        this.inputLogFilePattern = store.getString("DM.import.inputLogFilePattern");
        if (CommonUtils.isEmpty(this.inputLogFilePattern)) {
            this.inputLogFilePattern = "log-${database}-${timestamp}.log";
        }
		inputFile = store.getString("DM.import.inputFile");
        data = CommonUtils.getBoolean(store.getString("DM.import.data"), true);
        index = CommonUtils.getBoolean(store.getString("DM.import.index"), true);
        constraint = CommonUtils.getBoolean(store.getString("DM.import.constraint"), true);
        trigger = CommonUtils.getBoolean(store.getString("DM.import.trigger"), true);
        grants = CommonUtils.getBoolean(store.getString("DM.import.grants"), true);
        ignore = CommonUtils.getBoolean(store.getString("DM.import.ignore"), true);
        compile = CommonUtils.getBoolean(store.getString("DM.import.compile"), true);
        fastLoad = CommonUtils.getBoolean(store.getString("DM.import.fastLoad"), true);
        indexFirst = CommonUtils.getBoolean(store.getString("DM.import.indexFirst"), false);
        fldrOrder = CommonUtils.getBoolean(store.getString("DM.import.fldrOrder"), false);
        logWrite = CommonUtils.getBoolean(store.getString("DM.import.logWrite"), false);
        replaceTable = CommonUtils.getBoolean(store.getString("DM.import.replaceTable"), false);
        if (CommonUtils.isEmpty(getExtraCommandArgs())) {
            // Backward compatibility
            setExtraCommandArgs(store.getString("DM.import.extraArgs"));
        }

        originalName=store.getString("DM.import.originalName");
	}

	@Override
	public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
		// TODO Auto-generated method stub
		super.saveSettings(runnableContext, store);
		store.setValue("DM.import.inputLogFilePattern", this.inputLogFilePattern);
		store.setValue("DM.import.inputFile", inputFile);
		store.setValue("DM.import.data", data);
		store.setValue("DM.import.index", index);
		store.setValue("DM.import.constraint", constraint);
		store.setValue("DM.import.trigger", trigger);
		store.setValue("DM.import.grants", grants);
		store.setValue("DM.import.ignore", ignore);
		store.setValue("DM.import.compile", compile);
		store.setValue("DM.import.fastLoad", fastLoad);
		store.setValue("DM.import.indexFirst", indexFirst);
		store.setValue("DM.import.fldrOrder", fldrOrder);
		store.setValue("DM.import.logWrite", logWrite);
		store.setValue("DM.import.originalName", originalName);
		store.setValue("DM.import.replaceTable", replaceTable);
	}
    
}
