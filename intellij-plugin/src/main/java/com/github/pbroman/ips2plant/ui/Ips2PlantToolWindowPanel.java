package com.github.pbroman.ips2plant.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import com.github.pbroman.ips2plant.action.GeneratePlantUmlAction;
import com.github.pbroman.ips2plant.core.Ips2PlantGenerator;
import com.github.pbroman.ips2plant.core.Ips2PlantOptions;
import com.github.pbroman.ips2plant.core.IpsClassSearcher;
import com.github.pbroman.ips2plant.core.IpsProjectDetector;
import com.github.pbroman.ips2plant.core.MavenDependencyCollector;
import com.github.pbroman.ips2plant.core.MavenDependencyCollector.DependencyModel;
import com.github.pbroman.ips2plant.settings.Ips2PlantSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeListener;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

public class Ips2PlantToolWindowPanel extends JPanel {

    private static final Logger LOG = Logger.getInstance(Ips2PlantToolWindowPanel.class);

    private static final String TITLE_MODEL_DIRECTORIES = "Model Directories";
    private static final String TITLE_OPTIONS = "Options";
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
    private static final String LABEL_SHOW_POLICY_COMPONENTS = "Policy Components";
    private static final String LABEL_SHOW_PRODUCT_COMPONENTS = "Product Components";
    private static final String LABEL_CLEAR_DEPENDENCIES = "Clear Dependencies";
    private static final String LABEL_RESOLVE_DEPENDENCIES = "Resolve Dependencies";
    private static final String LABEL_PACKAGE_FILTER = "Package Filter:";
    private static final String LABEL_CONNECTOR_LENGTH = "Connector Length:";
    private static final String LABEL_GENERATE = "Generate Model UML";
    private static final String LABEL_NO_IPS_PROJECTS = "No .ipsproject files found.";
    private static final String LABEL_DEPENDENCIES = "dependencies";
    private static final String TASK_TITLE = "Generating PlantUML from IPS Models";
    private static final String TASK_TITLE_RESOLVE = "Resolving Maven Dependencies";
    private static final String TASK_STATUS_COLLECTING = "Collecting IPS files...";

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
    private static final String TOOLTIP_SHOW_POLICY_COMPONENTS = "Show policy components";
    private static final String TOOLTIP_SHOW_PRODUCT_COMPONENTS = "Show product components";
    private static final String TOOLTIP_CLEAR_DEPENDENCIES = "Clear all resolved dependencies and remove them from the tree";
    private static final String TOOLTIP_RESOLVE_DEPENDENCIES = "Resolve model directories in dependencies";
    private static final String TOOLTIP_PACKAGE_FILTER = "Filter the diagram by packages (comma separated list)";
    private static final String TOOLTIP_CONNECTOR_LENGTH = "Length of association connectors";
    private static final String TOOLTIP_GENERATE = "Generate PlantUML from selected IPS model directories";

    private static final String TITLE_SEARCH = "Search";
    private static final String LABEL_SEARCH = "Search";
    private static final String LABEL_ADD_SUPERTYPES = "Add Supertypes";
    private static final String LABEL_ADD_REFERENCING = "Add Referencing Classes";
    private static final String TOOLTIP_SEARCH_FIELD = "Search for IPS classes by name. Supports * wildcard (e.g. *Contract*) and regex (e.g. Contract|Policy, Policy.*Type). Emptying the field clears the search.";
    private static final String TOOLTIP_SEARCH_BUTTON = "Search for IPS classes matching the pattern";
    private static final String TOOLTIP_ADD_SUPERTYPES = "Transitively add all supertypes (parents, grandparents, etc.) of found classes";
    private static final String TOOLTIP_ADD_REFERENCING = "Add all classes that reference found classes through associations";
    private static final String TOOLTIP_SEARCH_RESULTS = "Check/uncheck classes to include in the generated PlantUML diagram";
    private static final String LABEL_SELECT_ALL_SEARCH = "Select All Results";
    private static final String LABEL_DESELECT_ALL_SEARCH = "Deselect All Results";
    private static final String TOOLTIP_SELECT_ALL_SEARCH = "Select or deselect all found classes";
    private static final int SELECT_ALL_SEARCH_THRESHOLD = 3;
    private static final String TASK_TITLE_SEARCH = "Searching IPS Classes";

    private static final String LABEL_SELECT_ALL_OPTIONS = "Select All";
    private static final String LABEL_RESET_ALL_OPTIONS = "Reset to Default";
    private static final String TOOLTIP_RESET_ALL_OPTIONS = "Reset all options to their defaults";
    private static final String LABEL_RESET_ALL = "Reset All";
    private static final String TOOLTIP_RESET_ALL = "Reset everything: model directory selection, search, search options, and all options to defaults; clears the diagram";
    private static final String EMPTY_PUML = "@startuml\nnote \"Nothing to show\" as N1\n@enduml\n";

    private final Project project;

    // Model directory selection
    private CheckboxTree modelDirTree;
    private CheckedTreeNode treeRoot;
    private boolean propagating;
    // Track temp dirs from dependency extraction for cleanup
    private final List<Path> dependencyTempRoots = new ArrayList<>();
    // Track dependency model dir -> maven module (groupId:artifactId) for resolved dependencies
    private final Map<Path, String> dependencyMavenModules = new LinkedHashMap<>();

    private final GenerationStateManager generationState = new GenerationStateManager();

    // Search
    private final JTextField searchField = withTooltip(new JTextField(15), TOOLTIP_SEARCH_FIELD);
    private final JCheckBox addSupertypesCheck = withTooltip(new JCheckBox(LABEL_ADD_SUPERTYPES), TOOLTIP_ADD_SUPERTYPES);
    private final JCheckBox addReferencingCheck = withTooltip(new JCheckBox(LABEL_ADD_REFERENCING), TOOLTIP_ADD_REFERENCING);
    private final CheckBoxList<String> searchResultsList = new CheckBoxList<>();
    private final JLabel noResultsLabel = new JLabel("No IPS classes found");
    private final CardLayout searchResultsCardLayout = new CardLayout();
    private final JPanel searchResultsCardPanel = new JPanel(searchResultsCardLayout);
    private static final String CARD_RESULTS = "results";
    private static final String CARD_NO_RESULTS = "noResults";
    private final JButton selectAllSearchButton = withTooltip(new JButton(LABEL_SELECT_ALL_SEARCH), TOOLTIP_SELECT_ALL_SEARCH);
    private final Map<String, File> searchResults = new LinkedHashMap<>();
    private boolean allSearchResultsSelected = true;

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
    private final JCheckBox showPolicyCheck = withTooltip(new JCheckBox(LABEL_SHOW_POLICY_COMPONENTS, true), TOOLTIP_SHOW_POLICY_COMPONENTS);
    private final JCheckBox showProductCheck = withTooltip(new JCheckBox(LABEL_SHOW_PRODUCT_COMPONENTS), TOOLTIP_SHOW_PRODUCT_COMPONENTS);
    private final JTextField packageFilterField = withTooltip(new JTextField(15), TOOLTIP_PACKAGE_FILTER);
    private final JSpinner connectorLengthSpinner = withTooltip(new JSpinner(new SpinnerNumberModel(2, 1, 10, 1)), TOOLTIP_CONNECTOR_LENGTH);
    private JCheckBox[] allOptionChecks;

    private static <T extends javax.swing.JComponent> T withTooltip(T component, String tooltip) {
        component.setToolTipText(tooltip);
        return component;
    }

    private static final int PREFERRED_WIDTH = 350;
    private static final int DEBOUNCE_DELAY_MS = 500;

    private final Timer regenerateTimer;

    public Ips2PlantToolWindowPanel(Project project) {
        this.project = project;
        regenerateTimer = new Timer(DEBOUNCE_DELAY_MS, e -> runGeneration());
        regenerateTimer.setRepeats(false);
        setPreferredSize(new Dimension(PREFERRED_WIDTH, 600));
        buildUi();
        detectModelDirs();
    }

    private void buildUi() {
        setLayout(new BorderLayout(0, 4));

        // --- Top: Model directories panel ---
        var modelSection = new JPanel(new BorderLayout(0, 2));
        modelSection.setBorder(BorderFactory.createTitledBorder(TITLE_MODEL_DIRECTORIES));

        treeRoot = new CheckedTreeNode(TITLE_MODEL_DIRECTORIES);
        modelDirTree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(JTree tree, Object value, boolean selected,
                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof CheckedTreeNode node) {
                    var userObject = node.getUserObject();
                    if (userObject instanceof ModelDirEntry entry) {
                        getTextRenderer().append(entry.displayName(),
                                SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    } else if (userObject instanceof String text) {
                        getTextRenderer().append(text,
                                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    }
                }
            }
        }, treeRoot);
        modelDirTree.setRootVisible(false);
        modelDirTree.setShowsRootHandles(true);
        modelDirTree.addCheckboxTreeListener(new CheckboxTreeListener() {
            @Override
            public void nodeStateChanged(@NotNull CheckedTreeNode node) {
                if (propagating) return;
                propagating = true;
                try {
                    boolean checked = node.isChecked();
                    setDescendantsChecked(node, checked);
                    updateAncestorsChecked(node);
                } finally {
                    propagating = false;
                }
                if (Ips2PlantSettings.getInstance().retriggerOnDirChange) {
                    if (generationState.isSearchActive()) {
                        runSearch();
                    } else {
                        scheduleRegeneration();
                    }
                }
            }
        });

        var treeScrollPane = new JScrollPane(modelDirTree);
        treeScrollPane.setBorder(null);
        modelSection.add(treeScrollPane, BorderLayout.CENTER);

        // Buttons below the tree
        var generateButton = new JButton(LABEL_GENERATE);
        generateButton.setToolTipText(TOOLTIP_GENERATE);
        generateButton.addActionListener(e -> {
            searchField.setText("");
            clearSearchResults();
            if (Ips2PlantSettings.getInstance().generateResetsOptions) {
                resetOptions(allOptionChecks);
            }
            generationState.activateModelGeneration();
            runGeneration();
        });
        var clearDepsButton = new JButton(LABEL_CLEAR_DEPENDENCIES);
        clearDepsButton.setToolTipText(TOOLTIP_CLEAR_DEPENDENCIES);
        clearDepsButton.addActionListener(e -> clearDependencies());
        var resolveButton = new JButton(LABEL_RESOLVE_DEPENDENCIES);
        resolveButton.setToolTipText(TOOLTIP_RESOLVE_DEPENDENCIES);
        resolveButton.addActionListener(e -> resolveDependencies());
        var buttonsPanel = new JPanel(new GridBagLayout());
        var bgbc = new GridBagConstraints();
        bgbc.insets = new Insets(2, 4, 2, 4);
        bgbc.fill = GridBagConstraints.HORIZONTAL;
        bgbc.gridy = 0; bgbc.gridx = 0; bgbc.weightx = 0.5;
        buttonsPanel.add(resolveButton, bgbc);
        bgbc.gridx = 1; bgbc.weightx = 0.5;
        buttonsPanel.add(clearDepsButton, bgbc);
        var resetAllButton = new JButton(LABEL_RESET_ALL);
        resetAllButton.setToolTipText(TOOLTIP_RESET_ALL);
        resetAllButton.addActionListener(e -> resetAll());
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
        searchField.addActionListener(e -> runSearch());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { /* no-op */ }
            @Override public void changedUpdate(DocumentEvent e) { /* no-op */ }
            @Override
            public void removeUpdate(DocumentEvent e) {
                if (searchField.getText().isEmpty()) {
                    generationState.deactivateSearch();
                    clearSearchResults();
                    GeneratePlantUmlAction.openInEditor(project, EMPTY_PUML);
                }
            }
        });
        searchInputPanel.add(searchField, BorderLayout.CENTER);
        var searchButton = new JButton(LABEL_SEARCH);
        searchButton.setToolTipText(TOOLTIP_SEARCH_BUTTON);
        searchButton.addActionListener(e -> runSearch());
        searchInputPanel.add(searchButton, BorderLayout.EAST);
        searchSection.add(searchInputPanel, BorderLayout.NORTH);

        // Select/Deselect All button for search results
        selectAllSearchButton.setVisible(false);
        selectAllSearchButton.addActionListener(e -> {
            allSearchResultsSelected = !allSearchResultsSelected;
            searchResultsList.clear();
            for (var className : searchResults.keySet()) {
                searchResultsList.addItem(className, className, allSearchResultsSelected);
            }
            selectAllSearchButton.setText(allSearchResultsSelected ? LABEL_DESELECT_ALL_SEARCH : LABEL_SELECT_ALL_SEARCH);
            if (allSearchResultsSelected) {
                scheduleRegeneration();
            }
        });

        searchResultsList.setToolTipText(TOOLTIP_SEARCH_RESULTS);
        searchResultsList.setCheckBoxListListener((index, value) -> {
            scheduleRegeneration();
        });
        var searchResultsScroll = new JScrollPane(searchResultsList);
        searchResultsScroll.setBorder(null);
        noResultsLabel.setForeground(new Color(200, 0, 0));
        noResultsLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        searchResultsCardPanel.add(searchResultsScroll, CARD_RESULTS);
        searchResultsCardPanel.add(noResultsLabel, CARD_NO_RESULTS);
        searchResultsCardLayout.show(searchResultsCardPanel, CARD_RESULTS);

        searchSection.add(searchResultsCardPanel, BorderLayout.CENTER);

        var searchBottomPanel = new JPanel(new GridBagLayout());
        var sgbc = new GridBagConstraints();
        sgbc.anchor = GridBagConstraints.WEST;
        sgbc.insets = new Insets(2, 4, 2, 4);
        sgbc.fill = GridBagConstraints.HORIZONTAL;
        sgbc.gridy = 0;
        sgbc.gridx = 0; sgbc.weightx = 0.5;
        searchBottomPanel.add(addSupertypesCheck, sgbc);
        sgbc.gridx = 1; sgbc.weightx = 0.5;
        searchBottomPanel.add(addReferencingCheck, sgbc);
        sgbc.gridy = 1; sgbc.gridx = 0; sgbc.gridwidth = 1; sgbc.weightx = 0.5;
        searchBottomPanel.add(selectAllSearchButton, sgbc);
        searchSection.add(searchBottomPanel, BorderLayout.SOUTH);

        searchSection.setMinimumSize(new Dimension(0, 80));

        // --- Bottom: Options ---
        var optionsPanel = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        allOptionChecks = new JCheckBox[]{ packagesCheck, printTargetRoleCheck, addSuperTypeCheck, addAssociationsCheck, showPolicyCheck,
                showProductCheck, showTablesCheck, showTableUsageCheck, showEnumTypesCheck, showEnumContentCheck, showEnumAssocCheck, showMavenModuleCheck, showDescriptionsCheck };

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

        var resetOptionsButton = new JButton(LABEL_RESET_ALL_OPTIONS);
        resetOptionsButton.setToolTipText(TOOLTIP_RESET_ALL_OPTIONS);
        resetOptionsButton.addActionListener(e -> {
            resetOptions(allOptionChecks);
            scheduleRegeneration();
        });

        gbc.gridy = row++;
        gbc.gridx = 0; gbc.weightx = 0.5;
        optionsPanel.add(selectAllOptionsCheck, gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.NONE;
        optionsPanel.add(resetOptionsButton, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JCheckBox[] leftColumn = { showPolicyCheck, showProductCheck, showTablesCheck, showTableUsageCheck, showEnumTypesCheck, showEnumContentCheck };
        JCheckBox[] rightColumn = { packagesCheck, printTargetRoleCheck, addSuperTypeCheck, addAssociationsCheck, showMavenModuleCheck, showEnumAssocCheck, showDescriptionsCheck };

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
        optionsPanel.add(new JLabel(LABEL_CONNECTOR_LENGTH), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        optionsPanel.add(connectorLengthSpinner, gbc);

        // Wrap options in a scroll pane, pushing content to top
        var optionsScrollPane = new JScrollPane(optionsPanel);
        optionsScrollPane.setBorder(BorderFactory.createTitledBorder(TITLE_OPTIONS));
        optionsScrollPane.setMinimumSize(new Dimension(0, 80));

        // --- Split panes: model dirs (top) + search (middle) + options (bottom) ---
        var bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchSection, optionsScrollPane);
        bottomSplit.setResizeWeight(0.5);
        bottomSplit.setDividerSize(6);
        bottomSplit.setContinuousLayout(true);

        var topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, modelSection, bottomSplit);
        topSplit.setResizeWeight(0.33);
        topSplit.setDividerSize(6);
        topSplit.setContinuousLayout(true);
        add(topSplit, BorderLayout.CENTER);

        // --- Auto-regeneration listeners ---
        setupAutoRegeneration(allOptionChecks);
    }

    private void setupAutoRegeneration(JCheckBox[] allOptionChecks) {
        // Checkboxes trigger immediate regeneration
        Runnable scheduleRegenerate = this::scheduleRegeneration;
        for (var cb : allOptionChecks) {
            cb.addActionListener(e -> scheduleRegenerate.run());
        }
        selectAllOptionsCheck.addActionListener(e -> scheduleRegenerate.run());

        // Package filter: regenerate on Enter, focus lost, or when emptied
        packageFilterField.addActionListener(e -> scheduleRegeneration());
        packageFilterField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) { scheduleRegeneration(); }
        });
        packageFilterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override 
            public void insertUpdate(DocumentEvent e) { /* no-op */ }
            @Override 
            public void changedUpdate(DocumentEvent e) { /* no-op */ }
            @Override
            public void removeUpdate(DocumentEvent e) {
                if (packageFilterField.getText().isEmpty()) {
                    scheduleRegeneration();
                }
            }
        });

        // Connector length spinner: regenerate on value change
        connectorLengthSpinner.addChangeListener(e -> scheduleRegeneration());

        // Search expansion checkboxes: regenerate on change
        addSupertypesCheck.addActionListener(e -> { if (generationState.isSearchActive()) scheduleRegeneration(); });
        addReferencingCheck.addActionListener(e -> { if (generationState.isSearchActive()) scheduleRegeneration(); });
    }

    private void scheduleRegeneration() {
        if (generationState.shouldRegenerate()) {
            regenerateTimer.restart();
        }
    }

    private void detectModelDirs() {
        var basePath = project.getBasePath();
        if (basePath == null) return;

        var detector = new IpsProjectDetector();
        var detected = detector.detectModelDirs(Path.of(basePath));

        rebuildTree(detected);
    }

    private void rebuildTree(List<Path> detected) {
        treeRoot.removeAllChildren();

        var basePath = project.getBasePath();
        var base = basePath != null ? Path.of(basePath) : null;

        // Add detected dirs into the tree hierarchy
        for (var dir : detected) {
            var relativePath = base != null ? base.relativize(dir) : dir;
            insertIntoTree(treeRoot, relativePath, dir, false);
        }

        if (treeRoot.getChildCount() == 0) {
            var emptyNode = new CheckedTreeNode(LABEL_NO_IPS_PROJECTS);
            emptyNode.setEnabled(false);
            treeRoot.add(emptyNode);
        }

        compactTree(treeRoot);

        reloadTree();
    }

    private void reloadTree() {
        var treeModel = (javax.swing.tree.DefaultTreeModel) modelDirTree.getModel();
        treeModel.setRoot(treeRoot);
        treeModel.reload();
        collapseAllNodes();
    }

    private void resolveDependencies() {
        // Find pom directories from the currently detected model dirs
        var basePath = project.getBasePath();
        if (basePath == null) {
            LOG.warn("resolveDependencies: project basePath is null");
            return;
        }

        var allModelDirs = getSelectedLocalDirs();
        if (allModelDirs.isEmpty()) {
            allModelDirs = getAllLocalDirs();
            if (!allModelDirs.isEmpty()) {
                showTemporaryStatusHint(
                        "Resolving dependencies is more efficient if you check model directories to resolve from", 6000);
            }
        }
        if (allModelDirs.isEmpty()) {
            LOG.warn("resolveDependencies: no model dirs available, aborting");
            return;
        }
        LOG.info("resolveDependencies: found " + allModelDirs.size() + " model dirs");

        var pomDirs = new LinkedHashSet<Path>();
        for (var modelDir : allModelDirs) {
            LOG.info("resolveDependencies: searching pom.xml for model dir: " + modelDir);
            var pomDir = findPomDir(modelDir);
            if (pomDir != null) {
                LOG.info("resolveDependencies: found pom dir: " + pomDir);
                pomDirs.add(pomDir);
            } else {
                LOG.warn("resolveDependencies: no pom.xml found for model dir: " + modelDir);
            }
        }

        if (pomDirs.isEmpty()) {
            LOG.warn("resolveDependencies: no pom directories found, aborting");
            return;
        }

        var projectArtifactIds = collectProjectArtifactIds(Path.of(basePath));
        LOG.info("resolveDependencies: project artifact IDs to exclude: " + projectArtifactIds);
        LOG.info("resolveDependencies: resolving dependencies for " + pomDirs.size() + " pom dirs");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, TASK_TITLE_RESOLVE, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Resolving Maven classpath...");
                var collector = new MavenDependencyCollector();
                var depModels = new ArrayList<DependencyModel>();

                for (var pomDir : pomDirs) {
                    int prevCount = collector.getResolvedArtifactCount();
                    try {
                        LOG.info("resolveDependencies: collecting from " + pomDir);
                        var collected = collector.collectFromDependencies(pomDir, projectArtifactIds);
                        LOG.info("resolveDependencies: collected " + collected.size() + " dependency models from " + pomDir);
                        depModels.addAll(collected);
                    } catch (IOException | InterruptedException e) {
                        LOG.error("resolveDependencies: failed to resolve dependencies from " + pomDir, e);
                    }
                    if (collector.getResolvedArtifactCount() == prevCount) {
                        LOG.info("resolveDependencies: no new artifacts from " + pomDir + ", stopping early");
                        break;
                    }
                }

                LOG.info("resolveDependencies: total dependency models: " + depModels.size());
                ApplicationManager.getApplication().invokeLater(() -> addDependenciesToTree(depModels));
            }
        });
    }

    private void clearDependencies() {
        cleanupTempDirs();
        dependencyMavenModules.clear();
        removeDependenciesNode();
        reloadTree();
    }

    private void addDependenciesToTree(List<DependencyModel> depModels) {
        if (depModels.isEmpty()) return;

        // Clean up previous temp dirs and maven module mappings
        cleanupTempDirs();
        dependencyMavenModules.clear();

        // Remove existing dependencies node if present
        removeDependenciesNode();

        // Create the "dependencies" parent node
        var depsNode = new CheckedTreeNode(LABEL_DEPENDENCIES);

        for (var dep : depModels) {
            dependencyTempRoots.add(dep.tempRoot());
            if (dep.mavenModule() != null) {
                dependencyMavenModules.put(dep.modelDir(), dep.mavenModule());
            }

            // Create artifactId node
            var artifactNode = findChildByName(depsNode, dep.artifactId());
            if (artifactNode == null) {
                artifactNode = new CheckedTreeNode(dep.artifactId());
                depsNode.add(artifactNode);
            }

            // The modelDir is the "model" directory inside the temp extraction.
            // List its subdirectories as selectable leaves, similar to how local model dirs work.
            // But the modelDir itself is selectable as a whole.
            var modelDir = dep.modelDir();
            var entry = new ModelDirEntry("model", modelDir);
            var leafNode = new CheckedTreeNode(entry);
            leafNode.setChecked(false);
            artifactNode.add(leafNode);
        }

        treeRoot.add(depsNode);
        compactTree(depsNode);

        reloadTree();
    }

    private boolean isDependenciesNode(CheckedTreeNode node) {
        return node.getUserObject() instanceof String text
                && (text.equals(LABEL_DEPENDENCIES) || text.startsWith(LABEL_DEPENDENCIES + "/"));
    }

    private void removeDependenciesNode() {
        Enumeration<?> children = treeRoot.children();
        CheckedTreeNode toRemove = null;
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child
                    && isDependenciesNode(child)) {
                toRemove = child;
                break;
            }
        }
        if (toRemove != null) {
            treeRoot.remove(toRemove);
        }
    }

    private void collectAllModelDirs(CheckedTreeNode node, List<Path> result) {
        if (node.getUserObject() instanceof ModelDirEntry entry) {
            result.add(entry.absolutePath());
        }
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            var child = children.nextElement();
            if (child instanceof CheckedTreeNode checkedChild) {
                collectAllModelDirs(checkedChild, result);
            }
        }
    }

    private Set<String> collectProjectArtifactIds(Path projectRoot) {
        var artifactIds = new HashSet<String>();
        var artifactPattern = java.util.regex.Pattern.compile("<artifactId>([^<]+)</artifactId>");
        try (var stream = Files.walk(projectRoot)) {
            stream.filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .filter(Files::isRegularFile)
                    .forEach(pomFile -> {
                        try {
                            var content = Files.readString(pomFile);
                            // Only look at the part before <dependencies> to avoid collecting dependency artifactIds
                            var depsIndex = content.indexOf("<dependencies");
                            var header = depsIndex >= 0 ? content.substring(0, depsIndex) : content;
                            // Strip <parent> block to avoid collecting the parent artifactId
                            header = header.replaceAll("(?s)<parent>.*?</parent>", "");
                            var matcher = artifactPattern.matcher(header);
                            if (matcher.find()) {
                                artifactIds.add(matcher.group(1).trim());
                            }
                        } catch (IOException e) {
                            LOG.warn("Failed to read pom.xml: " + pomFile, e);
                        }
                    });
        } catch (IOException e) {
            LOG.warn("Failed to walk project directory: " + projectRoot, e);
        }
        return artifactIds;
    }

    private Path findPomDir(Path modelDir) {
        var current = modelDir.getParent();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private void insertIntoTree(CheckedTreeNode parent, Path relativePath, Path absolutePath, boolean checked) {
        int nameCount = relativePath.getNameCount();

        // Navigate/create intermediate folder nodes
        CheckedTreeNode current = parent;
        for (int i = 0; i < nameCount - 1; i++) {
            var segment = relativePath.getName(i).toString();
            var existing = findChildByName(current, segment);
            if (existing != null) {
                current = existing;
            } else {
                var folderNode = new CheckedTreeNode(segment);
                current.add(folderNode);
                current = folderNode;
            }
        }

        // Create the leaf node for the actual model directory
        var leafName = relativePath.getName(nameCount - 1).toString();
        var entry = new ModelDirEntry(leafName, absolutePath);
        var leafNode = new CheckedTreeNode(entry);
        leafNode.setChecked(checked);
        current.add(leafNode);
    }

    private CheckedTreeNode findChildByName(CheckedTreeNode parent, String name) {
        Enumeration<?> children = parent.children();
        while (children.hasMoreElements()) {
            var child = (CheckedTreeNode) children.nextElement();
            var userObject = child.getUserObject();
            if (userObject instanceof String text && text.equals(name)) {
                return child;
            }
        }
        return null;
    }

    private void compactTree(CheckedTreeNode node) {
        // Recursively compact children first
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof CheckedTreeNode child) {
                compactTree(child);
            }
        }
        // Merge single-child intermediate (non-leaf) nodes with their parent
        // Only merge if both parent and child are folder nodes (String user objects)
        // Never merge into the tree root — direct children of root must remain folder nodes
        // so that getSelectedLocalDirs/getSelectedDependencyDirs can iterate them correctly
        while (node != treeRoot
                && node.getChildCount() == 1
                && node.getChildAt(0) instanceof CheckedTreeNode onlyChild
                && onlyChild.getUserObject() instanceof String childName
                && node.getUserObject() instanceof String parentName) {
            // Move all grandchildren up to this node
            var merged = parentName + "/" + childName;
            node.setUserObject(merged);
            node.removeAllChildren();
            Enumeration<?> grandChildren = onlyChild.children();
            var toAdd = new ArrayList<CheckedTreeNode>();
            while (grandChildren.hasMoreElements()) {
                if (grandChildren.nextElement() instanceof CheckedTreeNode gc) {
                    toAdd.add(gc);
                }
            }
            for (var gc : toAdd) {
                node.add(gc);
            }
        }
    }

    private void collapseAllNodes() {
        for (int i = modelDirTree.getRowCount() - 1; i >= 0; i--) {
            modelDirTree.collapseRow(i);
        }
    }

    private List<Path> getSelectedLocalDirs() {
        var result = new ArrayList<Path>();
        Enumeration<?> children = treeRoot.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child
                    && !isDependenciesNode(child)) {
                collectCheckedDirs(child, result, false);
            }
        }
        return result;
    }

    private List<Path> getAllLocalDirs() {
        var result = new ArrayList<Path>();
        Enumeration<?> children = treeRoot.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child
                    && !isDependenciesNode(child)) {
                collectAllModelDirs(child, result);
            }
        }
        return result;
    }

    private void showTemporaryStatusHint(String message, int durationMs) {
        var balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
                .setFadeoutTime(durationMs)
                .createBalloon();
        balloon.show(RelativePoint.getCenterOf(this), Balloon.Position.above);
    }

    private List<Path> getSelectedDependencyDirs() {
        var result = new ArrayList<Path>();
        Enumeration<?> children = treeRoot.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child
                    && isDependenciesNode(child)) {
                collectCheckedDirs(child, result, false);
            }
        }
        return result;
    }

    private void collectCheckedDirs(CheckedTreeNode node, List<Path> result, boolean checkSelf) {
        if (checkSelf && node.getUserObject() instanceof ModelDirEntry entry && node.isChecked()) {
            result.add(entry.absolutePath());
        }
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            var child = children.nextElement();
            if (child instanceof CheckedTreeNode checkedChild) {
                collectCheckedDirs(checkedChild, result, true);
            }
        }
    }

    private void setDescendantsChecked(CheckedTreeNode node, boolean checked) {
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child) {
                child.setChecked(checked);
                setDescendantsChecked(child, checked);
            }
        }
        modelDirTree.repaint();
    }

    private void updateAncestorsChecked(CheckedTreeNode node) {
        var parent = node.getParent();
        if (!(parent instanceof CheckedTreeNode parentNode)) return;
        if (parentNode == treeRoot) return;

        boolean allChecked = true;
        Enumeration<?> siblings = parentNode.children();
        while (siblings.hasMoreElements()) {
            if (siblings.nextElement() instanceof CheckedTreeNode sibling) {
                if (!sibling.isChecked()) {
                    allChecked = false;
                    break;
                }
            }
        }
        parentNode.setChecked(allChecked);
        updateAncestorsChecked(parentNode);
    }

    private void runSearch() {
        generationState.activateSearch();
        var pattern = searchField.getText().trim();
        if (pattern.isEmpty()) return;

        LOG.info("runSearch: pattern='" + pattern + "'");

        var selectedLocal = getSelectedLocalDirs();
        var selectedDeps = getSelectedDependencyDirs();
        var searchDirs = new ArrayList<Path>();
        boolean noneSelected = selectedLocal.isEmpty() && selectedDeps.isEmpty();
        boolean useAllDirs = noneSelected && Ips2PlantSettings.getInstance().searchFallbackToAll;
        if (useAllDirs) {
            searchDirs.addAll(getAllLocalModelDirs());
            searchDirs.addAll(getAllDependencyDirs());
        } else {
            searchDirs.addAll(selectedLocal);
            searchDirs.addAll(selectedDeps);
        }

        LOG.info("runSearch: searching in " + searchDirs.size() + " directories");
        var dirsToSearch = List.copyOf(searchDirs);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, TASK_TITLE_SEARCH, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Searching for IPS classes...");
                var searcher = new IpsClassSearcher();
                var results = searcher.search(pattern, dirsToSearch);
                LOG.info("runSearch: found " + results.size() + " matching classes");
                ApplicationManager.getApplication().invokeLater(() -> updateSearchResults(results));
            }
        });
    }

    private void updateSearchResults(Map<String, File> results) {
        searchResults.clear();
        searchResults.putAll(results);
        searchResultsList.clear();
        if (results.isEmpty()) {
            LOG.info("Search returned no results");
            selectAllSearchButton.setVisible(false);
            searchResultsCardLayout.show(searchResultsCardPanel, CARD_NO_RESULTS);
        } else {
            LOG.info("Search returned " + results.size() + " results");
            for (var className : results.keySet()) {
                searchResultsList.addItem(className, className, true);
            }
            allSearchResultsSelected = true;
            selectAllSearchButton.setText(LABEL_DESELECT_ALL_SEARCH);
            selectAllSearchButton.setVisible(results.size() >= SELECT_ALL_SEARCH_THRESHOLD);
            var settings = Ips2PlantSettings.getInstance();
            if (settings.searchResetsOptions) {
                resetOptions(allOptionChecks);
                addSupertypesCheck.setSelected(false);
                addReferencingCheck.setSelected(false);
            }
            if (settings.searchSelectsAllClassTypes) {
                autoEnableOptionsForResults(results);
            }
            searchResultsCardLayout.show(searchResultsCardPanel, CARD_RESULTS);
            // Auto-generate PlantUML for the found classes
            runGeneration();
        }
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

    private void clearSearchResults() {
        searchResults.clear();
        searchResultsList.clear();
        selectAllSearchButton.setVisible(false);
        searchResultsCardLayout.show(searchResultsCardPanel, CARD_RESULTS);
    }

    private void resetOptions(JCheckBox[] allOptionChecks) {
        selectAllOptionsCheck.setSelected(false);
        for (var cb : allOptionChecks) {
            cb.setSelected(false);
        }
        showPolicyCheck.setSelected(true);
        packageFilterField.setText("");
        connectorLengthSpinner.setValue(2);
    }

    private void resetAll() {
        generationState.reset();
        // Uncheck all model dirs in the tree
        uncheckAllTreeNodes(treeRoot);
        modelDirTree.repaint();
        // Clear search
        searchField.setText("");
        clearSearchResults();
        addSupertypesCheck.setSelected(false);
        addReferencingCheck.setSelected(false);
        resetOptions(allOptionChecks);
        // Open empty diagram
        GeneratePlantUmlAction.openInEditor(project, EMPTY_PUML);
    }

    private void uncheckAllTreeNodes(CheckedTreeNode node) {
        node.setChecked(false);
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child) {
                uncheckAllTreeNodes(child);
            }
        }
    }

    private List<Path> getAllLocalModelDirs() {
        var result = new ArrayList<Path>();
        Enumeration<?> children = treeRoot.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child
                    && !isDependenciesNode(child)) {
                collectAllModelDirs(child, result);
            }
        }
        return result;
    }

    private List<Path> getAllDependencyDirs() {
        var result = new ArrayList<Path>();
        Enumeration<?> children = treeRoot.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child
                    && isDependenciesNode(child)) {
                collectAllModelDirs(child, result);
            }
        }
        return result;
    }

    private Map<String, File> getSelectedSearchResults() {
        var selected = new LinkedHashMap<String, File>();
        int count = searchResultsList.getModel().getSize();
        for (int i = 0; i < count; i++) {
            var className = searchResultsList.getItemAt(i);
            if (className != null && searchResultsList.isItemSelected(className)) {
                var file = searchResults.get(className);
                if (file != null) {
                    selected.put(className, file);
                }
            }
        }
        return selected;
    }

    private void runGeneration() {
        var selectedSearchFiles = getSelectedSearchResults();
        var options = getOptions();

        if (!searchResults.isEmpty()) {
            // Search is active — only generate from selected results, never fall back to directories
            if (selectedSearchFiles.isEmpty()) {
                LOG.info("runGeneration: search active but no results selected, clearing diagram");
                GeneratePlantUmlAction.openInEditor(project, EMPTY_PUML);
                return;
            }
            LOG.info("runGeneration: generating from " + selectedSearchFiles.size() + " search results");
            boolean expandSupertypes = addSupertypesCheck.isSelected();
            boolean expandReferencing = addReferencingCheck.isSelected();
            var allDirs = new ArrayList<>(getAllLocalModelDirs());
            allDirs.addAll(getAllDependencyDirs());
            // Generate from search results
            ProgressManager.getInstance().run(new Task.Backgroundable(project, TASK_TITLE, false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setText(TASK_STATUS_COLLECTING);
                    var ipsFiles = new LinkedHashMap<>(selectedSearchFiles);
                    if (expandSupertypes) {
                        indicator.setText("Adding supertypes...");
                        var searcher = new IpsClassSearcher();
                        ipsFiles = new LinkedHashMap<>(searcher.addSupertypes(ipsFiles, allDirs));
                    }
                    if (expandReferencing) {
                        indicator.setText("Adding referencing classes...");
                        var searcher = new IpsClassSearcher();
                        ipsFiles = new LinkedHashMap<>(searcher.addReferencingClasses(ipsFiles, allDirs));
                    }
                    var generator = new Ips2PlantGenerator();
                    Map<String, String> mavenModules = Map.of();
                    if (options.isShowMavenModule()) {
                        var resolver = new com.github.pbroman.ips2plant.core.MavenModuleResolver();
                        for (var depEntry : dependencyMavenModules.entrySet()) {
                            resolver.registerModule(depEntry.getKey(), depEntry.getValue());
                        }
                        mavenModules = resolver.resolveAll(ipsFiles);
                    }
                    var pumlContent = generator.generate(ipsFiles, options, indicator::setText, mavenModules);

                    ApplicationManager.getApplication().invokeLater(() ->
                            GeneratePlantUmlAction.openInEditor(project, pumlContent));
                }
            });
        } else {
            // Generate from selected model directories
            var localDirs = getSelectedLocalDirs();
            var depDirs = getSelectedDependencyDirs();
            if (localDirs.isEmpty() && depDirs.isEmpty()) {
                if (!Ips2PlantSettings.getInstance().generateFallbackToAll) {
                    LOG.info("runGeneration: no directories selected and generateAllWhenNoneSelected=false, skipping");
                    return;
                }
                localDirs = getAllLocalModelDirs();
                depDirs = getAllDependencyDirs();
                if (localDirs.isEmpty() && depDirs.isEmpty()) {
                    LOG.info("runGeneration: no directories available, skipping");
                    return;
                }
            }

            var finalLocalDirs = localDirs;
            var finalDepDirs = depDirs;
            var depModules = Map.copyOf(dependencyMavenModules);
            LOG.info("runGeneration: generating from " + localDirs.size() + " local dirs + " + depDirs.size() + " dependency dirs");
            ProgressManager.getInstance().run(new Task.Backgroundable(project, TASK_TITLE, false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setText(TASK_STATUS_COLLECTING);
                    var generator = new Ips2PlantGenerator();
                    var pumlContent = generator.generate(finalLocalDirs, finalDepDirs, options, indicator::setText, depModules);

                    ApplicationManager.getApplication().invokeLater(() ->
                            GeneratePlantUmlAction.openInEditor(project, pumlContent));
                }
            });
        }
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
        options.setDescriptionLocale(com.github.pbroman.ips2plant.settings.Ips2PlantSettings.getInstance().locale);
        options.setPackageFilter(packageFilterField.getText().trim());
        options.setConnectorLength((int) connectorLengthSpinner.getValue());
        return options;
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

    private void cleanupTempDirs() {
        for (var tempRoot : dependencyTempRoots) {
            try {
                if (tempRoot != null && tempRoot.toString().contains("ips2plant-dep-")) {
                    deleteRecursively(tempRoot);
                }
            } catch (IOException e) {
                // best effort cleanup
            }
        }
        dependencyTempRoots.clear();
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (var child : stream.toList()) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    private record ModelDirEntry(String displayName, Path absolutePath) {}
}
