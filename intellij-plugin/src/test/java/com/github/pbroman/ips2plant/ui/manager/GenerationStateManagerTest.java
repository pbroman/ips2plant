package com.github.pbroman.ips2plant.ui.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationStateManagerTest {

    private GenerationStateManager state;

    @BeforeEach
    void setUp() {
        state = new GenerationStateManager();
    }

    // --- initial state ---

    @Test
    void initialState_bothFlagsAreFalse() {
        assertThat(state.isModelGenerationActive()).isFalse();
        assertThat(state.isSearchActive()).isFalse();
    }

    @Test
    void initialState_shouldRegenerateIsFalse() {
        assertThat(state.shouldRegenerate()).isFalse();
    }

    // --- activateModelGeneration ---

    @Test
    void activateModelGeneration_setsModelGenerationActive() {
        // when
        state.activateModelGeneration();

        // then
        assertThat(state.isModelGenerationActive()).isTrue();
        assertThat(state.isSearchActive()).isFalse();
    }

    @Test
    void activateModelGeneration_shouldRegenerateIsTrue() {
        // when
        state.activateModelGeneration();

        // then
        assertThat(state.shouldRegenerate()).isTrue();
    }

    @Test
    void activateModelGeneration_clearsSearchActive() {
        // given
        state.activateSearch();

        // when
        state.activateModelGeneration();

        // then
        assertThat(state.isSearchActive()).isFalse();
        assertThat(state.isModelGenerationActive()).isTrue();
    }

    // --- activateSearch ---

    @Test
    void activateSearch_setsSearchActive() {
        // when
        state.activateSearch();

        // then
        assertThat(state.isSearchActive()).isTrue();
        assertThat(state.isModelGenerationActive()).isFalse();
    }

    @Test
    void activateSearch_shouldRegenerateIsTrue() {
        // when
        state.activateSearch();

        // then
        assertThat(state.shouldRegenerate()).isTrue();
    }

    @Test
    void activateSearch_clearsModelGenerationActive() {
        // given
        state.activateModelGeneration();

        // when
        state.activateSearch();

        // then
        assertThat(state.isModelGenerationActive()).isFalse();
        assertThat(state.isSearchActive()).isTrue();
    }

    // --- mutual exclusivity ---

    @Test
    void bothFlagsAreNeverTrueAtTheSameTime_afterActivateModelGeneration() {
        // given
        state.activateSearch();

        // when
        state.activateModelGeneration();

        // then
        assertThat(state.isSearchActive()).isFalse();
        assertThat(state.isModelGenerationActive()).isTrue();
    }

    @Test
    void bothFlagsAreNeverTrueAtTheSameTime_afterActivateSearch() {
        // given
        state.activateModelGeneration();

        // when
        state.activateSearch();

        // then
        assertThat(state.isModelGenerationActive()).isFalse();
        assertThat(state.isSearchActive()).isTrue();
    }

    // --- deactivateSearch ---

    @Test
    void deactivateSearch_clearsSearchActive() {
        // given
        state.activateSearch();

        // when
        state.deactivateSearch();

        // then
        assertThat(state.isSearchActive()).isFalse();
    }

    @Test
    void deactivateSearch_clearsModelGenerationActive() {
        // given
        state.activateModelGeneration();

        // when
        state.deactivateSearch();

        // then
        assertThat(state.isModelGenerationActive()).isFalse();
    }

    @Test
    void deactivateSearch_shouldRegenerateIsFalse_afterModelGeneration() {
        // given
        state.activateModelGeneration();

        // when
        state.deactivateSearch();

        // then
        assertThat(state.shouldRegenerate()).isFalse();
    }

    @Test
    void deactivateSearch_shouldRegenerateIsFalse_afterSearch() {
        // given
        state.activateSearch();

        // when
        state.deactivateSearch();

        // then
        assertThat(state.shouldRegenerate()).isFalse();
    }

    // --- reset ---

    @Test
    void reset_clearsBothFlags_whenModelGenerationActive() {
        // given
        state.activateModelGeneration();

        // when
        state.reset();

        // then
        assertThat(state.isModelGenerationActive()).isFalse();
        assertThat(state.isSearchActive()).isFalse();
    }

    @Test
    void reset_clearsBothFlags_whenSearchActive() {
        // given
        state.activateSearch();

        // when
        state.reset();

        // then
        assertThat(state.isModelGenerationActive()).isFalse();
        assertThat(state.isSearchActive()).isFalse();
    }

    @Test
    void reset_shouldRegenerateIsFalse() {
        // given
        state.activateModelGeneration();

        // when
        state.reset();

        // then
        assertThat(state.shouldRegenerate()).isFalse();
    }

    // --- shouldRegenerate ---

    @Test
    void shouldRegenerate_trueOnlyWhenAtLeastOneFlagIsActive() {
        assertThat(state.shouldRegenerate()).isFalse();

        state.activateModelGeneration();
        assertThat(state.shouldRegenerate()).isTrue();

        state.reset();
        assertThat(state.shouldRegenerate()).isFalse();

        state.activateSearch();
        assertThat(state.shouldRegenerate()).isTrue();

        state.reset();
        assertThat(state.shouldRegenerate()).isFalse();
    }
}
