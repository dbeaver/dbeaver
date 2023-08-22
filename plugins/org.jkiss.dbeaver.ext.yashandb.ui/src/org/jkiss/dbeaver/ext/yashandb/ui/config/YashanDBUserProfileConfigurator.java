package org.jkiss.dbeaver.ext.yashandb.ui.config;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBUserProfile;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.util.Map;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/19 14:55
 */
public class YashanDBUserProfileConfigurator implements DBEObjectConfigurator<YashanDBUserProfile> {
    @Override
    public YashanDBUserProfile configureObject(DBRProgressMonitor monitor, Object container, YashanDBUserProfile profile, Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage page = new EntityEditPage(profile.getDataSource(), DBSEntityType.PROFILE);
            if (!page.edit()) {
                return null;
            }
            profile.setName(page.getEntityName());
//            profile.setFailLoginAttempts("UNLIMITED");
//            profile.setPasswordGraceTime("UNLIMITED");
//            profile.setPasswordLifeTime("UNLIMITED");
//            profile.setPasswordLockTime("UNLIMITED");
//            profile.setPasswordReuseMax("UNLIMITED");
//            profile.setPasswordReuseTime("UNLIMITED");
            return profile;
        });
    }
}
