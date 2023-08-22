package org.jkiss.dbeaver.ext.dm.tasks;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.DmTableBase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.utils.CommonUtils;

public class DmExportSettings extends AbstractImportExportSettings<DBSObject>{
	private static final Log log = Log.getLog(DmExportSettings.class);
	
	public List<DmSchemaExportInfo> exportObjects = new ArrayList<>();
    private boolean data = true; //数据行 设置为true默认选中
    private boolean index = true; // 索引
    private boolean constraint = true; //约束
    private boolean trigger = true; //触发器
    private boolean grants =true; //权限导出
    private boolean compress; //压缩
    private boolean drop; //导出后删除
    private boolean logWrite; //日志实时写入
    private boolean includeTableSpace;  //定义包含表空间
    private boolean modifyBase; //修改基础属性
    private boolean showViews;
    private int modifyNums;//修改的数量
    
    private String outputFile;
    
   private String outputLogFilePattern;
    
	public List<DmSchemaExportInfo> getExportObjects() {
		return exportObjects;
	}
	public void setExportObjects(List<DmSchemaExportInfo> exportObjects) {
		this.exportObjects = exportObjects;
	}
	
	public String getOutputLogFilePattern() {
		return outputLogFilePattern;
	}
	public void setOutputLogFilePattern(String outputLogFilePattern) {
		this.outputLogFilePattern = outputLogFilePattern;
	}
	public boolean isData() {
		return data;
	}
	
	public String getOutputFile() {
		return outputFile;
	}
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
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
	public boolean isCompress() {
		return compress;
	}
	public void setCompress(boolean compress) {
		this.compress = compress;
	}
	public boolean isDrop() {
		return drop;
	}
	public void setDrop(boolean drop) {
		this.drop = drop;
	}
	public boolean isLogWrite() {
		return logWrite;
	}
	public void setLogWrite(boolean logWrite) {
		this.logWrite = logWrite;
	}
	public boolean isIncludeTableSpace() {
		return includeTableSpace;
	}
	public void setIncludeTableSpace(boolean includeTableSpace) {
		this.includeTableSpace = includeTableSpace;
	}
	
	public boolean isModifyBase() {
		return modifyBase;
	}
	public void setModifyBase(boolean modifyBase) {
		this.modifyBase = modifyBase;
	}
	public boolean isShowViews() {
		return showViews;
	}
	public void setShowViews(boolean showViews) {
		this.showViews = showViews;
	}
    
    public int getModifyNums() {
		return modifyNums;
	}
	public void setModifyNums(int modifyNums) {
		this.modifyNums = modifyNums;
	}
	public void fillExportObjectsFromInput() {
        Map<DmSchema, List<DmTableBase>> objMap = new LinkedHashMap<>();
        for (DBSObject object : getDatabaseObjects()) {
        	DmSchema catalog = null;
            if (object instanceof DmSchema) {
                catalog = (DmSchema) object;
            } else if (object instanceof DmTableBase) {
                catalog = ((DmTableBase) object).getContainer();
            }
            if (catalog == null) {
                log.error("Can't determine export catalog");
                continue;
            }
            List<DmTableBase> tables = objMap.computeIfAbsent(catalog, DmSchema -> new ArrayList<>());
            if (object instanceof DmTableBase) {
                tables.add((DmTableBase) object);
            }
        }
        for (Map.Entry<DmSchema, List<DmTableBase>> entry : objMap.entrySet()) {
            getExportObjects().add(new DmSchemaExportInfo(entry.getKey(), entry.getValue()));
            
        }
        updateDataSourceContainer();
    }


    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
        super.loadSettings(runnableContext, store);
        this.outputLogFilePattern = store.getString("DM.export.outputLogFilePattern");
        if (CommonUtils.isEmpty(this.outputLogFilePattern)) {
            this.outputLogFilePattern = "log-${database}-${timestamp}.log";
        }
        this.outputFile = store.getString("DM.export.outputFilePattern"); // 不同的备份文件名分开获取
        if (CommonUtils.isEmpty(this.outputFile)) {
            this.outputFile = "dump-${database}-${timestamp}.dmp";
        }
        data = CommonUtils.getBoolean(store.getString("DM.export.data"), true);
        index = CommonUtils.getBoolean(store.getString("DM.export.index"), true);
        constraint = CommonUtils.getBoolean(store.getString("DM.export.constraint"), true);
        trigger = CommonUtils.getBoolean(store.getString("DM.export.trigger"), true);
        grants = CommonUtils.getBoolean(store.getString("DM.export.grants"), true);
        compress = CommonUtils.getBoolean(store.getString("DM.export.compress"), false);
        drop = CommonUtils.getBoolean(store.getString("DM.export.drop"), false); //设置为true默认选中
        logWrite = CommonUtils.getBoolean(store.getString("DM.export.logWrite"), false);
        includeTableSpace = CommonUtils.getBoolean(store.getString("DM.export.includeTableSpace"), false);
        showViews = CommonUtils.getBoolean(store.getString("DM.export.showViews"), false);
        if (CommonUtils.isEmpty(getExtraCommandArgs())) {
            // Backward compatibility
            setExtraCommandArgs(store.getString("DM.export.extraArgs"));
        }

        if (store instanceof DBPPreferenceMap) {
            // Save input objects to task properties
            List<Map<String, Object>> objectList = ((DBPPreferenceMap) store).getObject("exportObjects");
            if (!CommonUtils.isEmpty(objectList)) {
                for (Map<String, Object> object : objectList) {
                    String catalogId = CommonUtils.toString(object.get("catalog"));
                    if (!CommonUtils.isEmpty(catalogId)) {
                        List<String> tableNames = (List<String>) object.get("tables");
                        DmSchemaExportInfo exportInfo = loadDatabaseExportInfo(runnableContext, catalogId, tableNames);
                        if (exportInfo != null) {
                            exportObjects.add(exportInfo);
                        }
                    }
                }
            }
        }
    }

    private DmSchemaExportInfo loadDatabaseExportInfo(DBRRunnableContext runnableContext, String catalogId, List<String> tableNames) {
        DmSchemaExportInfo[] exportInfo = new DmSchemaExportInfo[1];
        try {
            runnableContext.run(false, true, monitor -> {
                try {
                    DmSchema catalog = (DmSchema) DBUtils.findObjectById(monitor, getProject(), catalogId);
                    if (catalog == null) {
                        throw new DBException("Catalog " + catalogId + " not found");
                    }
                    List<DmTableBase> tables = null;
                    if (!CommonUtils.isEmpty(tableNames)) {
                        tables = new ArrayList<>();
                        for (String tableName : tableNames) {
                            DmTableBase table = catalog.getTable(monitor, tableName);
                            if (table != null) {
                                tables.add(table);
                            }
                        }
                    }
                    exportInfo[0] = new DmSchemaExportInfo(catalog, tables);
                } catch (Throwable e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            log.error("Error loading objects configuration", e);
        } catch (InterruptedException e) {
            // Ignore
        }
        return exportInfo[0];
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
        super.saveSettings(runnableContext, store);
        store.setValue("DM.export.outputLogFilePattern", this.outputLogFilePattern); //key 值DM.export.outputFilePattern 决定是否多个数据库共用同一个属性
        store.setValue("DM.export.outputFilePattern", this.outputFile);
        store.setValue("DM.export.index", index);
        store.setValue("DM.export.data", data);
        store.setValue("DM.export.constraint", constraint);
        store.setValue("DM.export.trigger", trigger);
        store.setValue("DM.export.grants", grants);
        store.setValue("DM.export.compress", compress);
        store.setValue("DM.export.drop", drop);
        store.setValue("DM.export.logWrite", logWrite);
        store.setValue("DM.export.includeTableSpace", includeTableSpace);
        store.setValue("DM.export.showViews", showViews);
        if (store instanceof DBPPreferenceMap && !CommonUtils.isEmpty(exportObjects)) {
            // Save input objects to task properties
            List<Map<String, Object>> objectList = new ArrayList<>();
            for (DmSchemaExportInfo object : exportObjects) {
                Map<String, Object> objInfo = new LinkedHashMap<>();
                objInfo.put("catalog", DBUtils.getObjectFullId(object.getDatabase()));
                if (!CommonUtils.isEmpty(object.getTables())) {
                    List<String> tableList = new ArrayList<>();
                    for (DmTableBase table : object.getTables()) {
                        tableList.add(table.getName());
                    }
                    objInfo.put("tables", tableList);
                }
                objectList.add(objInfo);
            }

            ((DBPPreferenceMap) store).getPropertyMap().put("exportObjects", objectList);
        }
    }
    
    

}
