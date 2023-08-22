//package org.jkiss.dbeaver.ext.dm.ui.config;
//
//import java.util.Map;
//
//import org.jkiss.dbeaver.ext.dm.model.DmTableColumn;
//import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
//import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
//import org.jkiss.dbeaver.ui.UITask;
//import org.jkiss.dbeaver.ui.editors.object.struct.AttributeEditPage;
//
//public class DmTableColumnConfigurator implements DBEObjectConfigurator<DmTableColumn>{
//
//    @Override
//    public DmTableColumn configureObject(DBRProgressMonitor monitor, Object table, DmTableColumn object,Map<String, Object> options) {
//        return new UITask<DmTableColumn>() {
//            @Override
//            protected DmTableColumn runTask() {
//                AttributeEditPage page = new AttributeEditPage(null, object);
//                if (!page.edit()) {
//                    return null;
//                }
//                return object;
//            }
//        }.execute();
//	}
//}
