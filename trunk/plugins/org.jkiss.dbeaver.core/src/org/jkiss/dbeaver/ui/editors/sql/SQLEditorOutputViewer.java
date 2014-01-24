package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ThemeConstants;

/**
 * SQL content type describer
 */
public class SQLEditorOutputViewer extends Composite {

    private final StyledText text;

    public SQLEditorOutputViewer(final IWorkbenchPartSite site, Composite parent, int style) {
        super(parent, style);
        setLayout(new FillLayout());

        text = new StyledText(this, SWT.NONE);

        text.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e)
            {
                UIUtils.enableHostEditorKeyBindings(site, false);
            }
            @Override
            public void focusLost(FocusEvent e)
            {
                UIUtils.enableHostEditorKeyBindings(site, true);
            }
        });

        refreshStyles();
        //text.setFont();
        text.append("Hey there");
    }

    void refreshStyles() {
        ITheme currentTheme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
        Font outputFont = currentTheme.getFontRegistry().get("org.jkiss.dbeaver.sql.output.font");
        if (outputFont != null) {
            this.text.setFont(outputFont);
        }
    }
}
