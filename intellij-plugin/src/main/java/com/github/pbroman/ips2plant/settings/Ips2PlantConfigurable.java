package com.github.pbroman.ips2plant.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class Ips2PlantConfigurable implements Configurable {

    private JTextField localeField;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "IPS to PlantUML";
    }

    @Override
    public @Nullable JComponent createComponent() {
        localeField = new JTextField(10);

        var panel = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(4, 0, 4, 8);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Locale for descriptions and enum content (default: de):"), gbc);

        gbc.gridx = 1;
        panel.add(localeField, gbc);

        // push everything to top-left
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return !localeField.getText().trim().equals(Ips2PlantSettings.getInstance().locale);
    }

    @Override
    public void apply() {
        var locale = localeField.getText().trim();
        Ips2PlantSettings.getInstance().locale = locale.isEmpty() ? "de" : locale;
    }

    @Override
    public void reset() {
        localeField.setText(Ips2PlantSettings.getInstance().locale);
    }
}
