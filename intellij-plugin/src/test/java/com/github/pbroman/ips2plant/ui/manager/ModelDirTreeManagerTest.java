package com.github.pbroman.ips2plant.ui.manager;

import com.github.pbroman.ips2plant.ui.ModelDirEntry;
import com.intellij.ui.CheckedTreeNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the pure-logic, static methods in {@link ModelDirTreeManager}.
 *
 * The tree-manipulation methods (rebuildTree, addDependencyNodes, get*Dirs, …)
 * require a live IntelliJ application (CheckboxTree → TreeUIHelper → ApplicationManager)
 * and are therefore not covered here as unit tests.
 */
class ModelDirTreeManagerTest {

    // --- isDependenciesNode ---

    @Test
    void isDependenciesNode_withDepsLabel_returnsTrue() {
        var node = new CheckedTreeNode(ModelDirTreeManager.LABEL_DEPENDENCIES);
        assertThat(ModelDirTreeManager.isDependenciesNode(node)).isTrue();
    }

    @Test
    void isDependenciesNode_withCompactedDepsLabel_returnsTrue() {
        // compactTree merges single-child folder names with "/" — the prefix must still match
        var node = new CheckedTreeNode("dependencies/sub-folder");
        assertThat(ModelDirTreeManager.isDependenciesNode(node)).isTrue();
    }

    @Test
    void isDependenciesNode_withOtherString_returnsFalse() {
        var node = new CheckedTreeNode("some-module");
        assertThat(ModelDirTreeManager.isDependenciesNode(node)).isFalse();
    }

    @Test
    void isDependenciesNode_withStringThatContainsDepsButDoesNotStartWithIt_returnsFalse() {
        var node = new CheckedTreeNode("my-dependencies");
        assertThat(ModelDirTreeManager.isDependenciesNode(node)).isFalse();
    }

    @Test
    void isDependenciesNode_withModelDirEntry_returnsFalse() {
        var node = new CheckedTreeNode(new ModelDirEntry("model", Path.of("/some/path")));
        assertThat(ModelDirTreeManager.isDependenciesNode(node)).isFalse();
    }

    @Test
    void isDependenciesNode_withNullUserObject_returnsFalse() {
        var node = new CheckedTreeNode(null);
        assertThat(ModelDirTreeManager.isDependenciesNode(node)).isFalse();
    }
}
