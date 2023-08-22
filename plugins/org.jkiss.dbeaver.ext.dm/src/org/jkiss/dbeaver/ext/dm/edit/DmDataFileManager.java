package org.jkiss.dbeaver.ext.dm.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmDataFile;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.DmTablespace;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

/**
 * datafile manager
 * @author saorionesan
 *
 */
public class DmDataFileManager extends SQLObjectEditor<DmDataFile, DmDataSource>{

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Override
	public DBSObjectCache<? extends DBSObject, DmDataFile> getObjectsCache(DmDataFile object) {
		
		return object.getTablespace().fileCache;
	}

	@Override
	protected DmDataFile createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		DmTablespace tablespace=(DmTablespace)container;
		DmDataFile dataFile=new DmDataFile(tablespace.getDataSource(), tablespace);
		return dataFile;
	}

	//在表空间下增加一个新datafile
	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmDataFile, DmDataSource>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
        DmDataFile dataFile=(DmDataFile)command.getObject();
        StringBuffer sql = new StringBuffer("ALTER TABLESPACE \"" + dataFile.getTablespace().getName().toUpperCase()+"\"");
        boolean autoExtend = dataFile.isAutoExtensible();
        sql.append(" ADD DATAFILE '" + dataFile.getFile() + "' ");
        /**if(CommonUtils.isNotEmpty(dataFile.getMirrorPath())) { //先去掉镜像路径
         sql.append("MIRROR '"+dataFile.getMirrorPath()+"' ");
        }*/
        sql.append(" SIZE "+dataFile.getTotalSize()).append(" AUTOEXTEND " + (autoExtend ? "ON" : "OFF"));
        if (autoExtend) {
            sql.append(" NEXT "+dataFile.getNextExtSize());
            sql.append(" MAXSIZE "+dataFile.getMaxExtSize());
        }
        actions.add(new SQLDatabasePersistAction("Add DataFile", sql.toString()));
	}

	
	
	@Override //修改已有的datafile
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<DmDataFile, DmDataSource>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
        DmDataFile dataBaseFile = (DmDataFile)command.getObject();
        Map<Object,Object> properties = command.getProperties();
        if (properties.size() > 0 && dataBaseFile.isPersisted()){//只有当dataFile 为已存在的才能修改
            String src = dataBaseFile.getOldfile();
            String tar = dataBaseFile.getFile();
            actionList.addAll(makeModifyNotRename(dataBaseFile, properties)); //正常修改
            if (properties.get("name") != null && !src.equals(tar)) {
            	actionList.add(new SQLDatabasePersistAction("OFF Tablespace", "ALTER TABLESPACE \""+dataBaseFile.getTablespace().getName()+"\" OFFLINE;"));
                actionList.add(makeModifyRename(dataBaseFile, properties)); //增加重命名SQL
                actionList.add(new SQLDatabasePersistAction("ON Tablespace", "ALTER TABLESPACE \""+dataBaseFile.getTablespace().getName()+"\" ONLINE;"));
            }
        }
		
	}

	
	private List<DBEPersistAction> makeModifyNotRename(DmDataFile dataBaseFile, Map<Object, Object> properties) { //没有重命名语句时增加的SQL语句
     	List<DBEPersistAction>	actions=new ArrayList<>();
	    if(properties.get("totalSize")!=null) {//修改总大小
	    	StringBuffer buffer=new StringBuffer();
	    	buffer.append("ALTER TABLESPACE \"" + dataBaseFile.getTablespace().getName().toUpperCase()+"\"");
	    	buffer.append(" RESIZE DATAFILE '"+dataBaseFile.getName()+"' TO "+dataBaseFile.getTotalSize());
	    	actions.add(new SQLDatabasePersistAction("RESIZE", buffer.toString()));
	    }
	    /**
	     * 此处需要修改自动扩容的逻辑
	     * 
	     * 1. 未修改自动扩容属性的情况下: 创建时支持自动扩容并修改了扩容尺寸或者总尺寸，创建时不支持自动扩容
	     * 2. 修改了扩容属性情况下: 关闭了自动扩容、打开自动扩容并修改了扩容尺寸或者总尺寸
	     */
        Object value=properties.get("autoExtensible");
        if(value !=null) {
    	    Boolean extend =(Boolean)value;
         	if(extend) {//开启自动扩展
    	    	StringBuffer buffer=new StringBuffer();
    	    	buffer.append("ALTER TABLESPACE \"" + dataBaseFile.getTablespace().getName().toUpperCase()+"\" DATAFILE '"+dataBaseFile.getName()+"' ");
    	    	Object nextSize=properties.get("nextExtSize");
    	    	Object maxSize=properties.get("maxExtSize");
    	    	if(nextSize!=null || maxSize!=null) {
    	    	   buffer.append(" AUTOEXTEND ON NEXT "+dataBaseFile.getNextExtSize()+" MAXSIZE "+dataBaseFile.getMaxExtSize());
    	    	   actions.add(new SQLDatabasePersistAction("AUTOEXTEND ON",buffer.toString()));
    	    	}
         	}else {
    			StringBuffer buffer =new StringBuffer();
    			buffer.append("ALTER TABLESPACE \"" + dataBaseFile.getTablespace().getName().toUpperCase()+"\" DATAFILE '"+dataBaseFile.getName()+"' AUTOEXTEND OFF");
    			actions.add(new SQLDatabasePersistAction("AUTOEXTEND OFF",buffer.toString()));
    		}
        } else { //未修改扩容属性情况下
			if(dataBaseFile.isAutoExtensible()) { //只需要考虑支持自动扩容的情况
    	    	StringBuffer buffer=new StringBuffer();
    	    	buffer.append("ALTER TABLESPACE \"" + dataBaseFile.getTablespace().getName().toUpperCase()+"\" DATAFILE '"+dataBaseFile.getName()+"' ");
    	    	Object nextSize=properties.get("nextExtSize");
    	    	Object maxSize=properties.get("maxExtSize");
    	    	if(nextSize!=null || maxSize!=null) {
    	    	   buffer.append(" AUTOEXTEND ON NEXT "+dataBaseFile.getNextExtSize()+" MAXSIZE "+dataBaseFile.getMaxExtSize());
    	    	   actions.add(new SQLDatabasePersistAction("Alter Size",buffer.toString()));
    	    	}
			}
		}    	
		return actions;
	}

	private DBEPersistAction makeModifyRename(DmDataFile dataBaseFile, Map<Object, Object> properties) {//增加重命名SQL语句
        StringBuffer sb = new StringBuffer();
        sb.append("ALTER TABLESPACE ");
        sb.append("\""+dataBaseFile.getTablespace().getName()+"\"");
        sb.append(" RENAME DATAFILE '" + dataBaseFile.getName() + "'");
        sb.append(" TO '" + dataBaseFile.getFile() + "'");
        return new SQLDatabasePersistAction("Modify Datafile", sb.toString());	
	}

	//不可删除，该方法为空
	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmDataFile, DmDataSource>.ObjectDeleteCommand command,
			Map<String, Object> options) throws DBException {
	}
	
}
