package org.jkiss.dbeaver.ext.yashandb.debug.internal.jdbc;

import com.yashandb.jdbc.YasDebugCallableStatement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.debug.DBGEvent;
import org.jkiss.dbeaver.debug.jdbc.DBGJDBCSession;
import org.jkiss.dbeaver.debug.jdbc.DBGJDBCWorker;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.concurrent.Callable;

/**
 * @Author yangmeng on 2023/3/20 16:57
 */
public class YashanDBGJDBCWorker extends DBGJDBCWorker {

    private final DBGJDBCSession debugSession;
    private final Callable<Object> work;
    private final DBGEvent before;
    private final DBGEvent after;
    private final String name;

    public YashanDBGJDBCWorker(DBGJDBCSession debugSession, String name,  Callable<Object> work, DBGEvent begin, DBGEvent end) {
        super(debugSession, name, null, begin, end);
        this.debugSession = debugSession;
        this.work = work;
        this.before = begin;
        this.after = end;
        this.name=name;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        YasDebugCallableStatement sessionId = (YasDebugCallableStatement) debugSession.getSessionId();
        if (sessionId==null){
            return Status.OK_STATUS;
        }
        monitor.beginTask("Execute YashanDB "+name+" debug job", 1);
        try  {
            monitor.subTask("YashanDB pdbgStepNext");
                debugSession.fireEvent(before);
                work.call();
                debugSession.fireEvent(after);
                return Status.OK_STATUS;
        } catch (Throwable e) {
            return GeneralUtils.makeExceptionStatus(String.format("Failed to execute %s", name), e);
        } finally {
            monitor.done();
        }
    }


}
