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
package org.jkiss.dbeaver.ext.mimer.ui.internal;

import org.eclipse.osgi.util.NLS;

/**
 * NLS message bundle for hardcoded Java-side UI text in the {@code config} create pages and
 * {@code actions} navigator handlers: placeholder text ({@code Text#setMessage}), grid button
 * labels ({@code Button#setText}), tooltips, validation-error text, and dialog/confirm/error
 * message content.
 * <p>
 * This does <b>not</b> cover {@code plugin.xml}'s own {@code label=}/{@code description=}
 * strings, which resolve through the entirely separate {@code %key} + {@code
 * OSGI-INF/l10n/bundle.properties} mechanism instead (see this plugin's own {@code plugin.xml}
 * and {@code MANIFEST.MF}'s {@code Bundle-Localization} header), nor short SWT control labels
 * (e.g. {@code createLabelCombo(composite, "Grantee", ...)}) - those stay inline, matching this
 * plugin's own established scope for this bundle.
 *
 * @author Mimer Information Technology
 */
public final class MimerUIMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages"; //$NON-NLS-1$

    // Shared grid button labels (Parameters/Attributes grids - method specs, procedures, structured types)
    public static String button_add;
    public static String button_remove;

    // MimerCreateUserPage
    public static String page_create_user_password_placeholder;

    // MimerCreateProgramPage
    public static String page_create_program_password_placeholder;

    // MimerCreateCollationPage
    public static String page_create_collation_using_placeholder;

    // MimerCreateMethodSpecPage
    public static String page_create_method_spec_specific_name_placeholder;
    public static String page_create_method_spec_return_size_placeholder;

    // MimerCreateDatabankPage
    public static String page_create_databank_file_size_placeholder;
    public static String page_create_databank_min_size_placeholder;
    public static String page_create_databank_goal_size_placeholder;
    public static String page_create_databank_max_size_placeholder;

    // MimerCreateDistinctTypePage
    public static String page_create_distinct_type_size_placeholder;

    // MimerCreateProcedurePage
    public static String page_create_procedure_specific_name_placeholder;
    public static String page_create_procedure_external_name_placeholder;

    // MimerCreateLibraryPage
    public static String page_create_library_file_placeholder;

    // MimerCreateDatabankFilePage
    public static String page_create_databank_file_file_size_placeholder;
    public static String page_create_databank_file_min_size_placeholder;
    public static String page_create_databank_file_goal_size_placeholder;
    public static String page_create_databank_file_max_size_placeholder;

    // MimerCreateSequencePage
    public static String page_create_sequence_min_placeholder;
    public static String page_create_sequence_max_placeholder;

    // MimerCreateDomainPage
    public static String page_create_domain_size_placeholder;
    public static String page_create_domain_default_placeholder;
    public static String page_create_domain_constraint_name_placeholder;
    public static String page_create_domain_check_placeholder;

    // MimerCreateIndexPage
    public static String page_create_index_name_error;

    // Shared tooltips (identical text reused across several create-dialog pages)
    public static String tooltip_grantee_ident_public_program;
    public static String tooltip_grantee_ident;
    public static String tooltip_data_type;

    // MimerCreateGroupMemberPage
    public static String page_create_group_member_ident_tooltip;
    // MimerCreateSynonymPage
    public static String page_create_synonym_target_name_tooltip;
    // MimerCreateStatementPage
    public static String page_create_statement_cursor_tooltip;
    // MimerCreateGroupMembershipPage
    public static String page_create_group_membership_group_tooltip;
    // MimerCreateCollationPage
    public static String page_create_collation_source_tooltip;

    // actions/MimerShadowAddPagesHandler
    public static String action_shadow_add_pages_title;
    public static String action_shadow_add_pages_message;

    // actions/MimerShadowToMasterHandler
    public static String action_shadow_to_master_title;
    public static String action_shadow_to_master_message;

    // actions/MimerShadowRestoreFromLogHandler
    public static String action_shadow_restore_title;
    public static String action_shadow_restore_message;

    // actions/MimerOnlineActionUtils
    public static String action_shadow_behind_master_title;
    public static String action_shadow_behind_master_message;
    public static String action_job_failed;

    // actions/MimerOpenDependencyTargetHandler
    public static String action_open_target_title;
    public static String action_open_target_message;

    // actions/MimerSetOnlineStateHandler
    public static String action_set_online_state_title;
    public static String action_set_online_state_mixed_types_message;
    public static String action_set_online_state_mixed_status_message;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, MimerUIMessages.class);
    }

    private MimerUIMessages() {
    }
}
