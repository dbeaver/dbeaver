package org.jkiss.dbeaver.ext.erd.editor.tools;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.ext.erd.part.NodePart;

import java.util.List;

public class ChangeZOrderAction extends SelectionAction {

    private IStructuredSelection selection;
    private boolean front;

    public ChangeZOrderAction(ERDEditorPart part, IStructuredSelection selection, boolean front) {
        super(part);
        this.selection = selection;
        this.front = front;

        this.setText(front ? "Bring to front" : "Send to back");
        this.setToolTipText(front ? "Bring to front" : "Send to back");
        this.setId(front ? "bringToFront" : "sendToBack");
    }

    protected boolean calculateEnabled() {
        for (Object item : selection.toArray()) {
            if (item instanceof NodePart) {
                return true;
            }
        }
        return false;
    }

    protected void init() {
        super.init();
    }

    public void run() {
        this.execute(this.createReorderCommand(selection.toArray()));
    }

    private Command createReorderCommand(final Object[] objects) {
        return new Command() {
            @Override
            public void execute() {
                for (Object item : objects) {
                    if (item instanceof NodePart) {
                        IFigure child = ((NodePart) item).getFigure();
                        final IFigure parent = child.getParent();
                        final List children = parent.getChildren();
                        if (children != null) {
                            children.remove(child);
                            if (front) {
                                children.add(child);
                            } else {
                                children.add(0, child);
                            }
                            child.repaint();
                        }
                    }
                }
            }

            @Override
            public boolean canUndo() {
                return false;
            }
        };
    }
}
