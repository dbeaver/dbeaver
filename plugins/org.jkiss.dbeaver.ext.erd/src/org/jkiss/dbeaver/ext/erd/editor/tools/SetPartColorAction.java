package org.jkiss.dbeaver.ext.erd.editor.tools;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.ext.erd.part.IColorizedPart;
import org.jkiss.dbeaver.ext.erd.part.NodePart;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.HashMap;
import java.util.Map;

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
        this.execute(this.createColorCommand(selection.toArray()));
    }

    private Command createColorCommand(final Object[] objects) {
        return new Command() {
            private final Map<IColorizedPart, Color> oldColors = new HashMap<>();
            private Color newColor;
            @Override
            public void execute() {
                final Shell shell = UIUtils.createCenteredShell(getWorkbenchPart().getSite().getShell());
                try {
                    ColorDialog colorDialog = new ColorDialog(shell);
                    RGB color = colorDialog.open();
                    if (color == null) {
                        return;
                    }
                    newColor = new Color(Display.getCurrent(), color);
                    for (Object item : objects) {
                        if (item instanceof IColorizedPart) {
                            IColorizedPart colorizedPart = (IColorizedPart) item;
                            oldColors.put(colorizedPart, colorizedPart.getCustomBackgroundColor());
                            colorizedPart.customizeBackgroundColor(newColor);
                        }
                    }

                } finally {
                    shell.dispose();
                }
            }

            @Override
            public void undo() {
                for (Object item : objects) {
                    if (item instanceof IColorizedPart) {
                        IColorizedPart colorizedPart = (IColorizedPart) item;
                        colorizedPart.customizeBackgroundColor(oldColors.get(colorizedPart));
                    }
                }
            }

            @Override
            public void redo() {
                for (Object item : objects) {
                    if (item instanceof IColorizedPart) {
                        IColorizedPart colorizedPart = (IColorizedPart) item;
                        colorizedPart.customizeBackgroundColor(newColor);
                    }
                }
            }
        };
    }


}
