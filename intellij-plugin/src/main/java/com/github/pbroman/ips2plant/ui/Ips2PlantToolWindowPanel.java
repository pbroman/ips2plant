package com.github.pbroman.ips2plant.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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

import com.github.pbroman.ips2plant.action.GeneratePlantUmlAction;
import com.github.pbroman.ips2plant.core.Ips2PlantGenerator;
import com.github.pbroman.ips2plant.core.Ips2PlantOptions;
import com.github.pbroman.ips2plant.core.IpsProjectDetector;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeListener;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

public class Ips2PlantToolWindowPanel extends JPanel {

    private static final String TITLE_MODEL_DIRECTORIES = "Model Directories";
    private static final String TITLE_OPTIONS = "Options";
    private static final String LABEL_PACKAGES = "Packages";
    private static final String LABEL_PRINT_TARGET_ROLE = "Print target role";
    private static final String LABEL_EXTERNAL_SUPERTYPES = "External supertypes";
    private static final String LABEL_EXTERNAL_ASSOCIATIONS = "External associations";
    private static final String LABEL_SHOW_TABLES = "Show tables";
    private static final String LABEL_SHOW_TABLE_USAGE = "Show table usage";
    private static final String LABEL_SHOW_ENUM_TYPES = "Show enum types";
    private static final String LABEL_SHOW_ENUM_ASSOCIATIONS = "Show enum associations";
    private static final String LABEL_SHOW_PRODUCT_COMPONENTS = "Show product components";
    private static final String LABEL_PACKAGE_FILTER = "Package filter:";
    private static final String LABEL_CONNECTOR_LENGTH = "Connector length:";
    private static final String LABEL_GENERATE = "Generate PlantUML";
    private static final String LABEL_NO_IPS_PROJECTS = "No .ipsproject files found.";
    private static final String TASK_TITLE = "Generating PlantUML from IPS Models";
    private static final String TASK_STATUS_COLLECTING = "Collecting IPS files...";

    private static final String TOOLTIP_PACKAGES = "Displays classes in packages";
    private static final String TOOLTIP_PRINT_TARGET_ROLE = "Print the targetRolePlural attribute on the composition arrow";
    private static final String TOOLTIP_EXTERNAL_SUPERTYPES = "Adds inheritance of super types that are NOT present under the scanned models";
    private static final String TOOLTIP_EXTERNAL_ASSOCIATIONS = "Adds associations to classes that are NOT present under the scanned models";
    private static final String TOOLTIP_SHOW_TABLES = "Show tables";
    private static final String TOOLTIP_SHOW_TABLE_USAGE = "Show table usage by product component types (including external tables)";
    private static final String TOOLTIP_SHOW_ENUM_TYPES = "Show enum types";
    private static final String TOOLTIP_SHOW_ENUM_ASSOCIATIONS = "Show enum associations (including external enums)";
    private static final String TOOLTIP_SHOW_PRODUCT_COMPONENTS = "Show product components";
    private static final String TOOLTIP_PACKAGE_FILTER = "Filter the diagram to a package and its associations";
    private static final String TOOLTIP_CONNECTOR_LENGTH = "Length of association connectors";
    private static final String TOOLTIP_GENERATE = "Generate PlantUML class diagram from selected IPS model directories";

    private static final String LABEL_SELECT_ALL_OPTIONS = "Select all";

    private final Project project;

    // Model directory selection
    private CheckboxTree modelDirTree;
    private CheckedTreeNode treeRoot;
    private boolean propagating;
    // Options
    private final JCheckBox selectAllOptionsCheck = new JCheckBox(LABEL_SELECT_ALL_OPTIONS);
    private final JCheckBox packagesCheck = withTooltip(new JCheckBox(LABEL_PACKAGES), TOOLTIP_PACKAGES);
    private final JCheckBox printTargetRoleCheck = withTooltip(new JCheckBox(LABEL_PRINT_TARGET_ROLE), TOOLTIP_PRINT_TARGET_ROLE);
    private final JCheckBox addSuperTypeCheck = withTooltip(new JCheckBox(LABEL_EXTERNAL_SUPERTYPES), TOOLTIP_EXTERNAL_SUPERTYPES);
    private final JCheckBox addAssociationsCheck = withTooltip(new JCheckBox(LABEL_EXTERNAL_ASSOCIATIONS), TOOLTIP_EXTERNAL_ASSOCIATIONS);
    private final JCheckBox showTablesCheck = withTooltip(new JCheckBox(LABEL_SHOW_TABLES), TOOLTIP_SHOW_TABLES);
    private final JCheckBox showTableUsageCheck = withTooltip(new JCheckBox(LABEL_SHOW_TABLE_USAGE), TOOLTIP_SHOW_TABLE_USAGE);
    private final JCheckBox showEnumTypesCheck = withTooltip(new JCheckBox(LABEL_SHOW_ENUM_TYPES), TOOLTIP_SHOW_ENUM_TYPES);
    private final JCheckBox showEnumAssocCheck = withTooltip(new JCheckBox(LABEL_SHOW_ENUM_ASSOCIATIONS), TOOLTIP_SHOW_ENUM_ASSOCIATIONS);
    private final JCheckBox showProductCheck = withTooltip(new JCheckBox(LABEL_SHOW_PRODUCT_COMPONENTS), TOOLTIP_SHOW_PRODUCT_COMPONENTS);
    private final JTextField packageFilterField = withTooltip(new JTextField(15), TOOLTIP_PACKAGE_FILTER);
    private final JSpinner connectorLengthSpinner = withTooltip(new JSpinner(new SpinnerNumberModel(2, 1, 10, 1)), TOOLTIP_CONNECTOR_LENGTH);

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
            }
        });

        var treeScrollPane = new JScrollPane(modelDirTree);
        treeScrollPane.setBorder(null);
        modelSection.add(treeScrollPane, BorderLayout.CENTER);
        modelSection.setMinimumSize(new Dimension(0, 80));

        // --- Center: Options ---
        var optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(TITLE_OPTIONS));
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        JCheckBox[] allOptionChecks = { packagesCheck, printTargetRoleCheck, addSuperTypeCheck, addAssociationsCheck, showProductCheck,
                showTablesCheck, showTableUsageCheck, showEnumTypesCheck, showEnumAssocCheck };

        selectAllOptionsCheck.addActionListener(e -> {
            boolean selected = selectAllOptionsCheck.isSelected();
            for (var cb : allOptionChecks) {
                cb.setSelected(selected);
            }
        });
        for (var cb : allOptionChecks) {
            cb.addActionListener(e -> updateSelectAllState(allOptionChecks));
        }

        gbc.gridy = row++;
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1.0;
        optionsPanel.add(selectAllOptionsCheck, gbc);
        gbc.gridwidth = 1;

        JCheckBox[] leftColumn = { packagesCheck, printTargetRoleCheck, addSuperTypeCheck, addAssociationsCheck, showProductCheck };
        JCheckBox[] rightColumn = { showTablesCheck, showTableUsageCheck, showEnumTypesCheck, showEnumAssocCheck };

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

        // Wrap options in a wrapper that pushes content to top, then wrap in scroll pane
        var optionsWrapper = new JPanel(new BorderLayout());
        optionsWrapper.add(optionsPanel, BorderLayout.NORTH);
        var optionsScrollPane = new JScrollPane(optionsWrapper);
        optionsScrollPane.setBorder(null);

        var optionsSection = new JPanel(new BorderLayout());
        optionsSection.setBorder(BorderFactory.createTitledBorder(TITLE_OPTIONS));
        optionsSection.add(optionsScrollPane, BorderLayout.CENTER);
        optionsSection.setMinimumSize(new Dimension(0, 80));

        // Remove the border from the inner optionsPanel since the section has one
        optionsPanel.setBorder(null);

        // --- Split pane: model dirs (top) + options (bottom) ---
        var splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, modelSection, optionsSection);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(6);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);

        // --- Bottom: Generate button ---
        var bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        var generateButton = new JButton(LABEL_GENERATE);
        generateButton.setToolTipText(TOOLTIP_GENERATE);
        generateButton.addActionListener(e -> runGeneration());
        bottomPanel.add(generateButton);
        add(bottomPanel, BorderLayout.SOUTH);

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

        // Package filter: debounce on text changes
        packageFilterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { scheduleRegeneration(); }
            @Override
            public void removeUpdate(DocumentEvent e) { scheduleRegeneration(); }
            @Override
            public void changedUpdate(DocumentEvent e) { scheduleRegeneration(); }
        });

        // Connector length spinner: regenerate on value change
        connectorLengthSpinner.addChangeListener(e -> scheduleRegeneration());

    }

    private void scheduleRegeneration() {
        regenerateTimer.restart();
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

        var treeModel = (javax.swing.tree.DefaultTreeModel) modelDirTree.getModel();
        treeModel.setRoot(treeRoot);
        treeModel.reload();
        collapseAllNodes();
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
        while (node.getChildCount() == 1
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

    private List<Path> getSelectedDirs() {
        var result = new ArrayList<Path>();
        collectCheckedDirs(treeRoot, result);
        return result;
    }

    private void collectCheckedDirs(CheckedTreeNode node, List<Path> result) {
        if (node.getUserObject() instanceof ModelDirEntry entry && node.isChecked()) {
            result.add(entry.absolutePath());
        }
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            var child = children.nextElement();
            if (child instanceof CheckedTreeNode checkedChild) {
                collectCheckedDirs(checkedChild, result);
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

    private void runGeneration() {
        var dirs = getSelectedDirs();
        if (dirs.isEmpty()) return;
        var options = getOptions();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, TASK_TITLE, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText(TASK_STATUS_COLLECTING);
                var generator = new Ips2PlantGenerator();
                var pumlContent = generator.generate(dirs, options);

                ApplicationManager.getApplication().invokeLater(() ->
                        GeneratePlantUmlAction.openInEditor(project, pumlContent));
            }
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
        options.setShowEnumAssociations(showEnumAssocCheck.isSelected());
        options.setShowProductComponents(showProductCheck.isSelected());
        options.setPackageFilter(packageFilterField.getText().trim());
        options.setConnectorLength((int) connectorLengthSpinner.getValue());
        return options;
    }

    private void updateSelectAllState(JCheckBox[] checks) {
        boolean allSelected = true;
        for (var cb : checks) {
            if (!cb.isSelected()) {
                allSelected = false;
                break;
            }
        }
        selectAllOptionsCheck.setSelected(allSelected);
    }

    private record ModelDirEntry(String displayName, Path absolutePath) {}
}