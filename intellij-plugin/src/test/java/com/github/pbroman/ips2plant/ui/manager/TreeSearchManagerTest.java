package com.github.pbroman.ips2plant.ui.manager;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TreeSearchManagerTest {

    @Test
    void parseDepth_blankMeansUnlimited() {
        // when / then
        assertThat(TreeSearchManager.parseDepth("")).isNull();
        assertThat(TreeSearchManager.parseDepth("  ")).isNull();
        assertThat(TreeSearchManager.parseDepth(null)).isNull();
    }

    @Test
    void parseDepth_positiveNumberIsParsed() {
        // when / then
        assertThat(TreeSearchManager.parseDepth(" 3 ")).isEqualTo(3);
    }

    @Test
    void parseDepth_zeroNegativeOrNonNumericIsRejected() {
        // when / then
        assertThatThrownBy(() -> TreeSearchManager.parseDepth("0")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> TreeSearchManager.parseDepth("-1")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> TreeSearchManager.parseDepth("abc")).isInstanceOf(NumberFormatException.class);
    }
}
