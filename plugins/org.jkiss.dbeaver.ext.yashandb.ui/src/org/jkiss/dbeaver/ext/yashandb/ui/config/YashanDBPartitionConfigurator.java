package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableColumn;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTablePartition;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditPartitionPage;

import java.util.Map;
import java.util.function.BiFunction;

public class YashanDBPartitionConfigurator implements DBEObjectConfigurator<YashanDBTablePartition> {
    @Override
    public YashanDBTablePartition configureObject(DBRProgressMonitor monitor, Object parent,
                                                  YashanDBTablePartition partition,
                                                  Map<String, Object> options) {
        return UITask.run(() -> {
            EditPartitionPage editPage = new EditPartitionPage(
                    "Create Partition",  //TMP
                    partition, monitor, check, partition.getPartitionType());
            if (!editPage.edit()) {
                return null;
            }
            //TODO
            partition.setPartitionNames(editPage.getPartitionNames());
            partition.setPartitionType(editPage.getSelectedPartitionType());
            partition.setName(editPage.getPartitionNames());
            partition.setValue(editPage.getValue());
            for (DBSEntityAttribute selectedAttribute : editPage.getSelectedAttributes()) {
                partition.addColumn((YashanDBTableColumn) selectedAttribute);
            }
            return partition;
        });
    }

    BiFunction<TableItem, DBNDatabaseNode, Boolean> check = (tableItem, node) -> {
        YashanDBTablePartition partition = (YashanDBTablePartition) node.getObject();
        return partition.getColumns().stream().anyMatch(c -> tableItem.getText().equals(((DBPNamedObject) c).getName()));
    };

}