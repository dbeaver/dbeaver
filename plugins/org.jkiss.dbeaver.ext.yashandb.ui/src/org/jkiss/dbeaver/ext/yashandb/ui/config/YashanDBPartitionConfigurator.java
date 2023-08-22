package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableColumn;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableConstraintColumn;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTablePartition;
import org.jkiss.dbeaver.ext.yashandb.ui.internal.YashanDBUIMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;
import org.jkiss.dbeaver.ui.editors.object.struct.EditPartitionPage;

import java.util.Map;

public class YashanDBPartitionConfigurator implements DBEObjectConfigurator<YashanDBTablePartition> {
    @Override
    public YashanDBTablePartition configureObject(DBRProgressMonitor monitor, Object parent,
                                                  YashanDBTablePartition partition,
                                                  Map<String, Object> options) {
        return UITask.run(() -> {
            EditPartitionPage editPage = new EditPartitionPage(
                    "Create Partition",  //TMP
                    partition);
            if (!editPage.edit()) {
                return null;
            }
            //TODO
            partition.setPartitionNames(editPage.getPartitionNames());
            partition.setPartitionType(editPage.getSelectedPartitionType());
            partition.setName(editPage.getPartitionNames());
            for (DBSEntityAttribute selectedAttribute : editPage.getSelectedAttributes()) {
                partition.addColumn((YashanDBTableColumn) selectedAttribute);
            }
            return partition;
        });
    }
}