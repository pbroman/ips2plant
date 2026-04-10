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
    private JCheckBox generateFallbackToAllCheck;
    private JCheckBox generateResetsOptionsCheck;
    private JCheckBox searchFallbackToAllCheck;
    private JCheckBox searchSelectsAllClassTypesCheck;
    private JCheckBox searchResetsOptionsCheck;
    private JCheckBox retriggerOnDirChangeCheck;
    private JCheckBox selectAllIgnoresDescriptionsCheck;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "IPS to PlantUML";
    }

    @Override
    public @Nullable JComponent createComponent() {
        localeField = new JTextField(10);
        generateFallbackToAllCheck = new JCheckBox(
                "Model generation falls back on all directories when none selected");
        generateResetsOptionsCheck = new JCheckBox(
                "Model generation resets the options to default (showing only Policy Components)");
        searchFallbackToAllCheck = new JCheckBox(
                "Search falls back on all directories when none selected");
        searchSelectsAllClassTypesCheck = new JCheckBox(
                "Search selects all class types found");
        searchResetsOptionsCheck = new JCheckBox(
                "Search resets all options (other than class types found, when selected)");
        retriggerOnDirChangeCheck = new JCheckBox(
                "Selecting / deselecting model directories retriggers model generation / search");
        selectAllIgnoresDescriptionsCheck = new JCheckBox(
                "Options Select All ignores Descriptions (deselect still clears Descriptions)");

        var panel = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(4, 0, 4, 8);
        gbc.gridwidth = 1;

        int row = 0;

        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Locale for descriptions and enum content (default: de):"), gbc);
        gbc.gridx = 1;
        panel.add(localeField, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.insets = JBUI.insets(12, 0, 2, 8);
        panel.add(new JLabel("<html><b>Tool Window Behavior</b></html>"), gbc);

        gbc.insets = JBUI.insets(2, 0, 2, 8);

        gbc.gridy = row++;
        panel.add(generateFallbackToAllCheck, gbc);

        gbc.gridy = row++;
        panel.add(generateResetsOptionsCheck, gbc);

        gbc.gridy = row++;
        panel.add(searchFallbackToAllCheck, gbc);

        gbc.gridy = row++;
        panel.add(searchSelectsAllClassTypesCheck, gbc);

        gbc.gridy = row++;
        panel.add(searchResetsOptionsCheck, gbc);

        gbc.gridy = row++;
        panel.add(retriggerOnDirChangeCheck, gbc);

        gbc.gridy = row++;
        panel.add(selectAllIgnoresDescriptionsCheck, gbc);

        // push everything to top-left
        gbc.gridy = row;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        var s = Ips2PlantSettings.getInstance();
        return !localeField.getText().trim().equals(s.locale)
                || generateFallbackToAllCheck.isSelected() != s.generateFallbackToAll
                || generateResetsOptionsCheck.isSelected() != s.generateResetsOptions
                || searchFallbackToAllCheck.isSelected() != s.searchFallbackToAll
                || searchSelectsAllClassTypesCheck.isSelected() != s.searchSelectsAllClassTypes
                || searchResetsOptionsCheck.isSelected() != s.searchResetsOptions
                || retriggerOnDirChangeCheck.isSelected() != s.retriggerOnDirChange
                || selectAllIgnoresDescriptionsCheck.isSelected() != s.selectAllIgnoresDescriptions;
    }

    @Override
    public void apply() {
        var s = Ips2PlantSettings.getInstance();
        var locale = localeField.getText().trim();
        s.locale = locale.isEmpty() ? "de" : locale;
        s.generateFallbackToAll = generateFallbackToAllCheck.isSelected();
        s.generateResetsOptions = generateResetsOptionsCheck.isSelected();
        s.searchFallbackToAll = searchFallbackToAllCheck.isSelected();
        s.searchSelectsAllClassTypes = searchSelectsAllClassTypesCheck.isSelected();
        s.searchResetsOptions = searchResetsOptionsCheck.isSelected();
        s.retriggerOnDirChange = retriggerOnDirChangeCheck.isSelected();
        s.selectAllIgnoresDescriptions = selectAllIgnoresDescriptionsCheck.isSelected();
    }

    @Override
    public void reset() {
        var s = Ips2PlantSettings.getInstance();
        localeField.setText(s.locale);
        generateFallbackToAllCheck.setSelected(s.generateFallbackToAll);
        generateResetsOptionsCheck.setSelected(s.generateResetsOptions);
        searchFallbackToAllCheck.setSelected(s.searchFallbackToAll);
        searchSelectsAllClassTypesCheck.setSelected(s.searchSelectsAllClassTypes);
        searchResetsOptionsCheck.setSelected(s.searchResetsOptions);
        retriggerOnDirChangeCheck.setSelected(s.retriggerOnDirChange);
        selectAllIgnoresDescriptionsCheck.setSelected(s.selectAllIgnoresDescriptions);
    }
}
