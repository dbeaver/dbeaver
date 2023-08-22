package org.jkiss.dbeaver.ext.dm.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolHandler;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolSettings;
import org.jkiss.utils.CommonUtils;

public abstract class DmNativeToolHandler <SETTINGS extends AbstractNativeToolSettings<BASE_OBJECT>, BASE_OBJECT extends DBSObject, PROCESS_ARG> // 确保子类两个BASE_OBJECT 泛型所选定的类型一致
extends AbstractNativeToolHandler<SETTINGS, BASE_OBJECT, PROCESS_ARG> {
	
	 protected String quoteString="\""+"\""+"\"";
	
    @Override
    protected void setupProcessParameters(DBRProgressMonitor monitor,SETTINGS settings, PROCESS_ARG arg, ProcessBuilder process) {
        String userPassword = settings.getToolUserPassword();
        if (CommonUtils.isEmpty(userPassword)) {
            userPassword = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserPassword();
        }
        if (!CommonUtils.isEmpty(userPassword)) {
            process.environment().put("DM_PWD", userPassword);
        }/* else {
            // Empty password?
            process.environment().put(MySQLConstants.ENV_VARIABLE_MYSQL_PWD, "");
        }*/
    }
    
    

    protected List<String> getDmToolCommandLine(AbstractNativeToolHandler<SETTINGS, BASE_OBJECT, PROCESS_ARG> handler, SETTINGS settings, PROCESS_ARG arg) throws IOException {
        java.util.List<String> cmd = new ArrayList<>();
        /**
         * DM 中先组合用户名和连接信息，最后在组合相关参数
         */
        fillProcessParameters(settings, arg, cmd);
        return cmd;
    }

    public void checkButtons(DmExportSettings settings) {
   	 int nums=0;
        if(!settings.isData()) { // 此处导出配置有问题，注意！！！
        	settings.setModifyBase(true);
        	nums++;
        }
        if(!settings.isIndex()) {
        	settings.setModifyBase(true);
        	nums++;
        }
        if(!settings.isConstraint()) {
        	settings.setModifyBase(true);
        	nums++;
        }
        if(!settings.isTrigger()) {
        	settings.setModifyBase(true);
        	nums++;
        }
        if(!settings.isGrants()) {
        	settings.setModifyBase(true);
        	nums++;
        }
        settings.setModifyNums(nums);
    }
    
    public void checkAddSem(DmExportSettings settings,StringBuilder exclude) {
    Integer nums=settings.getModifyNums();
      if(nums>1) {
    	 exclude.append(","); 
    	 // nums-- 右减减，先进行操作再自减1，--nums 左减减，先自减1再进行操作
    	 settings.setModifyNums(--nums); 
      }
    }
        
}
