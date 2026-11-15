package com.github.pbroman.ips2plant.ui.manager;

import com.github.pbroman.ips2plant.core.IpsModelIndex;
import com.github.pbroman.ips2plant.core.IpsTreeWalker;
import com.github.pbroman.ips2plant.core.ModuleClasspath;
import com.github.pbroman.ips2plant.settings.Ips2PlantSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * "Tree" tab: descendants / ancestors of one root policy type, fed into the generation flow.
 */
public class TreeSearchManager {

    private static final Logger LOG = Logger.getInstance(TreeSearchManager.class);
    private static final String TASK_TITLE = "Walking IPS Class Tree";
    private static final String TOOLTIP_ROOT = "Exact simple or fully qualified name of a policy component type";
    private static final String TOOLTIP_DEPTH = "Maximum number of association hops; empty = unlimited";
    private static final String TOOLTIP_INCLUDE_ABSTRACT = "Include paths ending in abstract classes";
    private static final Color ERROR_COLOR = new Color(200, 0, 0);

    private final Project project;
    private final ModelDirTreeManager treeManager;
    private final DependencyManager depManager;
    private final GenerationStateManager generationState;

    private final JTextField rootField = new JTextField(15);
    private final JCheckBox descendantsCheck = new JCheckBox("Descendants", true);
    private final JCheckBox ancestorsCheck = new JCheckBox("Ancestors");
    private final JCheckBox includeAbstractCheck = new JCheckBox("Include abstract");
    private final JCheckBox mergeSupertypesCheck = new JCheckBox("Merge supertypes", true);
    private final JTextField depthField = new JTextField(3);
    private final JTextArea statusLabel = new JTextArea(3, 20);
    private final JPanel panel = new JPanel(new BorderLayout(0, 2));

    private final Map<String, File> results = new LinkedHashMap<>();
    private volatile String resultRoot = "";

    private Runnable onResultsUpdated;
    private Runnable onBeforeRun;
    private Consumer<Map<String, File>> onOptionsAutoEnabled;

    public TreeSearchManager(Project project, ModelDirTreeManager treeManager, DependencyManager depManager,
                             GenerationStateManager generationState) {
        this.project = project;
        this.treeManager = treeManager;
        this.depManager = depManager;
        this.generationState = generationState;
        buildPanel();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setOnResultsUpdated(Runnable callback) {
        this.onResultsUpdated = callback;
    }

    public void setOnBeforeRun(Runnable callback) {
        this.onBeforeRun = callback;
    }

    public void setOnOptionsAutoEnabled(Consumer<Map<String, File>> callback) {
        this.onOptionsAutoEnabled = callback;
    }

    public boolean isMergeSupertypes() {
        return mergeSupertypesCheck.isSelected();
    }

    /** Fully qualified name of the root of the current results, empty if there are none. */
    public String getRoot() {
        return resultRoot;
    }

    public Map<String, File> getResults() {
        return results;
    }

    public Map<String, File> getSelectedResults() {
        return new LinkedHashMap<>(results);
    }

    public void clear() {
        results.clear();
        showStatus("", false);
    }

    public void run() {
        var rootName = rootField.getText().trim();
        if (rootName.isEmpty()) {
            return;
        }
        if (!descendantsCheck.isSelected() && !ancestorsCheck.isSelected()) {
            showStatus("Select Descendants and/or Ancestors", true);
            return;
        }
        Integer depth;
        try {
            depth = parseDepth(depthField.getText());
        } catch (NumberFormatException e) {
            showStatus("Depth must be a positive number or empty", true);
            return;
        }

        generationState.activateTree();
        if (onBeforeRun != null) onBeforeRun.run();
        var selectedLocal = treeManager.getSelectedLocalDirs();
        var selectedDeps = treeManager.getSelectedDependencyDirs();
        var scopeDirs = new ArrayList<Path>();
        if (selectedLocal.isEmpty() && selectedDeps.isEmpty() && Ips2PlantSettings.getInstance().searchFallbackToAll) {
            scopeDirs.addAll(treeManager.getAllLocalDirs());
            scopeDirs.addAll(treeManager.getAllDependencyDirs());
        } else {
            scopeDirs.addAll(selectedLocal);
            scopeDirs.addAll(selectedDeps);
        }
        var allDirs = new ArrayList<Path>(treeManager.getAllLocalDirs());
        allDirs.addAll(treeManager.getAllDependencyDirs());
        scopeDirs.stream().filter(d -> !allDirs.contains(d)).forEach(allDirs::add);

        var localDirs = List.copyOf(treeManager.getAllLocalDirs());
        var depModules = depManager.getDependencyMavenModules();
        boolean descendants = descendantsCheck.isSelected();
        boolean ancestors = ancestorsCheck.isSelected();
        boolean includeAbstract = includeAbstractCheck.isSelected();
        boolean merge = mergeSupertypesCheck.isSelected();
        Integer finalDepth = depth;
        LOG.info("tree run: root='" + rootName + "' desc=" + descendants + " anc=" + ancestors
                + " depth=" + depth + " abstract=" + includeAbstract + " scopeDirs=" + scopeDirs.size());

        ProgressManager.getInstance().run(new Task.Backgroundable(project, TASK_TITLE, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                resultRoot = "";
                indicator.setText("Indexing policy component types...");
                var fullIndex = IpsModelIndex.build(allDirs, scopeDirs);
                var root = fullIndex.resolve(rootName);
                if (root.isEmpty()) {
                    finish(Map.of(), "Class not found or ambiguous (use the fully qualified name): " + rootName, true);
                    return;
                }
                if (!fullIndex.isInScope(root.get())) {
                    finish(Map.of(), root.get() + " is not in the selected model directories", true);
                    return;
                }
                var rootFile = fullIndex.get(root.get()).orElseThrow().file().toPath().toAbsolutePath();
                var rootDir = allDirs.stream().filter(d -> rootFile.startsWith(d.toAbsolutePath())).findFirst();
                var visible = rootDir.flatMap(d -> ModuleClasspath.visibleDirs(d, localDirs, depModules));
                String classpathNote = visible.isPresent() ? ""
                        : " (no pom.xml found for root: classpath not applied)";
                final IpsModelIndex index = visible.map(visibleDirs -> {
                    var restricted = scopeDirs.stream().filter(visibleDirs::contains).toList();
                    LOG.info("tree run: classpath of root has " + visibleDirs.size() + " model dirs, "
                            + restricted.size() + " in scope");
                    return IpsModelIndex.build(allDirs, restricted);
                }).orElse(fullIndex);
                var walked = new IpsTreeWalker(index).walk(root.get(), descendants, ancestors, finalDepth, includeAbstract, merge);
                var unresolved = walked.keySet().stream()
                        .flatMap(fqn -> index.get(fqn).map(IpsModelIndex.ClassInfo::supertype).stream())
                        .filter(s -> index.get(s).isEmpty())
                        .distinct()
                        .toList();
                String message = walked.size() + " classes" + classpathNote;
                if (!unresolved.isEmpty()) {
                    LOG.info("tree run: unresolved supertypes " + unresolved);
                    message += " - unresolved supertypes (resolve dependencies?): " + String.join(", ", unresolved);
                }
                resultRoot = root.get();
                finish(walked, message, !unresolved.isEmpty());
            }
        });
    }

    private void finish(Map<String, File> walked, String message, boolean warn) {
        ApplicationManager.getApplication().invokeLater(() -> {
            results.clear();
            results.putAll(walked);
            showStatus(message, warn);
            if (walked.isEmpty()) {
                return;
            }
            if (Ips2PlantSettings.getInstance().searchSelectsAllClassTypes && onOptionsAutoEnabled != null) {
                onOptionsAutoEnabled.accept(walked);
            }
            if (onResultsUpdated != null) {
                onResultsUpdated.run();
            }
        });
    }

    static Integer parseDepth(String text) {
        var trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int value = Integer.parseInt(trimmed);
        if (value < 1) {
            throw new NumberFormatException("depth must be >= 1");
        }
        return value;
    }

    private void showStatus(String message, boolean warn) {
        statusLabel.setText(message);
        statusLabel.setForeground(warn ? ERROR_COLOR : javax.swing.UIManager.getColor("Label.foreground"));
    }

    private void buildPanel() {
        rootField.setToolTipText(TOOLTIP_ROOT);
        depthField.setToolTipText(TOOLTIP_DEPTH);
        includeAbstractCheck.setToolTipText(TOOLTIP_INCLUDE_ABSTRACT);
        mergeSupertypesCheck.setToolTipText("Show inherited attributes and associations inside each class (below a dotted line, grey arrows) instead of separate supertype classes");
        descendantsCheck.setToolTipText("Add everything reachable from the root through associations");
        ancestorsCheck.setToolTipText("Add everything that reaches the root through associations");
        statusLabel.setEditable(false);
        statusLabel.setLineWrap(true);
        statusLabel.setWrapStyleWord(true);
        statusLabel.setOpaque(false);

        var input = new JPanel(new BorderLayout(4, 0));
        input.add(new JLabel("Root:"), BorderLayout.WEST);
        input.add(rootField, BorderLayout.CENTER);
        var findButton = new JButton("Build");
        findButton.setToolTipText("Collect descendants/ancestors of the root class");
        findButton.addActionListener(e -> run());
        rootField.addActionListener(e -> run());
        input.add(findButton, BorderLayout.EAST);
        panel.add(input, BorderLayout.NORTH);

        var scroll = new JScrollPane(statusLabel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        panel.add(scroll, BorderLayout.CENTER);

        var bottom = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.5;
        bottom.add(descendantsCheck, gbc);
        gbc.gridx = 1;
        bottom.add(ancestorsCheck, gbc);
        var depthRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        depthRow.add(new JLabel("Depth:"));
        depthRow.add(depthField);
        gbc.gridy = 1; gbc.gridx = 0; gbc.gridwidth = 2;
        bottom.add(depthRow, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 2; gbc.gridx = 0;
        bottom.add(mergeSupertypesCheck, gbc);
        gbc.gridx = 1;
        bottom.add(includeAbstractCheck, gbc);
        panel.add(bottom, BorderLayout.SOUTH);

        for (var cb : new JCheckBox[]{descendantsCheck, ancestorsCheck, includeAbstractCheck, mergeSupertypesCheck}) {
            cb.addActionListener(e -> rerunIfActive());
        }
        depthField.addActionListener(e -> run());
    }

    private void rerunIfActive() {
        if (generationState.isTreeActive()) {
            run();
        }
    }
}
