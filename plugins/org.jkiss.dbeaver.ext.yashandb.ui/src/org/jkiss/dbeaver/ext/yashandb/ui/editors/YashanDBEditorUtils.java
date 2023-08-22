package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBConstants;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDDLFormat;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBEditorUtils {
    public static void addDDLControl(IContributionManager contributionManager, YashanDBTableBase sourceObject, SQLSourceViewer source) {
        contributionManager.add(new Separator());
        contributionManager.add(new ControlContribution("DDLFormat") {
            @Override
            protected Control createControl(Composite parent) {
                YashanDBDDLFormat ddlFormat = YashanDBDDLFormat.getCurrentFormat(sourceObject.getDataSource());
                final Combo ddlFormatCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN);
                ddlFormatCombo.setToolTipText("DDL Format");
                for (YashanDBDDLFormat format : YashanDBDDLFormat.values()) {
                    ddlFormatCombo.add(format.getTitle());
                    if (format == ddlFormat) {
                        ddlFormatCombo.select(ddlFormatCombo.getItemCount() - 1);
                    }
                }
                ddlFormatCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        for (YashanDBDDLFormat format : YashanDBDDLFormat.values()) {
                            if (format.ordinal() == ddlFormatCombo.getSelectionIndex()) {
                                if (source instanceof YashanDBDDLOptions) {
                                    ((YashanDBDDLOptions) source).putDDLOptions(YashanDBConstants.PREF_KEY_DDL_FORMAT, format);
                                }
                                sourceObject.getDataSource().getContainer().getPreferenceStore().setValue(
                                        YashanDBConstants.PREF_KEY_DDL_FORMAT, format.name());
                                source.refreshPart(this, true);
                                break;
                            }
                        }
                    }
                });
                return ddlFormatCombo;
            }
        });
    }
}
