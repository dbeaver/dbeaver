/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * LocaleSelectorControl
 */
public class LocaleSelectorControl extends Composite
{
    private Combo languageCombo;
    private Combo countryCombo;
    private Combo variantCombo;
    private Text localeText;
    private Locale currentLocale;

    private boolean localeChanging = false;

    public LocaleSelectorControl(
        Composite parent,
        Locale defaultLocale)
    {
        super(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        this.setLayout(gl);

        Group group = new Group(this, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        gl = new GridLayout(2, false);
        group.setLayout(gl);
        group.setText(UIMessages.controls_locale_selector_group_locale);

        UIUtils.createControlLabel(group, UIMessages.controls_locale_selector_label_language);
        languageCombo = new Combo(group, SWT.DROP_DOWN);
        languageCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        languageCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                onLanguageChange(null);
                calculateLocale();
            }
        });

        UIUtils.createControlLabel(group, UIMessages.controls_locale_selector_label_country);
        countryCombo = new Combo(group, SWT.DROP_DOWN);
        countryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        countryCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                onCountryChange(null);
                calculateLocale();
            }
        });

        UIUtils.createControlLabel(group, UIMessages.controls_locale_selector_label_variant);
        variantCombo = new Combo(group, SWT.DROP_DOWN);
        variantCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        variantCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                calculateLocale();
            }
        });

        UIUtils.createControlLabel(group, UIMessages.controls_locale_selector_label_locale);
        localeText = new Text(group, SWT.BORDER | SWT.READ_ONLY);
        localeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Locale[] locales = Locale.getAvailableLocales();

        Set<String> languages = new TreeSet<>();
        for (Locale locale : locales) {
            languages.add(locale.getLanguage() + " - " + locale.getDisplayLanguage()); //$NON-NLS-1$
        }

        currentLocale = defaultLocale;
        if (currentLocale == null) {
            currentLocale = Locale.getDefault();
        }
        for (String language : languages) {
            languageCombo.add(language);
            if (getIsoCode(language).equals(currentLocale.getLanguage())) {
                languageCombo.select(languageCombo.getItemCount() - 1);
            }
        }

        onLanguageChange(currentLocale.getCountry());
        onCountryChange(currentLocale.getVariant());
    }

    private static String getIsoCode(String value)
    {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isLetter(value.charAt(i))) {
                return value.substring(0, i);
            }
        }
        return value;
    }

    private void onLocaleChange()
    {
        try {
            localeChanging = true;

            int count = languageCombo.getItemCount();
            boolean found = false;
            for (int i = 0; i < count; i++) {
                if (getIsoCode(languageCombo.getItem(i)).equals(currentLocale.getLanguage())) {
                    languageCombo.select(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                languageCombo.setText(currentLocale.getLanguage());
            }
            onLanguageChange(currentLocale.getLanguage());

            count = countryCombo.getItemCount();
            found = false;
            for (int i = 0; i < count; i++) {
                if (getIsoCode(countryCombo.getItem(i)).equals(currentLocale.getCountry())) {
                    countryCombo.select(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                countryCombo.setText(currentLocale.getCountry());
            }
            onCountryChange(currentLocale.getCountry());
            variantCombo.setText(currentLocale.getVariant());
        }
        finally {
            localeChanging = false;
        }
        calculateLocale();
    }

    private void onLanguageChange(String defCountry)
    {
        String language = getIsoCode(languageCombo.getText());
        Locale[] locales = Locale.getAvailableLocales();
        countryCombo.removeAll();
        Set<String> countries = new TreeSet<>();
        for (Locale locale : locales) {
            if (language.equals(locale.getLanguage()) && !CommonUtils.isEmpty(locale.getCountry())) {
                countries.add(locale.getCountry() + " - " + locale.getDisplayCountry()); //$NON-NLS-1$
            }
        }
        if (defCountry == null) {
            defCountry = currentLocale.getCountry();
        }
        for (String country : countries) {
            countryCombo.add(country);
            if (defCountry != null && defCountry.equals(getIsoCode(country))) {
                countryCombo.select(countryCombo.getItemCount() - 1);
            }
        }
        if (defCountry == null && countryCombo.getItemCount() > 0) {
            countryCombo.select(0);
        }
    }

    private void onCountryChange(String defVariant)
    {
        String language = getIsoCode(languageCombo.getText());
        String country = getIsoCode(countryCombo.getText());
        Locale[] locales = Locale.getAvailableLocales();
        variantCombo.removeAll();
        Set<String> variants = new TreeSet<>();
        for (Locale locale : locales) {
            if (language.equals(locale.getLanguage()) && country.equals(locale.getCountry())) {
                if (!CommonUtils.isEmpty(locale.getVariant())) {
                    if (locale.getVariant().equals(locale.getDisplayVariant())) {
                        variants.add(locale.getVariant());
                    } else {
                        variants.add(locale.getVariant() + " - " + locale.getDisplayVariant()); //$NON-NLS-1$
                    }
                }
            }
        }
        if (defVariant == null) {
            defVariant = currentLocale.getVariant();
        }
        for (String variant : variants) {
            variantCombo.add(variant);
            if (defVariant != null && defVariant.equals(getIsoCode(variant))) {
                variantCombo.select(variantCombo.getItemCount() - 1);
            }
        }
        if (defVariant == null && variantCombo.getItemCount() > 0) {
            variantCombo.select(0);
        }
    }

    private void calculateLocale()
    {
        if (localeChanging) {
            return;
        }
        String language = getIsoCode(languageCombo.getText());
        String country = getIsoCode(countryCombo.getText());
        String variant = getIsoCode(variantCombo.getText());
        currentLocale = new Locale(language, country, variant);
        localeText.setText(currentLocale.toString());

        Event event = new Event();
        event.data = currentLocale;

        super.notifyListeners(SWT.Selection, event);
    }
    
    public void setLocale(Locale locale)
    {
        currentLocale = locale;
        onLocaleChange();
    }

    public Locale getSelectedLocale()
    {
        return currentLocale;
    }

}