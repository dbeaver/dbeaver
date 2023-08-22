package org.jkiss.dbeaver.ext.dm.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.nativetool.NativeToolUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

public class DmSchemaImportHandler extends DmNativeToolHandler<DmImportSettings, DBSObject, DmSchema>{

	private PrintStream logWriter; //日志输出		
	
	@Override
	public Collection<DmSchema> getRunInfo(DmImportSettings settings) {
		// TODO Auto-generated method stub
		List<DBSObject> objects=settings.getDatabaseObjects();
		DBSObject object=objects.get(0);
		if(object instanceof DmSchema) {
			return Collections.singleton((DmSchema)object);
		}
		return null; 
	}
	
	@Override //获取导入配置相关
	protected DmImportSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
		DmImportSettings settings = new DmImportSettings();
		settings.loadSettings(context, new TaskPreferenceStore(task));
		return settings;
	}

	@Override
	protected List<String> getCommandLine(DmImportSettings settings, DmSchema arg) throws IOException {
		List<String> cmd = getDmToolCommandLine(this, settings, arg);
		/**
		 * if (!CommonUtils.isEmpty(arg.getTables())) {
		 * cmd.add(arg.getDatabase().getName()); for (DmTable table : arg.getTables()) {
		 * cmd.add(table.getName()); } } else { cmd.add(arg.getDatabase().getName()); }
		 */

		if(!CommonUtils.isEmpty(settings.getOriginalName())) {
		cmd.add("REMAP_SCHEMA="+settings.getOriginalName()+":"+arg.getName());	
		}else {
			cmd.add("SCHEMAS=(" + arg.getName() + ")");
		}
		return cmd;
	}

	@Override
	public void fillProcessParameters(DmImportSettings settings, DmSchema arg, List<String> cmd) throws IOException {
		// TODO Auto-generated method stub
		File dumpBinary = RuntimeUtils.getNativeClientBinary(settings.getClientHome(), "bin", "dimp"); //$NON-NLS-1$
		String dumpPath = dumpBinary.getAbsolutePath();
		cmd.add(dumpPath);
		/*
		 * 添加导出的用户名等
		 */
		DBPConnectionConfiguration connectionInfo = settings.getDataSourceContainer()
				.getActualConnectionConfiguration();
		StringBuilder stringBuilder = new StringBuilder();
		String toolUserName = settings.getToolUserName();
		if (CommonUtils.isEmpty(toolUserName)) {
			toolUserName = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserName();
		}
		if (!CommonUtils.isEmpty(toolUserName)) {
			stringBuilder.append(quoteString+toolUserName+quoteString);
		}
		String password = connectionInfo.getUserPassword(); // 密码
		if (!CommonUtils.isEmpty(password)) {
			stringBuilder.append("/");
			stringBuilder.append(quoteString+password+quoteString);
		}
		stringBuilder.append("@" + connectionInfo.getHostName());
		if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
			stringBuilder.append(":" + connectionInfo.getHostPort());
		}
		cmd.add(stringBuilder.toString());
		
		String outLogName = GeneralUtils.replaceVariables(settings.getInputLogFilePattern(), name -> {
			switch (name) {
			case NativeToolUtils.VARIABLE_DATABASE:
				return arg.getName();
			case NativeToolUtils.VARIABLE_HOST:
				return arg.getDataSource().getContainer().getConnectionConfiguration().getHostName();
			case NativeToolUtils.VARIABLE_TIMESTAMP:
				return RuntimeUtils.getCurrentTimeStamp();
			case NativeToolUtils.VARIABLE_DATE:
				return RuntimeUtils.getCurrentDate();
			default:
				System.getProperty(name);
			}
			return null;
		});
		
		cmd.add("FILE="+settings.getInputFile());
		
		if(settings.isIgnore()) {
			cmd.add("IGNORE=Y");
		}else {
			cmd.add("IGNORE=N");
		}
		
		if(settings.isCompile()) {
			cmd.add("COMPILE=Y");
		}else {
			cmd.add("COMPILE=N");
		}
		
		if(settings.isFastLoad()) {
			cmd.add("FAST_LOAD=Y");
		}else {
			cmd.add("FAST_LOAD=N");
		}
		
		if(settings.isFldrOrder()) {
			cmd.add("FLDR_ORDER=Y");
		}else {
			cmd.add("FLDR_ORDER=N");
		}
		
		if(settings.isIndexFirst()) {
			cmd.add("INDEXFIRST=Y");
		}else {
            cmd.add("INDEXFIRST=N");
		}
		
		if(settings.isReplaceTable()) {
			cmd.add("TABLE_EXISTS_ACTION=REPLACE");
		}
		
        checkButtons(settings);
        if(settings.isModifyBase()) {
            StringBuilder exclude=new StringBuilder();
            exclude.append("EXCLUDE=(");
            if(!settings.isData()) {
            	exclude.append("ROWS");
            	checkAddSem(settings, exclude); //检测是否需要添加分号
            }
            if(!settings.isIndex()) {
            	exclude.append("INDEXES");
            	checkAddSem(settings, exclude); //检测是否需要添加分号
            }
            if(!settings.isConstraint()) {
            	exclude.append("CONSTRAINTS");
            	checkAddSem(settings, exclude); //检测是否需要添加分号
            }
            if(!settings.isTrigger()) {
            	exclude.append("TRIGGERS");
            	checkAddSem(settings, exclude); //检测是否需要添加分号
            }
            if(!settings.isGrants()) {
            	exclude.append("GRANTS");
            	checkAddSem(settings, exclude); //检测是否需要添加分号
            }
            exclude.append(")");
            cmd.add(exclude.toString());
        }
		
        cmd.add("LOG="+outLogName);
        if(settings.isLogWrite()) {
        	cmd.add("LOG_WRITE=Y");  //日志实时写入
        }else {
        	cmd.add("LOG_WRITE=N");
		}
        
        settings.addExtraCommandArgs(cmd);
	}
	

    @Override
	public boolean executeProcess(DBRProgressMonitor monitor, DBTTask task, DmImportSettings settings, DmSchema arg,
			Log log) throws IOException, InterruptedException {
		monitor.beginTask(task.getType().getName(), 1);
		logWriter=settings.getLogWriter();	
        String lf = GeneralUtils.getDefaultLineSeparator();
        try {
            monitor.subTask("启动本地还原工具");
            final List<String> commandLine = getCommandLine(settings, arg);
            final File execPath = new File(commandLine.get(0));

            ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
            processBuilder.directory(execPath.getParentFile());
            if (this.isMergeProcessStreams()) {
                processBuilder.redirectErrorStream(true);
            }
            setupProcessParameters(monitor,settings, arg, processBuilder);
            Process process = processBuilder.start();
            
            /**
             * 获取子进程的输入流，这个输入流是相对于父进程而言。即调用CMD进程时，父进程获取到子进程执行的输出数据，相对于父进程而言即为输入流
             * 获取子进程的输出流，这个输出流是相对于父进程而言。父进程要往子进程输出数据时，即需要调用输出流来对子进程输出数据
             */
            
            //startProcessHandler(monitor, task, settings, arg, processBuilder, process, log); // DM 备份时，如果开启新线程记录日志会导致CMD卡死
            // 该方法默认是开启日志记录线程
            monitor.subTask("---正在还原中---");
            
            List<String> command = processBuilder.command();

            // Dump command line
            StringBuilder cmdString = new StringBuilder();
            for (String cmd : command) {
                if (NativeToolUtils.isSecureString(settings, cmd)) {
                    cmd = "******";
                }
                if (cmdString.length() > 0) cmdString.append(' ');
                cmdString.append(cmd);
            }
            cmdString.append(lf);
            
            logWriter.print(cmdString.toString());

            logWriter.print("Task '" + task.getName() + "' started at " + new Date() + lf);
            logWriter.flush();

            InputStream in = process.getInputStream();
            Reader reader = new InputStreamReader(in,"GBK");
            StringBuilder buf = new StringBuilder();
            for (; ; ) {
                if (monitor.isCanceled()) {
                    process.destroy();
                }
                try {
                    int b = reader.read();
                    if (b == -1) {
                        break;
                    }
                    buf.append((char) b);
                    if (b == '\n') {
                        logWriter.print(buf.toString());
                        logWriter.flush();
                        buf.setLength(0);
                    }
                    final int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        throw new IOException("Process failed (exit code = " + exitCode + "). See error log.");
                    }
                } catch (IllegalThreadStateException e) {
                    // Still running
                	// 跳过本次循环 执行下次循环，当一直在运行时那么一直跳过本次循环（循环时获取exitValue()会报错），直到结束时获取到exitValue()不报错了那么就会调用break。
                    continue;
                }
                break; 
            }
        } catch (Exception e) {
            log.error(" error: " + e.getMessage());
            throw e;
        } finally {
            logWriter.print("Task '" + task.getName() + "' finished at " + new Date() +lf);
            logWriter.flush();
            monitor.done();
        }
        return true;		
	}

	private void checkButtons(DmImportSettings settings) {
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
       
       private void checkAddSem(DmImportSettings settings,StringBuilder exclude) {
       Integer nums=settings.getModifyNums();
         if(nums>1) {
       	 exclude.append(","); 
       	 // nums-- 右减减，先进行操作再自减1，--nums 左减减，先自减1再进行操作
       	 settings.setModifyNums(--nums); 
         }
       }

//       @Override
       protected void onSuccess(DBTTask task, DmImportSettings settings, long workTime) {
           StringBuilder message = new StringBuilder();
           message.append("Task [").append(task.getName()).append("] is completed (").append(workTime/1000).append("s)");
           List<String> objNames = new ArrayList<>();
           for (DBSObject obj : settings.getDatabaseObjects()) {
               objNames.add(obj.getName());
           }
           message.append("\nObject(s) processed: ").append(String.join(",", objNames));
           DBWorkbench.getPlatformUI().showMessageBox(task.getName(), message.toString(), false);
       }


       
}
