package com.github.pbroman.ips2plant.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Ips2PlantOptionsTest {

    @Test
    void toXsltParams_defaultOptions_containsOnlyConnectors() {
        // given
        var options = new Ips2PlantOptions();

        // when
        var params = options.toXsltParams();

        // then
        assertThat(params).containsEntry("connector", "--")
                .containsEntry("dottedConnector", "..")
                .hasSize(2);
    }

    @Test
    void toXsltParams_allBooleanOptionsEnabled_containsAllFlags() {
        // given
        var options = new Ips2PlantOptions();
        options.setPackages(true);
        options.setPrintTargetRole(true);
        options.setAddSuperType(true);
        options.setAddAssociations(true);
        options.setShowTables(true);
        options.setShowTableUsage(true);
        options.setShowEnumTypes(true);
        options.setShowEnumAssociations(true);
        options.setShowProductComponents(true);

        // when
        var params = options.toXsltParams();

        // then
        assertThat(params).containsEntry("packages", "true")
                .containsEntry("printTargetRole", "true")
                .containsEntry("addSuperType", "true")
                .containsEntry("addAssociations", "true")
                .containsEntry("showTables", "true")
                .containsEntry("showTableUsage", "true")
                .containsEntry("showEnumTypes", "true")
                .containsEntry("showEnumAssociations", "true")
                .containsEntry("showProductComponents", "true");
    }

    @Test
    void toXsltParams_customConnectorLength_repeatsCharacters() {
        // given
        var options = new Ips2PlantOptions();
        options.setConnectorLength(5);

        // when
        var params = options.toXsltParams();

        // then
        assertThat(params).containsEntry("connector", "-----")
                .containsEntry("dottedConnector", ".....");
    }

    @Test
    void toXsltParams_withPackageFilter_includesFilter() {
        // given
        var options = new Ips2PlantOptions();
        options.setPackageFilter("com.example.model");

        // when
        var params = options.toXsltParams();

        // then
        assertThat(params).containsEntry("packageFilter", "com.example.model");
    }

    @Test
    void toXsltParams_blankPackageFilter_excluded() {
        // given
        var options = new Ips2PlantOptions();
        options.setPackageFilter("   ");

        // when
        var params = options.toXsltParams();

        // then
        assertThat(params).doesNotContainKey("packageFilter");
    }

    @Test
    void toXsltParams_disabledBooleans_notIncluded() {
        // given
        var options = new Ips2PlantOptions();
        options.setPackages(false);
        options.setShowTables(false);

        // when
        var params = options.toXsltParams();

        // then
        assertThat(params).doesNotContainKey("packages")
                .doesNotContainKey("showTables");
    }
}