package com.github.pbroman.ips2plant.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import com.github.pbroman.ips2plant.core.Ips2PlantOptions;
import com.github.pbroman.ips2plant.settings.Ips2PlantSettings;
import com.github.pbroman.ips2plant.ui.manager.DependencyManager;
import com.github.pbroman.ips2plant.ui.manager.GenerationManager;
import com.github.pbroman.ips2plant.ui.manager.GenerationStateManager;
import com.github.pbroman.ips2plant.ui.manager.ModelDirTreeManager;
import com.github.pbroman.ips2plant.ui.manager.SearchManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;

public class Ips2PlantToolWindowPanel extends JPanel {

    private static final String TITLE_MODEL_DIRECTORIES = "Model Directories";
    private static final String TITLE_OPTIONS = "Options";
    private static final String TITLE_SEARCH = "Search";
    private static final String LABEL_PACKAGES = "Packages";
    private static final String LABEL_PRINT_TARGET_ROLE = "Target Roles";
    private static final String LABEL_EXTERNAL_SUPERTYPES = "External Supertypes";
    private static final String LABEL_EXTERNAL_ASSOCIATIONS = "External Associations";
    private static final String LABEL_SHOW_TABLE_STRUCTURES = "Table Structures";
    private static final String LABEL_SHOW_TABLE_USAGE = "Table Usage";
    private static final String LABEL_SHOW_ENUM_TYPES = "Enum Types";
    private static final String LABEL_SHOW_ENUM_CONTENT = "Enum Content";
    private static final String LABEL_SHOW_ENUM_ASSOCIATIONS = "Enum Associations";
    private static final String LABEL_SHOW_MAVEN_MODULE = "Maven Modules";
    private static final String LABEL_SHOW_DESCRIPTIONS = "Descriptions";
    private static final String LABEL_SHOW_CARDINALITIES = "Cardinalities";
    private static final String LABEL_SHOW_POLICY_COMPONENTS = "Policy Components";
    private static final String LABEL_SHOW_PRODUCT_COMPONENTS = "Product Components";
    private static final String LABEL_CLEAR_DEPENDENCIES = "Clear Dependencies";
    private static final String LABEL_RESOLVE_DEPENDENCIES = "Resolve Dependencies";
    private static final String LABEL_PACKAGE_FILTER = "Package Filter:";
    private static final String LABEL_CONNECTOR_LENGTH = "Connector Length:";
    private static final String LABEL_GENERATE = "Generate Model UML";
    private static final String LABEL_SEARCH = "Search";
    private static final String LABEL_ADD_SUPERTYPES = "Add Supertypes";
    private static final String LABEL_ADD_REFERENCING = "Add Referencing Classes";
    private static final String LABEL_SELECT_ALL_OPTIONS = "Select All";
    private static final String LABEL_RESET_ALL_OPTIONS = "Reset to Default";
    private static final String LABEL_RESET_ALL = "Reset All";

    private static final String TOOLTIP_PACKAGES = "Displays classes in packages";
    private static final String TOOLTIP_PRINT_TARGET_ROLE = "Print the targetRolePlural attribute on the composition arrow";
    private static final String TOOLTIP_EXTERNAL_SUPERTYPES = "Adds inheritance of supertypes not in the selected packages";
    private static final String TOOLTIP_EXTERNAL_ASSOCIATIONS = "Adds associations to classes not in the selected packages";
    private static final String TOOLTIP_SHOW_TABLE_STRUCTURES = "Show table structures";
    private static final String TOOLTIP_SHOW_TABLE_USAGE = "Show table usage by product component types (including external tables)";
    private static final String TOOLTIP_SHOW_ENUM_TYPES = "Show enum types";
    private static final String TOOLTIP_SHOW_ENUM_CONTENT = "Show enum content (values of extensible enum types)";
    private static final String TOOLTIP_SHOW_ENUM_ASSOCIATIONS = "Show enum associations (including external enums)";
    private static final String TOOLTIP_SHOW_MAVEN_MODULE = "Show the modules where the classes are defined";
    private static final String TOOLTIP_SHOW_DESCRIPTIONS = "Show Description texts as PlantUML notes";
    private static final String TOOLTIP_SHOW_CARDINALITIES = "Show cardinalities on association arrows";
    private static final String TOOLTIP_SHOW_POLICY_COMPONENTS = "Show policy components";
    private static final String TOOLTIP_SHOW_PRODUCT_COMPONENTS = "Show product components";
    private static final String TOOLTIP_CLEAR_DEPENDENCIES = "Clear all resolved dependencies and remove them from the tree";
    private static final String TOOLTIP_RESOLVE_DEPENDENCIES = "Resolve model directories in dependencies";
    private static final String TOOLTIP_PACKAGE_FILTER = "Filter the diagram by packages (comma separated list)";
    private static final String TOOLTIP_CONNECTOR_LENGTH = "Length of association connectors";
    private static final String TOOLTIP_GENERATE = "Generate PlantUML from selected IPS model directories";
    private static final String TOOLTIP_SEARCH_FIELD = "Search for IPS classes by name. Supports * wildcard (e.g. *Contract*) and regex (e.g. Contract|Policy, Policy.*Type). Emptying the field clears the search.";
    private static final String TOOLTIP_SEARCH_BUTTON = "Search for IPS classes matching the pattern";
    private static final String TOOLTIP_RESET_ALL_OPTIONS = "Reset all options to their defaults";
    private static final String TOOLTIP_RESET_ALL = "Reset everything: model directory selection, search, search options, and all options to defaults; clears the diagram";

    private static final int PREFERRED_WIDTH = 350;

    private final Project project;

    // Managers
    private final GenerationStateManager generationState = new GenerationStateManager();
    private final ModelDirTreeManager treeManager;
    private final DependencyManager depManager;
    private final SearchManager searchManager;
    private final GenerationManager generationManager;

    // Search UI (panel-owned)
    private final JTextField searchField = withTooltip(new JTextField(15), TOOLTIP_SEARCH_FIELD);

    // Options
    private final JCheckBox selectAllOptionsCheck = new JCheckBox(LABEL_SELECT_ALL_OPTIONS);
    private final JCheckBox packagesCheck = withTooltip(new JCheckBox(LABEL_PACKAGES), TOOLTIP_PACKAGES);
    private final JCheckBox printTargetRoleCheck = withTooltip(new JCheckBox(LABEL_PRINT_TARGET_ROLE), TOOLTIP_PRINT_TARGET_ROLE);
    private final JCheckBox addSuperTypeCheck = withTooltip(new JCheckBox(LABEL_EXTERNAL_SUPERTYPES), TOOLTIP_EXTERNAL_SUPERTYPES);
    private final JCheckBox addAssociationsCheck = withTooltip(new JCheckBox(LABEL_EXTERNAL_ASSOCIATIONS), TOOLTIP_EXTERNAL_ASSOCIATIONS);
    private final JCheckBox showTablesCheck = withTooltip(new JCheckBox(LABEL_SHOW_TABLE_STRUCTURES), TOOLTIP_SHOW_TABLE_STRUCTURES);
    private final JCheckBox showTableUsageCheck = withTooltip(new JCheckBox(LABEL_SHOW_TABLE_USAGE), TOOLTIP_SHOW_TABLE_USAGE);
    private final JCheckBox showEnumTypesCheck = withTooltip(new JCheckBox(LABEL_SHOW_ENUM_TYPES), TOOLTIP_SHOW_ENUM_TYPES);
    private final JCheckBox showEnumContentCheck = withTooltip(new JCheckBox(LABEL_SHOW_ENUM_CONTENT), TOOLTIP_SHOW_ENUM_CONTENT);
    private final JCheckBox showEnumAssocCheck = withTooltip(new JCheckBox(LABEL_SHOW_ENUM_ASSOCIATIONS), TOOLTIP_SHOW_ENUM_ASSOCIATIONS);
    private final JCheckBox showMavenModuleCheck = withTooltip(new JCheckBox(LABEL_SHOW_MAVEN_MODULE), TOOLTIP_SHOW_MAVEN_MODULE);
    private final JCheckBox showDescriptionsCheck = withTooltip(new JCheckBox(LABEL_SHOW_DESCRIPTIONS), TOOLTIP_SHOW_DESCRIPTIONS);
    private final JCheckBox showCardinalitiesCheck = withTooltip(new JCheckBox(LABEL_SHOW_CARDINALITIES), TOOLTIP_SHOW_CARDINALITIES);
    private final JCheckBox showPolicyCheck = withTooltip(new JCheckBox(LABEL_SHOW_POLICY_COMPONENTS, true), TOOLTIP_SHOW_POLICY_COMPONENTS);
    private final JCheckBox showProductCheck = withTooltip(new JCheckBox(LABEL_SHOW_PRODUCT_COMPONENTS), TOOLTIP_SHOW_PRODUCT_COMPONENTS);
    private final JTextField packageFilterField = withTooltip(new JTextField(15), TOOLTIP_PACKAGE_FILTER);
    private final JLabel connectorLengthLabel = new JLabel(LABEL_CONNECTOR_LENGTH);
    private final JSpinner connectorLengthSpinner = withTooltip(new JSpinner(new SpinnerNumberModel(2, 1, 10, 1)), TOOLTIP_CONNECTOR_LENGTH);
    private JCheckBox[] allOptionChecks;

    private static <T extends javax.swing.JComponent> T withTooltip(T component, String tooltip) {
        component.setToolTipText(tooltip);
        return component;
    }

    public Ips2PlantToolWindowPanel(Project project) {
        this.project = project;
        treeManager = new ModelDirTreeManager(project);
        depManager = new DependencyManager(project, treeManager);
        searchManager = new SearchManager(project, treeManager, generationState);
        generationManager = new GenerationManager(project, searchManager, treeManager, depManager, generationState, this::getOptions);

        treeManager.setOnNodeStateChanged(() -> {
            if (Ips2PlantSettings.getInstance().retriggerOnDirChange) {
                if (generationState.isSearchActive()) {
                    searchManager.runSearch(searchField.getText().trim());
                } else {
                    generationManager.scheduleRegeneration();
                }
            }
        });
        searchManager.setOnResultsUpdated(generationManager::runGeneration);
        searchManager.setOnOptionsReset(() -> resetOptions(allOptionChecks));
        searchManager.setOnOptionsAutoEnabled(this::autoEnableOptionsForResults);

        setPreferredSize(new Dimension(PREFERRED_WIDTH, 600));
        buildUi();
        treeManager.detectModelDirs();

        ApplicationManager.getApplication().getMessageBus().connect(project)
                .subscribe(Ips2PlantSettings.SettingsListener.TOPIC,
                        () -> updateConnectorLengthVisibility());
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 4));

        // --- Top: Model directories panel ---
        var modelSection = new JPanel(new BorderLayout(0, 2));
        modelSection.setBorder(BorderFactory.createTitledBorder(TITLE_MODEL_DIRECTORIES));

        var treeScrollPane = new JScrollPane(treeManager.getTree());
        treeScrollPane.setBorder(null);
        modelSection.add(treeScrollPane, BorderLayout.CENTER);

        var generateButton = new JButton(LABEL_GENERATE);
        generateButton.setToolTipText(TOOLTIP_GENERATE);
        generateButton.addActionListener(e -> {
            searchField.setText("");
            searchManager.clearSearchResults();
            if (Ips2PlantSettings.getInstance().generateResetsOptions) {
                resetOptions(allOptionChecks);
            }
            generationState.activateModelGeneration();
            generationManager.runGeneration();
        });
        var clearDepsButton = withTooltip(new JButton(LABEL_CLEAR_DEPENDENCIES), TOOLTIP_CLEAR_DEPENDENCIES);
        clearDepsButton.addActionListener(e -> depManager.clearDependencies());
        var resolveButton = withTooltip(new JButton(LABEL_RESOLVE_DEPENDENCIES), TOOLTIP_RESOLVE_DEPENDENCIES);
        resolveButton.addActionListener(e -> depManager.resolveDependencies(resolveButton));
        var resetAllButton = withTooltip(new JButton(LABEL_RESET_ALL), TOOLTIP_RESET_ALL);
        resetAllButton.addActionListener(e -> resetAll());

        var buttonsPanel = new JPanel(new GridBagLayout());
        var bgbc = new GridBagConstraints();
        bgbc.insets = new Insets(2, 4, 2, 4);
        bgbc.fill = GridBagConstraints.HORIZONTAL;
        bgbc.gridy = 0; bgbc.gridx = 0; bgbc.weightx = 0.5;
        buttonsPanel.add(resolveButton, bgbc);
        bgbc.gridx = 1; bgbc.weightx = 0.5;
        buttonsPanel.add(clearDepsButton, bgbc);
        bgbc.gridy = 1; bgbc.gridx = 0; bgbc.gridwidth = 1; bgbc.weightx = 0.5;
        buttonsPanel.add(generateButton, bgbc);
        bgbc.gridx = 1; bgbc.weightx = 0.5;
        buttonsPanel.add(resetAllButton, bgbc);
        modelSection.add(buttonsPanel, BorderLayout.SOUTH);
        modelSection.setMinimumSize(new Dimension(0, 80));

        // --- Middle: Search section ---
        var searchSection = new JPanel(new BorderLayout(0, 2));
        searchSection.setBorder(BorderFactory.createTitledBorder(TITLE_SEARCH));

        var searchInputPanel = new JPanel(new BorderLayout(4, 0));
        searchField.addActionListener(e -> searchManager.runSearch(searchField.getText().trim()));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { /* no-op */ }
            @Override public void changedUpdate(DocumentEvent e) { /* no-op */ }
            @Override
            public void removeUpdate(DocumentEvent e) {
                if (searchField.getText().isEmpty()) {
                    generationState.deactivateSearch();
                    searchManager.clearSearchResults();
                    GenerationManager.openInEditor(project, GenerationManager.EMPTY_PUML);
                }
            }
        });
        searchInputPanel.add(searchField, BorderLayout.CENTER);
        var searchButton = withTooltip(new JButton(LABEL_SEARCH), TOOLTIP_SEARCH_BUTTON);
        searchButton.addActionListener(e -> searchManager.runSearch(searchField.getText().trim()));
        searchInputPanel.add(searchButton, BorderLayout.EAST);
        searchSection.add(searchInputPanel, BorderLayout.NORTH);
        searchSection.add(searchManager.getResultsCardPanel(), BorderLayout.CENTER);

        var searchBottomPanel = new JPanel(new GridBagLayout());
        var sgbc = new GridBagConstraints();
        sgbc.anchor = GridBagConstraints.WEST;
        sgbc.insets = new Insets(2, 4, 2, 4);
        sgbc.fill = GridBagConstraints.HORIZONTAL;
        sgbc.gridy = 0;
        sgbc.gridx = 0; sgbc.weightx = 0.5;
        searchBottomPanel.add(searchManager.addSupertypesCheck, sgbc);
        sgbc.gridx = 1; sgbc.weightx = 0.5;
        searchBottomPanel.add(searchManager.addReferencingCheck, sgbc);
        sgbc.gridy = 1; sgbc.gridx = 0; sgbc.gridwidth = 1; sgbc.weightx = 0.5;
        searchBottomPanel.add(searchManager.getSelectAllButton(), sgbc);
        searchSection.add(searchBottomPanel, BorderLayout.SOUTH);
        searchSection.setMinimumSize(new Dimension(0, 80));

        // --- Bottom: Options ---
        allOptionChecks = new JCheckBox[]{ packagesCheck, printTargetRoleCheck, addSuperTypeCheck, addAssociationsCheck,
                showPolicyCheck, showProductCheck, showTablesCheck, showTableUsageCheck,
                showEnumTypesCheck, showEnumContentCheck, showEnumAssocCheck, showMavenModuleCheck, showDescriptionsCheck, showCardinalitiesCheck };

        var optionsPanel = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        selectAllOptionsCheck.addActionListener(e -> {
            boolean selected = selectAllOptionsCheck.isSelected();
            for (var cb : allOptionChecks) {
                if (selected && cb == showDescriptionsCheck
                        && Ips2PlantSettings.getInstance().selectAllIgnoresDescriptions) {
                    continue;
                }
                cb.setSelected(selected);
            }
        });
        for (var cb : allOptionChecks) {
            cb.addActionListener(e -> updateSelectAllState(allOptionChecks));
        }

        var resetOptionsButton = withTooltip(new JButton(LABEL_RESET_ALL_OPTIONS), TOOLTIP_RESET_ALL_OPTIONS);
        resetOptionsButton.addActionListener(e -> {
            resetOptions(allOptionChecks);
            generationManager.scheduleRegeneration();
        });

        int row = 0;
        gbc.gridy = row++;
        gbc.gridx = 0; gbc.weightx = 0.5;
        optionsPanel.add(selectAllOptionsCheck, gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.NONE;
        optionsPanel.add(resetOptionsButton, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JCheckBox[] leftColumn  = { showPolicyCheck, showProductCheck, showTablesCheck, showEnumTypesCheck, showEnumContentCheck, showTableUsageCheck, showEnumAssocCheck };
        JCheckBox[] rightColumn = { packagesCheck, showCardinalitiesCheck, printTargetRoleCheck, addSuperTypeCheck, addAssociationsCheck, showMavenModuleCheck, showDescriptionsCheck };

        int maxRows = Math.max(leftColumn.length, rightColumn.length);
        for (int i = 0; i < maxRows; i++) {
            gbc.gridy = row++;
            if (i < leftColumn.length) {
                gbc.gridx = 0; gbc.weightx = 0.5;
                optionsPanel.add(leftColumn[i], gbc);
            }
            if (i < rightColumn.length) {
                gbc.gridx = 1; gbc.weightx = 0.5;
                optionsPanel.add(rightColumn[i], gbc);
            }
        }

        gbc.gridy = row++;
        gbc.gridx = 0; gbc.weightx = 0;
        optionsPanel.add(new JLabel(LABEL_PACKAGE_FILTER), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        optionsPanel.add(packageFilterField, gbc);

        gbc.gridy = row;
        gbc.gridx = 0; gbc.weightx = 0;
        optionsPanel.add(connectorLengthLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        optionsPanel.add(connectorLengthSpinner, gbc);
        updateConnectorLengthVisibility();

        var optionsScrollPane = new JScrollPane(optionsPanel);
        optionsScrollPane.setBorder(BorderFactory.createTitledBorder(TITLE_OPTIONS));
        optionsScrollPane.setMinimumSize(new Dimension(0, 80));

        // --- Split panes ---
        var bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchSection, optionsScrollPane);
        bottomSplit.setResizeWeight(0.5);
        bottomSplit.setDividerSize(6);
        bottomSplit.setContinuousLayout(true);

        var topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, modelSection, bottomSplit);
        topSplit.setResizeWeight(0.33);
        topSplit.setDividerSize(6);
        topSplit.setContinuousLayout(true);
        add(topSplit, BorderLayout.CENTER);

        setupAutoRegeneration(allOptionChecks);
    }

    private void setupAutoRegeneration(JCheckBox[] allOptionChecks) {
        for (var cb : allOptionChecks) {
            cb.addActionListener(e -> generationManager.scheduleRegeneration());
        }
        selectAllOptionsCheck.addActionListener(e -> generationManager.scheduleRegeneration());

        packageFilterField.addActionListener(e -> generationManager.scheduleRegeneration());
        packageFilterField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) { 
                if (!e.isTemporary()) { 
                    generationManager.scheduleRegeneration(); 
                } 
            }
        });
        packageFilterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { /* no-op */ }
            @Override public void changedUpdate(DocumentEvent e) { /* no-op */ }
            @Override
            public void removeUpdate(DocumentEvent e) {
                if (packageFilterField.getText().isEmpty()) {
                    generationManager.scheduleRegeneration();
                }
            }
        });

        connectorLengthSpinner.addChangeListener(e -> generationManager.scheduleRegeneration());

        searchManager.addSupertypesCheck.addActionListener(e -> {
            if (generationState.isSearchActive()) generationManager.scheduleRegeneration();
        });
        searchManager.addReferencingCheck.addActionListener(e -> {
            if (generationState.isSearchActive()) generationManager.scheduleRegeneration();
        });
    }

    public Ips2PlantOptions getOptions() {
        var options = new Ips2PlantOptions();
        options.setPackages(packagesCheck.isSelected());
        options.setPrintTargetRole(printTargetRoleCheck.isSelected());
        options.setAddSuperType(addSuperTypeCheck.isSelected());
        options.setAddAssociations(addAssociationsCheck.isSelected());
        options.setShowTables(showTablesCheck.isSelected());
        options.setShowTableUsage(showTableUsageCheck.isSelected());
        options.setShowEnumTypes(showEnumTypesCheck.isSelected());
        options.setShowEnumContent(showEnumContentCheck.isSelected());
        options.setShowEnumAssociations(showEnumAssocCheck.isSelected());
        options.setShowPolicyComponents(showPolicyCheck.isSelected());
        options.setShowProductComponents(showProductCheck.isSelected());
        options.setShowMavenModule(showMavenModuleCheck.isSelected());
        options.setShowDescriptions(showDescriptionsCheck.isSelected());
        options.setShowCardinalities(showCardinalitiesCheck.isSelected());
        options.setSortAttributesAlphabetically(Ips2PlantSettings.getInstance().sortAttributesAlphabetically);
        options.setDescriptionLocale(Ips2PlantSettings.getInstance().locale);
        options.setPackageFilter(packageFilterField.getText().trim());
        options.setConnectorLength((int) connectorLengthSpinner.getValue());
        return options;
    }

    private void resetOptions(JCheckBox[] checks) {
        selectAllOptionsCheck.setSelected(false);
        for (var cb : checks) {
            cb.setSelected(false);
        }
        showPolicyCheck.setSelected(true);
        packageFilterField.setText("");
        connectorLengthSpinner.setValue(2);
    }

    private void resetAll() {
        generationState.reset();
        treeManager.uncheckAll();
        searchField.setText("");
        searchManager.clearSearchResults();
        searchManager.resetSearchOptions();
        resetOptions(allOptionChecks);
        GenerationManager.openInEditor(project, GenerationManager.EMPTY_PUML);
    }

    private void updateSelectAllState(JCheckBox[] checks) {
        boolean ignoreDescriptions = Ips2PlantSettings.getInstance().selectAllIgnoresDescriptions;
        boolean allSelected = true;
        for (var cb : checks) {
            if (ignoreDescriptions && cb == showDescriptionsCheck) continue;
            if (!cb.isSelected()) {
                allSelected = false;
                break;
            }
        }
        selectAllOptionsCheck.setSelected(allSelected);
    }

    private void autoEnableOptionsForResults(Map<String, File> results) {
        for (var file : results.values()) {
            var name = file.getName();
            if (name.endsWith(".ipspolicycmpttype")) {
                showPolicyCheck.setSelected(true);
            } else if (name.endsWith(".ipsproductcmpttype")) {
                showProductCheck.setSelected(true);
            } else if (name.endsWith(".ipstablestructure")) {
                showTablesCheck.setSelected(true);
            } else if (name.endsWith(".ipsenumtype")) {
                showEnumTypesCheck.setSelected(true);
            } else if (name.endsWith(".ipsenumcontent")) {
                showEnumContentCheck.setSelected(true);
            }
        }
    }

    private void updateConnectorLengthVisibility() {
        boolean visible = Ips2PlantSettings.getInstance().showConnectorLength;
        connectorLengthLabel.setVisible(visible);
        connectorLengthSpinner.setVisible(visible);
    }

    private void showTemporaryStatusHint(String message, int durationMs) {
        var balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
                .setFadeoutTime(durationMs)
                .createBalloon();
        balloon.show(RelativePoint.getCenterOf(this), Balloon.Position.above);
    }
}
