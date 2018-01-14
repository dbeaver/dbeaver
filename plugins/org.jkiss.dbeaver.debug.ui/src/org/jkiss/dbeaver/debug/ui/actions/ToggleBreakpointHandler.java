package org.jkiss.dbeaver.debug.ui.actions;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.debug.ui.actions.RulerToggleBreakpointActionDelegate;
import org.eclipse.swt.widgets.Event;

public class ToggleBreakpointHandler extends AbstractHandler implements IHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
//FIXME:AF: this is a dirty hack to enable ancient 3.x- actions with handlers, needs rework
        RulerToggleBreakpointActionDelegate delegate = new RulerToggleBreakpointActionDelegate();
        delegate.runWithEvent(null, (Event) event.getTrigger());
        return null;
    }

}
