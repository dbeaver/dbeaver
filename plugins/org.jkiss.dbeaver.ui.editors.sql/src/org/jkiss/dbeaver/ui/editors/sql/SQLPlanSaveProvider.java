package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerSerializable;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

public abstract class SQLPlanSaveProvider implements SQLPlanViewProvider {

    public static final String[] EXT = {"*.dbplan", "*"}; //$NON-NLS-1$ //$NON-NLS-2$;
    private static final String[] NAMES = {"DBeaver Plan File", "All files"}; //$NON-NLS-1$ //$NON-NLS-2$;

    private Viewer viewer;
    private SQLQuery query;
    private DBCPlan plan;

    private SaveAction saveAction;

    private DBCQueryPlanner planner;

    protected abstract void showPlan(Viewer viewer, SQLQuery query, DBCPlan plan);
    
    public SQLPlanSaveProvider() {
       saveAction = new SaveAction("Save plan", DBeaverIcons.getImageDescriptor(UIIcon.SAVE_AS), this);
    }

    protected void doSave() {
        if (query != null) {

            if (planner instanceof DBCQueryPlannerSerializable) {

                FileDialog saveDialog = new FileDialog(viewer.getControl().getShell(), SWT.SAVE);
                saveDialog.setFilterExtensions(EXT);
                saveDialog.setFilterNames(NAMES);

                String filePath = DialogUtils.openFileDialog(saveDialog);
                if (filePath == null || filePath.trim().length() == 0) {
                    return;
                }

                try (Writer w = new FileWriter(filePath)) {

                    ((DBCQueryPlannerSerializable) planner).serialize(w, plan);

                } catch (IOException | InvocationTargetException e) {

                    DBWorkbench.getPlatformUI().showError("Load plan", "Error loading plan", e);

                }

            } else {
                saveAction.setEnabled(false);
            }
        }
    }

    protected void fillPlan(SQLQuery query, DBCPlan plan) {
        this.query = query;
        this.plan = plan;
        this.planner = GeneralUtils.adapt(query.getDataSource(),DBCQueryPlanner.class);
    }

    @Override
    public void contributeActions(Viewer viewer, IContributionManager contributionManager, SQLQuery lastQuery, DBCPlan lastPlan) {
        this.viewer = viewer;

        if (saveAction.isEnabled()) {
            contributionManager.add(saveAction);
            contributionManager.add(new Separator());
        }
    }

    class SaveAction extends Action {

        SQLPlanSaveProvider provider;

        public SaveAction(String text, ImageDescriptor image, SQLPlanSaveProvider provider) {
            super(text, image);
            this.provider = provider;
        }

        @Override
        public boolean isEnabled() {
            return planner instanceof DBCQueryPlannerSerializable;
        }

        @Override
        public void run() {
            provider.doSave();
        }
    }
}

