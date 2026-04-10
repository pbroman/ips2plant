package com.github.pbroman.ips2plant.ui.manager;

import com.github.pbroman.ips2plant.core.IpsProjectDetector;
import com.github.pbroman.ips2plant.core.MavenDependencyCollector.DependencyModel;
import com.github.pbroman.ips2plant.ui.ModelDirEntry;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeListener;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ModelDirTreeManager {

    static final String LABEL_DEPENDENCIES = "dependencies";

    private static final String TITLE_MODEL_DIRECTORIES = "Model Directories";
    private static final String LABEL_NO_IPS_PROJECTS = "No .ipsproject files found.";

    private final Project project;
    private final CheckedTreeNode treeRoot;
    private final CheckboxTree modelDirTree;
    private boolean propagating;
    private Runnable onNodeStateChanged;

    public ModelDirTreeManager(Project project) {
        this.project = project;
        treeRoot = new CheckedTreeNode(TITLE_MODEL_DIRECTORIES);
        modelDirTree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(JTree tree, Object value, boolean selected,
                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof CheckedTreeNode node) {
                    var userObject = node.getUserObject();
                    if (userObject instanceof ModelDirEntry entry) {
                        getTextRenderer().append(entry.displayName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    } else if (userObject instanceof String text) {
                        getTextRenderer().append(text, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
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
                    setDescendantsChecked(node, node.isChecked());
                    updateAncestorsChecked(node);
                } finally {
                    propagating = false;
                }
                if (onNodeStateChanged != null) {
                    onNodeStateChanged.run();
                }
            }
        });
    }

    public void setOnNodeStateChanged(Runnable callback) {
        this.onNodeStateChanged = callback;
    }

    public CheckboxTree getTree() {
        return modelDirTree;
    }

    public void detectModelDirs() {
        var basePath = project.getBasePath();
        if (basePath == null) return;
        var detector = new IpsProjectDetector();
        var detected = detector.detectModelDirs(Path.of(basePath));
        rebuildTree(detected);
    }

    public void rebuildTree(List<Path> detected) {
        treeRoot.removeAllChildren();
        var basePath = project.getBasePath();
        var base = basePath != null ? Path.of(basePath) : null;
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

    /** Adds resolved dependency models to the tree under a "dependencies" node. */
    public void addDependencyNodes(List<DependencyModel> depModels) {
        removeDependenciesNode();
        var depsNode = new CheckedTreeNode(LABEL_DEPENDENCIES);
        for (var dep : depModels) {
            var artifactNode = findChildByName(depsNode, dep.artifactId());
            if (artifactNode == null) {
                artifactNode = new CheckedTreeNode(dep.artifactId());
                depsNode.add(artifactNode);
            }
            var entry = new ModelDirEntry("model", dep.modelDir());
            var leafNode = new CheckedTreeNode(entry);
            leafNode.setChecked(false);
            artifactNode.add(leafNode);
        }
        treeRoot.add(depsNode);
        compactTree(depsNode);
        reloadTree();
    }

    public void removeDependenciesNode() {
        Enumeration<?> children = treeRoot.children();
        CheckedTreeNode toRemove = null;
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child && isDependenciesNode(child)) {
                toRemove = child;
                break;
            }
        }
        if (toRemove != null) {
            treeRoot.remove(toRemove);
        }
    }

    public void uncheckAll() {
        uncheckAllNodes(treeRoot);
        modelDirTree.repaint();
    }

    public List<Path> getSelectedLocalDirs() {
        var result = new ArrayList<Path>();
        Enumeration<?> children = treeRoot.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child && !isDependenciesNode(child)) {
                collectCheckedDirs(child, result, false);
            }
        }
        return result;
    }

    public List<Path> getAllLocalDirs() {
        var result = new ArrayList<Path>();
        Enumeration<?> children = treeRoot.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child && !isDependenciesNode(child)) {
                collectAllModelDirs(child, result);
            }
        }
        return result;
    }

    public List<Path> getSelectedDependencyDirs() {
        var result = new ArrayList<Path>();
        Enumeration<?> children = treeRoot.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child && isDependenciesNode(child)) {
                collectCheckedDirs(child, result, false);
            }
        }
        return result;
    }

    public List<Path> getAllDependencyDirs() {
        var result = new ArrayList<Path>();
        Enumeration<?> children = treeRoot.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child && isDependenciesNode(child)) {
                collectAllModelDirs(child, result);
            }
        }
        return result;
    }

    static boolean isDependenciesNode(CheckedTreeNode node) {
        return node.getUserObject() instanceof String text
                && (text.equals(LABEL_DEPENDENCIES) || text.startsWith(LABEL_DEPENDENCIES + "/"));
    }

    private void reloadTree() {
        var treeModel = (DefaultTreeModel) modelDirTree.getModel();
        treeModel.setRoot(treeRoot);
        treeModel.reload();
        collapseAllNodes();
    }

    private void insertIntoTree(CheckedTreeNode parent, Path relativePath, Path absolutePath, boolean checked) {
        int nameCount = relativePath.getNameCount();
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
        var leafName = relativePath.getName(nameCount - 1).toString();
        var entry = new ModelDirEntry(leafName, absolutePath);
        var leafNode = new CheckedTreeNode(entry);
        leafNode.setChecked(checked);
        current.add(leafNode);
    }

    private void compactTree(CheckedTreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof CheckedTreeNode child) {
                compactTree(child);
            }
        }
        while (node != treeRoot
                && node.getChildCount() == 1
                && node.getChildAt(0) instanceof CheckedTreeNode onlyChild
                && onlyChild.getUserObject() instanceof String childName
                && node.getUserObject() instanceof String parentName) {
            node.setUserObject(parentName + "/" + childName);
            node.removeAllChildren();
            var toAdd = new ArrayList<CheckedTreeNode>();
            Enumeration<?> grandChildren = onlyChild.children();
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

    private CheckedTreeNode findChildByName(CheckedTreeNode parent, String name) {
        Enumeration<?> children = parent.children();
        while (children.hasMoreElements()) {
            var child = (CheckedTreeNode) children.nextElement();
            if (child.getUserObject() instanceof String text && text.equals(name)) {
                return child;
            }
        }
        return null;
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
            if (siblings.nextElement() instanceof CheckedTreeNode sibling && !sibling.isChecked()) {
                allChecked = false;
                break;
            }
        }
        parentNode.setChecked(allChecked);
        updateAncestorsChecked(parentNode);
    }

    private void uncheckAllNodes(CheckedTreeNode node) {
        node.setChecked(false);
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child) {
                uncheckAllNodes(child);
            }
        }
    }

    private void collectCheckedDirs(CheckedTreeNode node, List<Path> result, boolean checkSelf) {
        if (checkSelf && node.getUserObject() instanceof ModelDirEntry entry && node.isChecked()) {
            result.add(entry.absolutePath());
        }
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child) {
                collectCheckedDirs(child, result, true);
            }
        }
    }

    private void collectAllModelDirs(CheckedTreeNode node, List<Path> result) {
        if (node.getUserObject() instanceof ModelDirEntry entry) {
            result.add(entry.absolutePath());
        }
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            if (children.nextElement() instanceof CheckedTreeNode child) {
                collectAllModelDirs(child, result);
            }
        }
    }
}
