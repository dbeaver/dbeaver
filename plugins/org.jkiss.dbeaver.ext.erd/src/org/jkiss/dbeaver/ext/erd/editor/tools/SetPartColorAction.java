package org.jkiss.dbeaver.ext.erd.editor.tools;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.ext.erd.part.NodePart;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;

public class SetPartColorAction extends SelectionAction {

    private IStructuredSelection selection;

    public SetPartColorAction(ERDEditorPart part, IStructuredSelection selection) {
        super(part);
        this.selection = selection;

        this.setText("Set color");
        this.setToolTipText("Set figure color");
        this.setId("setFigureColor");
    }

    protected boolean calculateEnabled() {
        return true;
    }

    protected void init() {
        super.init();
    }

    public void run() {
        final Shell shell = UIUtils.createCenteredShell(getWorkbenchPart().getSite().getShell());
        try {
            ColorDialog colorDialog = new ColorDialog(shell);
            RGB color = colorDialog.open();
            if (color == null) {
                return;
            }
            Color newColor = new Color(Display.getCurrent(), color);
            for (Object item : selection.toArray()) {
                if (item instanceof NodePart) {
                    IFigure figure = ((NodePart) item).getFigure();
                    figure.setBackgroundColor(newColor);
                }
            }

        } finally {
            shell.dispose();
        }
    }
}
