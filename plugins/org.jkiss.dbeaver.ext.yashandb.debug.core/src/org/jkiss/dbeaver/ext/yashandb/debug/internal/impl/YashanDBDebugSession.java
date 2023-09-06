
package org.jkiss.dbeaver.ext.yashandb.debug.internal.impl;

import com.yashandb.jdbc.DebugBreakpointImpl;
import com.yashandb.jdbc.DebugFrame;
import com.yashandb.jdbc.DebugVar;
import com.yashandb.jdbc.YasConnection;
import com.yashandb.jdbc.YasDebugCallableStatement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGBaseController;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.debug.DBGEvent;
import org.jkiss.dbeaver.debug.DBGException;
import org.jkiss.dbeaver.debug.DBGSessionInfo;
import org.jkiss.dbeaver.debug.DBGStackFrame;
import org.jkiss.dbeaver.debug.DBGVariable;
import org.jkiss.dbeaver.debug.core.DebugUtils;
import org.jkiss.dbeaver.debug.jdbc.DBGJDBCSession;
import org.jkiss.dbeaver.ext.yashandb.debug.YashanDBDebugConstants;
import org.jkiss.dbeaver.ext.yashandb.debug.core.YashanDBDebugCore;
import org.jkiss.dbeaver.ext.yashandb.debug.core.YashanDBUtil;
import org.jkiss.dbeaver.ext.yashandb.debug.internal.jdbc.YashanDBGJDBCWorker;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataBase;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataType;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureArgument;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureStandalone;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSourceType;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.IOUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;


public class YashanDBDebugSession extends DBGJDBCSession {
    private final JDBCExecutionContext controllerConnection;

    private volatile YasDebugCallableStatement sessionStatement ;

    //TODO: 临时
    private YasConnection yasConnection ;

    private DBGSessionInfo sessionInfo;

    private YashanDBDebugBreakpointDescriptor bpGlobal;

    private volatile JDBCCallableStatement localStatement;

    private static final int LOCAL_WAIT = 50; // 0.5 sec

    private static final int LOCAL_TIMEOT = 1000 * LOCAL_WAIT; // 50 sec

    private static final Log log = Log.getLog(YashanDBDebugSession.class);

    private Collection<YashanDBProcedureStandalone> procedures;

    public Collection<YashanDBProcedureStandalone> getProcedures() {
        return procedures;
    }

    public void setProcedures(Collection<YashanDBProcedureStandalone> procedures) {
        this.procedures = procedures;
    }

    /**
     * Create session with two description after creation session need to be
     * attached to postgres procedure by attach method
     */
    YashanDBDebugSession(DBRProgressMonitor monitor, DBGBaseController controller)
        throws DBGException {
        super(controller);
        YashanDBDataSource dataSource = (YashanDBDataSource) controller.getDataSourceContainer().getDataSource();
        try {
            YashanDBDataSource instance;
                YashanDBProcedureStandalone function = YashanDBDebugCore.resolveFunction(monitor, controller.getDataSourceContainer(), controller.getDebugConfiguration(), null, null);
                instance = function.getDataSource();

            this.controllerConnection = new YashanDBDataBase(instance).openIsolatedContext(monitor,"YashanDB Debug controller session",null);

            log.debug("Debug controller session created.");
            if (instance != null) {
                log.debug(String.format("Active schema %s", function.getSchema().getName()));
                if (instance.getInfo() instanceof JDBCDataSourceInfo) {
                    JDBCDataSourceInfo JDBCinfo = (JDBCDataSourceInfo) instance.getInfo();
                    log.debug("------------DATABASE DRIVER INFO---------------");
                    log.debug(String.format("Database Product Name %s", JDBCinfo.getDatabaseProductName()));
                    log.debug(String.format("Database Product Version %s", JDBCinfo.getDatabaseProductVersion()));
                    log.debug(String.format("Database Version %s", JDBCinfo.getDatabaseVersion()));
                    log.debug(String.format("Driver Name %s", JDBCinfo.getDriverName()));
                    log.debug(String.format("Driver Version %s", JDBCinfo.getDriverVersion()));
                    log.debug("-----------------------------------------------");
                } else {
                    log.debug("No additional Driver info");
                }
            } else {
                log.debug("Unknown Driver version");
            }

            //init sessionInfo
            //this.sessionInfo  = new YashanDBDebugSessionInfo();

        } catch (DBException e) {
            log.debug(String.format("Error creating debug session %s", e.getMessage()));
            throw new DBGException(e, dataSource);
        }
    }

    @Override
    public JDBCExecutionContext getControllerConnection() {
        return controllerConnection;
    }

    @Override
    public DBGSessionInfo getSessionInfo() {
        return sessionInfo;
    }

    protected String composeAddBreakpointCommand(DBGBreakpointDescriptor descriptor) {
        YashanDBDebugBreakpointDescriptor bp = (YashanDBDebugBreakpointDescriptor) descriptor;
        long line = bp.isOnStart() ? -1 : bp.getLineNo();
        try {
            int subprogramId = getSubprogramId(bp);
            sessionStatement.pdbgAddBreakpoint((long)bp.getObjectId(), subprogramId, bp.getLineNo());
            log.debug(String.format("yashandb's %s Added breakpoint to line #%d",bp.getObjectName(), line));
        } catch (Exception e) {
            closeDebugStatement();
            throw new RuntimeException(e);
        }
        return null;
    }

    protected String composeRemoveBreakpointCommand(DBGBreakpointDescriptor breakpointDescriptor) {
        YashanDBDebugBreakpointDescriptor bp = (YashanDBDebugBreakpointDescriptor) breakpointDescriptor;
        try {
            int subprogramId = getSubprogramId(bp);
            DebugBreakpointImpl debugBreakpoint = new DebugBreakpointImpl(Long.parseLong(bp.getObjectId().toString()),subprogramId, bp.getLineNo());
            sessionStatement.pdbgDeleteBreakpoint(debugBreakpoint);
            log.debug(String.format("yashandb's %s Added breakpoint to line #%d",bp.getObjectName(), bp.getLineNo()));
        } catch (Exception e) {
            closeDebugStatement();
            throw new RuntimeException(e);
        }
        return null;
    }

    private int getSubprogramId(YashanDBDebugBreakpointDescriptor bp) {
        return procedures.stream().filter(t -> t.getObjectId() == Long.parseLong(bp.getObjectId().toString()))
                .collect(Collectors.toList()).get(0).getSubprogramId();
    }

    @Override
    public void execContinue() throws DBGException {
        log.debug("try continue for");
        execStep(DBGEvent.RESUME, new Callable<Object>() {
            @Override
            public Void call() throws Exception {
                try {
                    sessionStatement.pdbgContinue();
                } catch (Throwable e) {
                    if (!sessionStatement.isDebugOn() && e.getMessage().contains("This DebugMode has been closed")){
                        log.debug("This DebugMode has been closed");
                        fireEvent(new DBGEvent(this, DBGEvent.TERMINATE, DBGEvent.CLIENT_REQUEST));
                    }
                    e.getMessage();
                    throw new DBGException(e.getMessage());
                }finally {
                    if (!sessionStatement.isDebugOn()){
                        log.debug("This DebugMode has been closed");
                        fireEvent(new DBGEvent(this, DBGEvent.TERMINATE, DBGEvent.CLIENT_REQUEST));
                    }
                }
				return null;
            }
        }, "continue");
        log.debug("continue for realized");
    }

    @Override
    public void execStepInto() throws DBGException {
        log.debug("try step into");
        execStep(DBGEvent.STEP_INTO, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    sessionStatement.pdbgStepInto();
                } catch (Throwable e) {
                    if (!sessionStatement.isDebugOn() && e.getMessage().contains("This DebugMode has been closed")){
                        log.debug("This DebugMode has been closed");
                        fireEvent(new DBGEvent(this, DBGEvent.TERMINATE, DBGEvent.CLIENT_REQUEST));
                    }
                    e.printStackTrace();
                    throw new DBGException(e.getMessage());
                }finally {
                    if (!sessionStatement.isDebugOn()){
                        log.debug("This DebugMode has been closed");
                        fireEvent(new DBGEvent(this, DBGEvent.TERMINATE, DBGEvent.CLIENT_REQUEST));
                    }
                }
                return null;
            }
        }, "step into");
        log.debug("step into realized");
    }

    @Override
    public void execStepOver() throws DBGException {
        log.debug("try step over");
        execStep(DBGEvent.STEP_OVER, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    sessionStatement.pdbgStepNext();
                } catch (Throwable e) {
                    e.printStackTrace();
                    if (!sessionStatement.isDebugOn() && e.getMessage().contains("This DebugMode has been closed")){
                        log.debug("This DebugMode has been closed");
                        fireEvent(new DBGEvent(this, DBGEvent.TERMINATE, DBGEvent.CLIENT_REQUEST));
                    }
                    throw new RuntimeException(e.getMessage());
                }finally {
                    if (!sessionStatement.isDebugOn()){
                        log.debug("This DebugMode has been closed");
                        fireEvent(new DBGEvent(this, DBGEvent.TERMINATE, DBGEvent.CLIENT_REQUEST));
                    }
                }
                return null;
            }
        }, "step over");
        log.debug("step over realized");
    }

    @Override
    public void execStepReturn() throws DBGException {
        log.debug("try step return");
        execStep(DBGEvent.STEP_RETURN, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    sessionStatement.pdbgStepOut();
                } catch (Throwable e) {
                    if (!sessionStatement.isDebugOn() && e.getMessage().contains("This DebugMode has been closed")){
                        log.debug("This DebugMode has been closed");
                        fireEvent(new DBGEvent(this, DBGEvent.TERMINATE, DBGEvent.CLIENT_REQUEST));
                    }
                    e.printStackTrace();
                    throw new DBGException(e.getMessage());
                }finally {
                    if (!sessionStatement.isDebugOn()){
                        log.debug("This DebugMode has been closed");
                        fireEvent(new DBGEvent(this, DBGEvent.TERMINATE, DBGEvent.CLIENT_REQUEST));
                    }
                }
                return null;
            }
        }, "step return");
        log.debug("step return realized");
    }

    @Override
    public void resume() throws DBGException {
        log.debug("try continue execution");
        execContinue();
        log.debug("continue execution realized");
    }

    @Override
    public void suspend() throws DBGException {
        throw new DBGException("YashanDB Suspend not implemented");
    }

    /**
     * Execute step SQL command asynchronously, set debug session name to
     * [sessionID] name [managerPID]
     *
     */
    public void execStep( int eventDetail, Callable<Object> work, String taskName) throws DBGException {
        DBGEvent begin = new DBGEvent(this, DBGEvent.RESUME, eventDetail);
        DBGEvent end = new DBGEvent(this, DBGEvent.SUSPEND, eventDetail);
        runAsync( taskName, begin, end, work);
    }

    protected void runAsync( String name, DBGEvent begin, DBGEvent end, Callable<Object> work) throws RuntimeException {
        workerJob = new YashanDBGJDBCWorker(this, name, work, begin, end);

        workerJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                workerJob = null;
            }
        });
        workerJob.schedule();
    }

    @Override
    public List<DBGVariable<?>> getVariables(DBGStackFrame stack) throws DBGException {
        //if (stack != null) {
        //    selectFrame(stack.getLevel());
        //}

        log.debug("Get vars values");
        List<DBGVariable<?>> vars = new ArrayList<>();

        //TODO: TMP
        //YashanDBDebugVariable yashanDBDebugVariable
        //        = new YashanDBDebugVariable(
        //               "va",
        //        3,
        //        124556,
        //        "yangmeng-"+TEST_STEP_OVER
        //);
        //YashanDBDebugVariable yashanDBDebugVariable2
        //        = new YashanDBDebugVariable(
        //               "vb",
        //        3,
        //        124556,
        //        null
        //);
        //vars.add(yashanDBDebugVariable);
        //vars.add(yashanDBDebugVariable2);

        YashanDBDebugVariable variable;
        YashanDBDebugObjectDescriptor objectDesc = getObjectDesc();
        try {
            List<DebugVar> debugVars = sessionStatement.pdbgShowFrameVariables();
            log.debug(YashanDBUtil.toJson(debugVars));
            for (DebugVar debugVar : debugVars) {
                Object var = debugVar.getVar();
                String val = var ==null? null: var.toString();
                variable= new YashanDBDebugVariable(debugVar.getName(),
                        stack.getLineNumber(), objectDesc.getOid(), val, debugVar.getDataType());
                vars.add(variable);
            }

            log.debug(YashanDBUtil.toJson(vars));
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().contains("This DebugMode has been closed")){
                //sessionStatement=null;
                log.debug("This DebugMode has been closed");
            }else {
                e.printStackTrace();
                throw new DBGException(e.getMessage());
            }
        }
        log.debug(String.format("Return %d var(s)", vars.size()));
        return vars;

    }

    @Override
    public void setVariableVal(DBGVariable<?> variable, Object value) throws DBGException {
        log.debug("Set var value");
        //TODO: YANGMENG, 动态设置变量暂不实现

    }

    @Override
    public List<DBGStackFrame> getStack() throws DBGException {
        List<DBGStackFrame> stack = new ArrayList<>(1);
        log.debug("YashanDB Get stack");
        YashanDBDebugObjectDescriptor objectDesc = getObjectDesc();
        //YashanDBDebugStackFrame yashanDBDebugStackFrame
        //        = new YashanDBDebugStackFrame(0, objectDesc.getName(), objectDesc.getOid(), TEST_STEP_OVER++, null);
        //stack.add(yashanDBDebugStackFrame);

        //start query stack
        try {
            YashanDBDebugStackFrame stackFrame;
            List<DebugFrame> debugFrames = sessionStatement.pdbgShowFrames();
            for (DebugFrame debugFrame : debugFrames) {
                stackFrame = new YashanDBDebugStackFrame();
                stackFrame.setLevel(debugFrame.getBlockNo());
                stackFrame.setName(debugFrame.getClassInfo());
                if (debugFrame.getClassInfo().equalsIgnoreCase("ANONYMOUS"))
                    continue;
                // 防止不同的schema下游同名的plsql
                stackFrame.setOid(procedures.stream().filter(t -> {
                            String[] split = debugFrame.getClassInfo().split("\\.");
                            return t.getName().equals(split[1]) && t.getParentObject().getName().equals(split[0]);
                        })
                        .collect(Collectors.toList()).get(0).getObjectId());
                stackFrame.setLineNo(debugFrame.getLineNum());
                stack.add(stackFrame);
            }
            log.debug(YashanDBUtil.toJson(stack));
        } catch (Exception e) {
            closeDebugStatement();
            if (e.getMessage().contains("This DebugMode has been closed")){
                //sessionStatement=null;
                log.debug("This DebugMode has been closed");
            }else {
                e.printStackTrace();
                throw new DBGException(e.getMessage());
            }
        }
        log.debug(String.format("Return %d stack frame(s)", stack.size()));
        return stack;
    }

    private void closeDebugStatement() {
        try {
            sessionStatement.pdbgAbort();
        }catch (SQLException ez){
            log.error("sessionStatement.pdbgAbort()  error");
        }
    }

    @Override
    public String getSource(DBGStackFrame stack) throws DBGException {
        log.debug("Get source");
        if (stack instanceof YashanDBDebugStackFrame) {
            YashanDBDebugStackFrame yashandbStack = (YashanDBDebugStackFrame) stack;
            String src = getSource(yashandbStack.getOid());
            log.debug(String.format("Return %d src char(s)", src.length()));
            return src;
        }
        String message = String.format("Unable to get source for stack %s", stack);
        throw new DBGException(message);
    }

    /**
     * Return source for func OID in debug session
     *
     * @return String
     */

    public String getSource(long OID) throws DBGException {
        log.debug("Get source for func OID in debug session");
//        String sql = SQL_GET_SRC.replaceAll("\\?sessionid", String.valueOf(sessionId)).replaceAll("\\?oid",
//            String.valueOf(OID));
//        try (JDBCSession session = getControllerConnection().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Get session source")) {
//            try (Statement stmt = session.createStatement()) {
//                try (ResultSet rs = stmt.executeQuery(sql)) {
//                    if (rs.next()) {
//                        String src = rs.getString(1);
//                        log.debug(String.format("Return %d src char(s)", src.length()));
//                        return src;
//                    }
                    return null;
//                }
//            }
//        } catch (SQLException e) {
//            log.debug(String.format("Unable to get source for OID %s", e.getMessage()));
//            throw new DBGException("SQL error", e);
//        }
    }

    /**
     * This function changes the debugger focus to the indicated frame (in the
     * call stack). Whenever the target stops (at a breakpoint or as the result
     * of a step/into or step/over), the debugger changes focus to most deeply
     * nested function in the call stack (because that's the function that's
     * executing).
     * <p>
     * You can change the debugger focus to other stack frames - once you do
     * that, you can examine the source code for that frame, the variable values
     * in that frame, and the breakpoints in that target.
     * <p>
     * The debugger focus remains on the selected frame until you change it or
     * the target stops at another breakpoint.
     */

    public void selectFrame(int frameNumber) throws DBGException {
        log.debug("Select frame");
//        String sql = SQL_SELECT_FRAME.replaceAll("\\?sessionid", String.valueOf(sessionId)).replaceAll("\\?frameno",
//            String.valueOf(frameNumber));

        String sql="select * from pg_frame";
        try (JDBCSession session = getControllerConnection().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Select debug frame")) {
            try (Statement stmt = session.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (!rs.next()) {
                        log.debug("Unable to select frame");
                        throw new DBGException("Unable to select frame");
                    }

                    log.debug("Frame selected");

                }
            }
        } catch (SQLException e) {
            log.debug(String.format("Unable to select frame %s", e.getMessage()));
            throw new DBGException("SQL error", e);
        }
    }

    @Override
    public YasDebugCallableStatement getSessionId() {
        return sessionStatement;
    }

    @Override
    public boolean canStepInto() {
        return true;
    }

    @Override
    public boolean canStepOver() {
        return true;
    }

    @Override
    public boolean canStepReturn() {
        return true;
    }



    /**
     * Return true if debug session up and running on server
     *
     * @return boolean
     */
    public boolean isAttached() {
        return sessionStatement != null;
    }

    /**
     * Return true if session waiting target connection (on breakpoint, after
     * step or continue) in debug thread
     *
     * @return boolean
     */
    public boolean isDone() {
//        switch (attachKind) {
//            case GLOBAL:
//                return workerJob == null || workerJob.isFinished();
//            case LOCAL:
//                return sessionId > 0;
//            default:
                return true;
//        }
    }

    @Override
    public void closeSession(DBRProgressMonitor monitor) throws DBGException {
        if (!isAttached()) {
            return;
        }
        log.debug("YashanDB Closing session.");
        try {
            super.closeSession(monitor);
            sessionStatement.close();
            yasConnection.close();
            log.debug("YashanDB Session closed.");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (controllerConnection != null) {
                IOUtils.close(controllerConnection);
            }
            if (yasConnection!=null){
                try {
                    yasConnection.close();
                } catch (SQLException e) {
                   yasConnection=null;
                }
            }
        }
    }

    @Override
    protected void doDetach(DBRProgressMonitor monitor) throws DBGException {

    }

    @Override
    protected String composeAbortCommand() {
        return null;
    }

    @Override
    public YashanDBDebugObjectDescriptor getObjectDesc() {
        return (YashanDBDebugObjectDescriptor)super.getObjectDesc();
    }

    @SuppressWarnings("unchecked")
    public void startYashanDBDebug(DBRProgressMonitor monitor, Map<String, Object> configuration) throws SQLException, DBGException {
        // init debug ojbect
        YashanDBDebugObjectDescriptor yashanDBDebugObjectDescriptor =
                new YashanDBDebugObjectDescriptor(
                        Integer.valueOf(configuration.get(YashanDBDebugConstants.attrFunctionOid).toString()),
                        (String) configuration.get(YashanDBDebugConstants.ATTR_FUNCTION_NAME),
                        (String) configuration.get(DBGConstants.ATTR_DATASOURCE_ID),
                        (String) configuration.get(YashanDBDebugConstants.ATTR_SCHEMA_NAME),
                        null
                );

        //init debug other
        JDBCExecutionContext executionContext = null;
        try {
            procedures= ((YashanDBDataSource) getController().getDataSourceContainer().getDataSource()).getSchemas(monitor)
                    .stream().flatMap(
                            t -> {
                                try {
                                    return t.getProcedures(monitor).stream();
                                } catch (DBException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    ).collect(Collectors.toList());

            //procedures = ((YashanDBDataSource) getController().getDataSourceContainer().getDataSource())
            //        .getSchema(monitor, configuration.get(YashanDBDebugConstants.ATTR_SCHEMA_NAME).toString()).getProcedures(monitor);

             executionContext =
                    (JDBCExecutionContext) controllerConnection.getOwnerInstance().openIsolatedContext(monitor, "Debug process session", null);

            try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.USER, "Run SQL command")) {
                try (Statement stmt = session.createStatement()) {
                    String sql = "select SUBPROGRAM_ID FROM dba_procedures  " +
                            "WHERE OWNER ='"+yashanDBDebugObjectDescriptor.getOwner()+"' AND OBJECT_NAME ='"+yashanDBDebugObjectDescriptor.getName()+"'" +
                            " AND OWNER ='"+yashanDBDebugObjectDescriptor.getOwner()+"'";
                    log.debug("YashanDB query SUBPROGRAM_ID sql: \t"+sql);
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        if (!rs.next()) {
                            log.error("YashanDB Error find objectId");
                            throw new DBGException("YashanDB Error find objectId");
                        }
                        int subprogramId = rs.getInt(1);
                        yashanDBDebugObjectDescriptor.setSubprogramId(subprogramId);
                    }

                    String sql2 = "select VERSION from ALL_SOURCE where NAME = '"+yashanDBDebugObjectDescriptor.getName()+"'" +
                            " AND OWNER ='"+yashanDBDebugObjectDescriptor.getOwner()+"'";
                    log.debug("YashanDB query VERSION sql: \t"+sql2);
                    try (ResultSet rs = stmt.executeQuery(sql2)) {
                        if (!rs.next()) {
                            log.error("YashanDB Error find VERSION");
                            throw new DBGException("YashanDB Error find VERSION");
                        }
                        int version = rs.getInt(1);
                        yashanDBDebugObjectDescriptor.setVersion(version);
                    }

                }
            }

            setDbgObjectDescriptor(yashanDBDebugObjectDescriptor);

            //parameter init
            YashanDBProcedureStandalone function = YashanDBDebugCore.resolveFunction(monitor, controllerConnection.getDataSource().getContainer(), configuration, null, null);
            List<String> parameterValues = (List<String>) configuration.get(YashanDBDebugConstants.ATTR_FUNCTION_PARAMETERS);

            //param vaild
            List<YashanDBProcedureArgument> inputParams = function.getInputParams();
            if (inputParams.size() != parameterValues.size()) {
                String unmatched = "Parameter value count (" + parameterValues.size() + ") doesn't match actual function parameters (" + inputParams.size() + ")";
                log.error(unmatched);
                throw new DBGException(unmatched);
            }

            //TODO: 后续处理大小写
            Collection<YashanDBProcedureArgument> parameters = function.getParameters(monitor);
            List<YashanDBProcedureArgument> inOutParams = parameters.stream()
                    .filter(t -> t.getParameterKind().equals(DBSProcedureParameterKind.IN) ||
                    t.getParameterKind().equals(DBSProcedureParameterKind.OUT) ||
                    t.getParameterKind().equals(DBSProcedureParameterKind.INOUT)).collect(Collectors.toList());

            //不管是函数还是存储过程, 都区分有参数和无参数
            StringBuilder startSql = null;
            if (function.getSourceType().equals(YashanDBSourceType.UDF)){
                startSql = getAnonymousSqlBuilder(yashanDBDebugObjectDescriptor, parameterValues, inOutParams, true);
            }else {
                startSql = getAnonymousSqlBuilder(yashanDBDebugObjectDescriptor, parameterValues, inOutParams, false);
            }
            
            log.debug("YashanDB debug start sql: \t"+startSql);

            //Connection connection1 = executionContext.getConnection(monitor);
            //
            //Method createDebugStatement = connection1.getClass()
            //        .getMethod("createDebugStatement", String.class, long.class, int.class, int.class);
            //
            ////ConnectionImpl connection2 =
            ////YasDebugCallableStatement yasDebugCallableStatement = new YasDebugCallableStatement();
            //
            //sessionStatement = (YasDebugCallableStatement) createDebugStatement
            //        .invoke( connection1, startSql.toString(), yashanDBDebugObjectDescriptor.getOid(),
            //                yashanDBDebugObjectDescriptor.getSubprogramId(),
            //                yashanDBDebugObjectDescriptor.getVersion());

            DBPConnectionConfiguration connectionConfiguration = getController().getDataSourceContainer().getConnectionConfiguration();

            //TODO:临时办法, 缺点: 会多用一个connect
            yasConnection=(YasConnection) getYSConnection(connectionConfiguration.getUrl(),
                    connectionConfiguration.getUserName(), connectionConfiguration.getUserPassword());
            sessionStatement = yasConnection.createDebugStatement(startSql.toString(), yashanDBDebugObjectDescriptor.getOid(),
                                    yashanDBDebugObjectDescriptor.getSubprogramId(),
                                    yashanDBDebugObjectDescriptor.getVersion());

            //breakpoint init
            sessionStatement.pdbgStart();

           getController().fireEvent(new DBGEvent(this, DBGEvent.SUSPEND, DBGEvent.MODEL_SPECIFIC));
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionStatement != null && sessionStatement.isDebugOn())
                sessionStatement.pdbgAbort();
            if (yasConnection != null)
                yasConnection.close();
            throw new DBGException(e.getMessage());
        }finally {
            assert executionContext != null;
            executionContext.close();
        }
    }

    /**
     * 获取匿名块调用
     *
     * @param yashanDBDebugObjectDescriptor yashanDBDebugObjectDescriptor
     * @param parameterValues               parameterValues
     * @param inOutParams                   inOutParams
     * @param isFunction                    isFunction
     * @return sql
     * @throws DBGException DBGException
     */
    private static StringBuilder getAnonymousSqlBuilder(YashanDBDebugObjectDescriptor yashanDBDebugObjectDescriptor,
                                                        List<String> parameterValues,
                                                        List<YashanDBProcedureArgument> inOutParams,
                                                        boolean isFunction) throws DBGException {
        StringBuilder startSql;
        //default length, TODO: 后续如果有特殊变化需要修改, SQL DEV默认是200, 但是可以手动修改
        String varcharLength = "10000";
        if (inOutParams.size()>0){
            startSql = new StringBuilder("\ndeclare \n");
            startSql.append("v_result varchar(" + varcharLength + ");\n");
            List<Integer> chars = List.of(Types.CHAR, Types.VARCHAR, Types.NVARCHAR, Types.LONGNVARCHAR, Types.LONGVARCHAR, Types.NCHAR);

            int parameterIndex=0;
            for (YashanDBProcedureArgument inOutParam : inOutParams) {
                //param besic info
                DBSProcedureParameterKind inOutParamParameterKind = inOutParam.getParameterKind();
                int inOutParamTypeID = inOutParam.getTypeID();
                YashanDBDataType inOutParamType = (YashanDBDataType)inOutParam.getType();
                String paramName = inOutParam.getName();
                int typeID = inOutParamType.getTypeID();


                //in out
                long dataLengthFromAllArguments = inOutParam.getMaxLength();
                if (inOutParamParameterKind.equals(DBSProcedureParameterKind.IN) || inOutParamParameterKind.equals(DBSProcedureParameterKind.INOUT)){
                    //TODO: maybe null
                    switch (inOutParamTypeID){
                        case Types.VARCHAR:
                        case Types.CHAR:
                        case Types.LONGVARCHAR:
                        case Types.OTHER:
                        case Types.VARBINARY:
                            //DataType.RAW
                            //RAW,CHAR需要特殊处理, 保持和oracle一致
                            //varchar的参数长度特殊处理一下, 要不然报错YAS-00109 work stack overflow, try to push 32004 bytes
                            startSql.append(paramName).append(" ").append(
                                            typeID == (Types.CHAR) ||
                                                    typeID == Types.VARBINARY ||
                                                    (typeID == Types.OTHER && inOutParamType.getName().equals("JSON") //JSON
                                                    ) ? "VARCHAR" : inOutParamType.toString())
                                    .append("(").append(chars.contains(typeID)? varcharLength: dataLengthFromAllArguments).append(") ").append(":=")
                                    .append('\'').append(parameterValues.get(parameterIndex)).append('\'').append(";\n");
                            break;
                        case Types.DATE:
                        case Types.TIMESTAMP:
                        case Types.TIME:
                        case Types.BLOB:
                        case Types.CLOB:
                        case Types.ROWID:
                            startSql.append(paramName).append(" ").append(inOutParamType.toString()).append(":=")
                                    .append('\'').append(parameterValues.get(parameterIndex)).append('\'').append(";\n");
                            break;
                        case Types.INTEGER:
                        case Types.BIGINT:
                        case Types.TINYINT:
                        case Types.NUMERIC:
                        case Types.SMALLINT:
                        case Types.TIME_WITH_TIMEZONE:
                        case Types.TIMESTAMP_WITH_TIMEZONE:
                        case Types.BIT:
                            startSql.append(paramName).append(" ").append(inOutParamType.toString())
                                    .append("(").append(dataLengthFromAllArguments).append(") ").append(":=")
                                    .append(parameterValues.get(parameterIndex)).append(";\n");
                            break;
                        case Types.BOOLEAN:
                        case Types.FLOAT:
                        case Types.DOUBLE:
                            startSql.append(paramName).append(" ").append(inOutParamType.toString()).append(":=")
                                    .append(parameterValues.get(parameterIndex)).append(";\n");
                            break;
                        default:
                            throw new DBGException("param inOutParamType error");
                    }
                    //TODO: 暂时用下标, 实在不行用map
                    parameterIndex++;
                }else {
                    //out
                    switch (inOutParamTypeID){
                        case Types.VARCHAR:
                        case Types.CHAR:
                        case Types.LONGVARCHAR:
                        case Types.OTHER:
                        case Types.VARBINARY:
                            startSql.append(paramName).append(" ").append(inOutParamType.toString())
                                    .append("(").append(chars.contains(typeID)? varcharLength: dataLengthFromAllArguments).append(") ").append(";\n");
                            break;
                        case Types.INTEGER:
                        case Types.BIGINT:
                        case Types.TINYINT:
                        case Types.NUMERIC:
                        case Types.SMALLINT:
                        case Types.TIME_WITH_TIMEZONE:
                        case Types.TIMESTAMP_WITH_TIMEZONE:
                        case Types.BIT:
                            startSql.append(paramName).append(" ").append(inOutParamType.toString()).append("(").append(dataLengthFromAllArguments).append(");\n");
                            break;
                        case Types.BOOLEAN:
                        case Types.FLOAT:
                        case Types.DOUBLE:
                        case Types.CLOB:
                        case Types.BLOB:
                        case Types.TIMESTAMP:
                        case Types.DATE:
                        case Types.TIME:
                        case Types.ROWID:
                            startSql.append(paramName).append(" ").append(inOutParamType.toString()).append(";\n");
                            break;
                        default:
                            throw new DBGException("param inOutParamType error");
                    }
                }
            }
            startSql.append("begin \n");

            //p_salaries_out :=debug_Stepinto_func3(p_name,p_bonus,p_salaries_out);
            startSql.append(" -- Dynamic PL/SQL block invokes subprogram:\n");
            if (isFunction)
                startSql.append("v_result := ");
            startSql.append(yashanDBDebugObjectDescriptor.getOwner()).append(".")
                    .append(yashanDBDebugObjectDescriptor.getName())
                    .append(" (");

            //加入参数
            for (int i = 0; i < inOutParams.size(); i++) {
                startSql.append(inOutParams.get(i).getName());
                if (i != inOutParams.size() - 1){
                    startSql.append(",");
                }
            }
            startSql.append("); \n end;\n");
        }else {
            startSql = new StringBuilder("\n declare\n");
            startSql.append("v_result varchar("+varcharLength+");\n");
            startSql.append(" begin \n");
            if (isFunction)
                startSql.append("v_result :=");
            startSql.append(yashanDBDebugObjectDescriptor.getOwner()).append(".")
                    .append(yashanDBDebugObjectDescriptor.getName())
                    .append(" (");
            startSql.append("); \n end;\n");
        }
        return startSql;
    }

    /**
     * TMP
     * @return Connection
     */
    public static Connection getYSConnection( String url,  String user,   String password) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }


}
