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
import java.util.Date;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.qm.QMRegistryImpl;
import org.jkiss.dbeaver.tasks.nativetool.NativeToolUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

public class DmSchemaExportHandler extends DmNativeToolHandler<DmExportSettings, DBSObject, DmSchemaExportInfo> {

    private PrintStream logWriter; //日志输出

    @Override
    public Collection<DmSchemaExportInfo> getRunInfo(DmExportSettings settings) {
        return settings.getExportObjects();
    }


    @Override
    protected DmExportSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        DmExportSettings settings = new DmExportSettings();
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }


    @Override
    protected boolean validateTaskParameters(DBTTask task, DmExportSettings settings, Log log) {
        if (task.getType().getId().equals(DmTasks.TASK_DATABASE_BACKUP)) {
//            final File dir = settings.getOutputFolder();
            final File dir = new File(settings.getOutputFolderPattern());
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.error("Can't create directory '" + dir.getAbsolutePath() + "'");
                    return false;
                }
            }
        }
        return true;
    }


    // 获取命令行，放入需要导出的模式名
    //此处设置导出的部分表以及所导出的数据库
    //DM 暂时不考虑相关内容
    @Override
    protected List<String> getCommandLine(DmExportSettings settings, DmSchemaExportInfo arg) throws IOException {
        List<String> cmd = getDmToolCommandLine(this, settings, arg);

        cmd.add("SCHEMAS=(" + arg.getDatabase().getName() + ")");

        return cmd;
    }


    //填充参数
    @Override
    public void fillProcessParameters(DmExportSettings settings, DmSchemaExportInfo arg, List<String> cmd)
            throws IOException {

        File dumpBinary = RuntimeUtils.getNativeClientBinary(settings.getClientHome(), "bin", "dexp"); //$NON-NLS-1$
        String dumpPath = dumpBinary.getAbsolutePath();
        cmd.add(dumpPath);

        /*
         * 添加导出的用户名等
         */
        DBPConnectionConfiguration connectionInfo = settings.getDataSourceContainer().getActualConnectionConfiguration();
        StringBuilder stringBuilder = new StringBuilder();
        String toolUserName = settings.getToolUserName();
        if (CommonUtils.isEmpty(toolUserName)) {
            toolUserName = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserName();
        }
        if (!CommonUtils.isEmpty(toolUserName)) {
            stringBuilder.append(quoteString + toolUserName + quoteString);
        }
        String password = connectionInfo.getUserPassword(); //密码
        if (!CommonUtils.isEmpty(password)) {
            stringBuilder.append("/");
            stringBuilder.append(quoteString + password + quoteString);
        }
        stringBuilder.append("@" + connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            stringBuilder.append(":" + connectionInfo.getHostPort());
        }
        cmd.add(stringBuilder.toString());

        String outFileName = GeneralUtils.replaceVariables(settings.getOutputFile(), name -> { //导出的文件
            switch (name) {
                case NativeToolUtils.VARIABLE_DATABASE:
                    return arg.getDatabase().getName();
                case NativeToolUtils.VARIABLE_HOST:
                    return arg.getDatabase().getDataSource().getContainer().getConnectionConfiguration().getHostName();
                case NativeToolUtils.VARIABLE_TIMESTAMP:
                    return RuntimeUtils.getCurrentTimeStamp();
                case NativeToolUtils.VARIABLE_DATE:
                    return RuntimeUtils.getCurrentDate();
                default:
                    System.getProperty(name);
            }
            return null;
        });

        String outLogName = GeneralUtils.replaceVariables(settings.getOutputLogFilePattern(), name -> {
            switch (name) {
                case NativeToolUtils.VARIABLE_DATABASE:
                    return arg.getDatabase().getName();
                case NativeToolUtils.VARIABLE_HOST:
                    return arg.getDatabase().getDataSource().getContainer().getConnectionConfiguration().getHostName();
                case NativeToolUtils.VARIABLE_TIMESTAMP:
                    return RuntimeUtils.getCurrentTimeStamp();
                case NativeToolUtils.VARIABLE_DATE:
                    return RuntimeUtils.getCurrentDate();
                default:
                    System.getProperty(name);
            }
            return null;
        });
//        String outputPath = settings.getOutputFolder().getAbsolutePath();
        String outputPath = settings.getOutputFolderPattern();
        cmd.add("DIRECTORY=" + outputPath);
        cmd.add("FILE=" + outFileName);
        if (settings.isIncludeTableSpace()) { //定义包含表空间
            cmd.add("TABLESPACE=Y"); //$NON-NLS-1$
        } else {
            cmd.add("TABLESPACE=N");
        }

        cmd.add("LOG=" + outLogName);
        if (settings.isLogWrite()) {
            cmd.add("LOG_WRITE=Y");  //日志实时写入
        } else {
            cmd.add("LOG_WRITE=N");
        }

        if (settings.isDrop()) { //导出后删除
            cmd.add("DROP=Y");
        } else {
            cmd.add("DROP=N");
        }

        if (settings.isCompress()) { //压缩
            cmd.add("COMPRESS=Y"); //$NON-NLS-1$
        }

        checkButtons(settings);
        if (settings.isModifyBase()) {
            StringBuilder exclude = new StringBuilder();
            exclude.append("EXCLUDE=(");
            if (!settings.isData()) {
                exclude.append("ROWS");
                checkAddSem(settings, exclude); //检测是否需要添加分号
            }
            if (!settings.isIndex()) {
                exclude.append("INDEXES");
                checkAddSem(settings, exclude); //检测是否需要添加分号
            }
            if (!settings.isConstraint()) {
                exclude.append("CONSTRAINTS");
                checkAddSem(settings, exclude); //检测是否需要添加分号
            }
            if (!settings.isTrigger()) {
                exclude.append("TRIGGERS");
                checkAddSem(settings, exclude); //检测是否需要添加分号
            }
            if (!settings.isGrants()) {
                exclude.append("GRANTS");
                checkAddSem(settings, exclude); //检测是否需要添加分号
            }
            exclude.append(")");
            cmd.add(exclude.toString());
        }

        settings.addExtraCommandArgs(cmd);
    }

    @Override
    protected boolean needsModelRefresh() {
        return false;
    }

    @Override
    protected boolean isLogInputStream() { //DM的log由进程的process的getInputStream() 获得 而不是ErrorStream输出打印日志
        return false;
    }


    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, DmExportSettings settings,
                                       DmSchemaExportInfo arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException {

        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);

    }


    /**
     * DM 执行Process 直接输出日志
     */
    @Override
    public boolean executeProcess(DBRProgressMonitor monitor, DBTTask task, DmExportSettings settings,
                                  DmSchemaExportInfo arg, Log log) throws IOException, InterruptedException {
        monitor.beginTask(task.getType().getName(), 1);
        logWriter = settings.getLogWriter();
        String lf = GeneralUtils.getDefaultLineSeparator();//\r\n
        try {
            monitor.subTask("启动本地备份工具");
            final List<String> commandLine = getCommandLine(settings, arg); //获取当前备份的参数命令行
            final File execPath = new File(commandLine.get(0)); //获取备份工具的位置

            ProcessBuilder processBuilder = new ProcessBuilder(commandLine);//准备创建process进程 
            processBuilder.directory(execPath.getParentFile());
            if (this.isMergeProcessStreams()) {
                processBuilder.redirectErrorStream(true);
            }
            setupProcessParameters(monitor, settings, arg, processBuilder);
            Process process = processBuilder.start(); //执行进程，导出备份文件

            /**
             * 获取子进程的输入流，这个输入流是相对于父进程而言。即调用CMD进程时，父进程获取到子进程执行的输出数据，相对于父进程而言即为输入流
             * 获取子进程的输出流，这个输出流是相对于父进程而言。父进程要往子进程输出数据时，即需要调用输出流来对子进程输出数据
             */

            //startProcessHandler(monitor, task, settings, arg, processBuilder, process, log); // DM 备份时，如果开启新线程记录日志会导致CMD卡死
            // 该方法默认是开启日志记录线程
            monitor.subTask("---正在备份中---");

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
            Reader reader = new InputStreamReader(in, "GBK");
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
            //throw e;
        } finally {
            logWriter.print("Task '" + task.getName() + "' finished at " + new Date() + lf);
            logWriter.flush();
            monitor.done();
        }
        return true;
    }


//    @Override
    protected void onSuccess(DBTTask task, DmExportSettings settings, long workTime) {
        StringBuilder message = new StringBuilder();
        message.append("Task [").append(task.getName()).append("] is completed (").append(workTime / 1000).append("s)");
        List<String> objNames = new ArrayList<>();
        for (DBSObject obj : settings.getDatabaseObjects()) {
            objNames.add(obj.getName());
        }
        message.append("\nObject(s) processed: ").append(String.join(",", objNames));
        DBWorkbench.getPlatformUI().showMessageBox(task.getName(), message.toString(), false);
    }


}
