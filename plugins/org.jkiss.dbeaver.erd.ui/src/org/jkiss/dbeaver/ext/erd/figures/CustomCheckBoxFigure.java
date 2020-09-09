//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Toggle;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

public final class CustomCheckBoxFigure extends Toggle {
    private Label label;
    private static final Image UNCHECKED = DBeaverIcons.getImage(UIIcon.CHECK_OFF);
    private static final Image CHECKED = DBeaverIcons.getImage(UIIcon.CHECK_ON);

    public CustomCheckBoxFigure() {
        this("");
    }

    public CustomCheckBoxFigure(String text) {
        this.label = new Label(text, UNCHECKED);
        this.setContents(this.label);
    }

    protected void handleSelectionChanged() {
        if (this.isSelected()) {
            this.label.setIcon(CHECKED);
        } else {
            this.label.setIcon(UNCHECKED);
        }

    }

    protected void init() {
        super.init();
        this.addChangeListener(changeEvent -> {
            if (changeEvent.getPropertyName().equals("selected")) {
                CustomCheckBoxFigure.this.handleSelectionChanged();
            }

        });
    }
}
