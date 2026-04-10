package com.github.pbroman.ips2plant.ui.manager;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationManagerTest {

    // GenerationManager depends on IntelliJ platform services (ProgressManager, ApplicationManager)
    // and on SearchManager (which requires CheckBoxList). Full instantiation requires the IntelliJ
    // test framework. The tests below cover what is accessible without a running platform.

    @Test
    void emptyPuml_startsWithStartUml() {
        assertThat(GenerationManager.EMPTY_PUML).startsWith("@startuml");
    }

    @Test
    void emptyPuml_endsWithEndUml() {
        assertThat(GenerationManager.EMPTY_PUML).endsWith("@enduml\n");
    }

    @Test
    void emptyPuml_containsNothingToShow() {
        assertThat(GenerationManager.EMPTY_PUML).contains("Nothing to show");
    }
}
