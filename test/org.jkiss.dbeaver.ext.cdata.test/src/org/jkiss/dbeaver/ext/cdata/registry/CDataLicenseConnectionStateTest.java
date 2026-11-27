/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.cdata.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cdata.model.CDataIcons;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverWithLicense;
import org.jkiss.dbeaver.model.exec.DBCDriverLicenseException;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CDataLicenseConnectionStateTest extends DBeaverUnitTest {
    @Test
    public void migratedConnectionUsesCurrentDriverLicense() {
        var oldDriver = Mockito.mock(DBPDriver.class, Mockito.withSettings().extraInterfaces(DBPDriverWithLicense.class));
        var newDriver = Mockito.mock(DBPDriver.class, Mockito.withSettings().extraInterfaces(DBPDriverWithLicense.class));
        var oldLicense = (DBPDriverWithLicense) oldDriver;
        var newLicense = (DBPDriverWithLicense) newDriver;
        Mockito.when(oldLicense.isLicenseInvalid()).thenReturn(true);
        Mockito.when(newLicense.isLicenseInvalid()).thenReturn(false);
        var parent = Mockito.mock(DBNNode.class);
        var model = Mockito.spy(new DBNModel(DBWorkbench.getPlatform(), null));
        Mockito.doNothing().when(model).fireNodeUpdate(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(parent.getModel()).thenReturn(model);
        var container = Mockito.mock(DBPDataSourceContainer.class,
            Mockito.withSettings().extraInterfaces(DBPImageProvider.class, DBPStatefulObject.class));
        Mockito.when(container.getDriver()).thenReturn(oldDriver);
        Mockito.when(((DBPImageProvider) container).getObjectImage(Mockito.anyBoolean())).thenReturn(DBIcon.DATABASE_DEFAULT);
        Mockito.when(((DBPStatefulObject) container).getObjectState()).thenReturn(DBSObjectState.NORMAL);
        var node = new TestNode(parent, container);
        try {
            Assertions.assertSame(DBIcon.OVER_ERROR, ((DBIconComposite) node.getNodeIcon()).getBottomRight());

            Mockito.when(container.getDriver()).thenReturn(newDriver);

            Assertions.assertSame(DBIcon.DATABASE_DEFAULT, node.getNodeIcon());
            Mockito.verify(oldLicense).removeLicenseChangeListener(Mockito.any());
            Mockito.verify(newLicense).addLicenseChangeListener(Mockito.any());

            Mockito.when(newLicense.isLicenseInvalid()).thenReturn(true);
            Assertions.assertSame(DBIcon.OVER_ERROR, ((DBIconComposite) node.getNodeIcon()).getBottomRight());

            Mockito.when(container.getDriver()).thenReturn(Mockito.mock(DBPDriver.class));
            Assertions.assertSame(DBIcon.DATABASE_DEFAULT, node.getNodeIcon());
            Mockito.verify(newLicense).removeLicenseChangeListener(Mockito.any());
            node.dispose(false);
            Mockito.when(container.getDriver()).thenReturn(oldDriver);
            Assertions.assertSame(DBIcon.DATABASE_DEFAULT, node.getNodeIcon());
            Mockito.verify(oldLicense, Mockito.times(1)).addLicenseChangeListener(Mockito.any());
        } finally {
            node.dispose(false);
        }
    }

    @Test
    public void licenseErrorReferencesDriverAndPreservesCause() {
        var container = Mockito.mock(DBPDataSourceContainer.class);
        var driver = Mockito.mock(DBPDriver.class);
        Mockito.when(container.getName()).thenReturn("My PostgreSQL connection");
        Mockito.when(container.getDriver()).thenReturn(driver);
        Mockito.when(driver.getName()).thenReturn("CData PostgreSQL");
        var cause = new DBException("Trial expired");

        var error = new DBCDriverLicenseException(container, cause);

        Assertions.assertEquals("The driver license for 'CData PostgreSQL' is not active", error.getMessage());
        Assertions.assertSame(container, error.getContainer());
        Assertions.assertSame(cause, error.getCause());
        var status = GeneralUtils.makeExceptionStatus(error);
        Assertions.assertSame(error, status.getException());
        Assertions.assertSame(cause, status.getChildren()[0].getException());
    }

    @Test
    public void sharedLicenseErrorClearsWithoutChangingConnectionState() {
        var registry = DataSourceProviderRegistry.getInstance();
        var driver = Mockito.spy(new CDataDriverDescriptor(registry.getDataSourceProvider("generic"), "test-cdata-shared-license",
            new CDataDriverInfo("postgresql", "postgresql", "PostgreSQL JDBC Driver", 2026,
                CDataDriverTier.PROFESSIONAL, "https://www.cdata.com/", "postgresql", "APRN-VSDBVR")));
        Mockito.doNothing().when(driver).loadIcon(Mockito.any());
        var parent = Mockito.mock(DBNNode.class);
        var model = Mockito.spy(new DBNModel(DBWorkbench.getPlatform(), null));
        Mockito.doNothing().when(model).fireNodeUpdate(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(parent.getModel()).thenReturn(model);
        var nodes = new TestNode[2];
        var states = new DBSObjectState[] {DBSObjectState.NORMAL, DBSObjectState.ACTIVE};
        var driverIcon = driver.getIcon();
        for (int index = 0; index < nodes.length; index++) {
            var container = Mockito.mock(DBPDataSourceContainer.class,
                Mockito.withSettings().extraInterfaces(DBPImageProvider.class, DBPStatefulObject.class));
            Mockito.when(container.getDriver()).thenReturn(driver);
            Mockito.when(container.getConnectionConfiguration()).thenReturn(new DBPConnectionConfiguration());
            Mockito.when(container.getActualConnectionConfiguration()).thenReturn(new DBPConnectionConfiguration());
            Mockito.when(((DBPImageProvider) container).getObjectImage(Mockito.anyBoolean())).thenReturn(driverIcon);
            Mockito.when(((DBPStatefulObject) container).getObjectState()).thenReturn(states[index]);
            nodes[index] = new TestNode(parent, container);
        }
        try {
            driver.setCurrentLicense(new CDataDriverLicense(CDataLicenseStatus.TRIAL_EXPIRED, null, null));
            for (var node : nodes) {
                Assertions.assertSame(DBIcon.OVER_ERROR, ((DBIconComposite) node.getNodeIcon()).getBottomRight());
                Mockito.verify(model).fireNodeUpdate(node, node, DBNEvent.NodeChange.STRUCT_REFRESH);
            }
            Mockito.clearInvocations(model);

            driver.setCurrentLicense(new CDataDriverLicense(CDataLicenseStatus.PURCHASED_ACTIVE, null, null));

            Assertions.assertSame(CDataIcons.CDATA_OVERLAY, ((DBIconComposite) nodes[0].getNodeIcon()).getBottomRight());
            Assertions.assertSame(DBIcon.OVER_SUCCESS, ((DBIconComposite) nodes[1].getNodeIcon()).getBottomRight());
            Assertions.assertSame(CDataIcons.CDATA_OVERLAY, ((DBIconComposite) driver.getIcon()).getBottomRight());
            for (var node : nodes) {
                Mockito.verify(model).fireNodeUpdate(node, node, DBNEvent.NodeChange.STRUCT_REFRESH);
                node.dispose(false);
            }
            Mockito.clearInvocations(model);
            driver.setCurrentLicense(new CDataDriverLicense(CDataLicenseStatus.EXPIRED, null, null));
            Mockito.verifyNoInteractions(model);
        } finally {
            for (var node : nodes) {
                node.dispose(false);
            }
        }
    }

    @Test
    public void activeAndExpiredLicensesCannotStartAnotherTrial() {
        for (var status : new CDataLicenseStatus[] {
            CDataLicenseStatus.TRIAL_ACTIVE, CDataLicenseStatus.TRIAL_EXPIRING, CDataLicenseStatus.TRIAL_EXPIRED,
            CDataLicenseStatus.PURCHASED_ACTIVE, CDataLicenseStatus.PURCHASED_EXPIRING, CDataLicenseStatus.EXPIRED
        }) {
            Assertions.assertFalse(status.allowsTrialActivation(), status.name());
        }
        Assertions.assertTrue(CDataLicenseStatus.NOT_INSTALLED.allowsTrialActivation());
        Assertions.assertFalse(CDataLicenseStatus.VALIDATION_UNAVAILABLE.isValid());
        Assertions.assertTrue(CDataLicenseStatus.VALIDATION_UNAVAILABLE.allowsDriverUsage());
    }

    private static final class TestNode extends DBNDataSource {
        private TestNode(@NotNull DBNNode parent, @NotNull DBPDataSourceContainer container) {
            super(parent, container);
        }

        @Override
        public void dispose(boolean reflect) {
            super.dispose(reflect);
        }
    }
}
