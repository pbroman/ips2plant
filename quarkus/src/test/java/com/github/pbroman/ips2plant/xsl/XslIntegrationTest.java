package com.github.pbroman.ips2plant.xsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.pbroman.ips2plant.core.AbstractTest;
import com.github.pbroman.ips2plant.core.xslt.SaxonXsltProcessor;

public class XslIntegrationTest extends AbstractTest {

    @TempDir
    Path tempDir;

    private final SaxonXsltProcessor processor = new SaxonXsltProcessor();

    // -------------------------------------------------------------------------
    // Parameterized test
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void xsl_scenario(String scenarioName, Map<String, String> params) throws Exception {
        // given
        var collectionXml = getResourceFile("integration/collection.xml").toPath();
        var xsl = getResourceFile("xsl/ips2plant.xsl").toPath();
        var output = tempDir.resolve("result.puml");
        var expected = getResourceFile("integration/expected/" + scenarioName + ".puml").toPath();

        // when
        processor.process(xsl, collectionXml, output, params);

        // then
        assertThat(Files.readString(output))
                .isEqualToIgnoringNewLines(Files.readString(expected));
    }

    // -------------------------------------------------------------------------
    // Scenarios
    // -------------------------------------------------------------------------

    static Stream<Arguments> scenarios() {
        return Stream.of(
                scenario("01_default",
                        withBase()),

                scenario("02_with_products",
                        withBase("showProductComponents", "true")),

                scenario("03_with_packages",
                        withBase("packages", "true")),

                scenario("04_with_package_filter",
                        withBase("packageFilter", "policy")),

                scenario("05_package_filter_with_add_assoc",
                        withBase("packageFilter", "policy",
                                "addAssociations", "true")),

                scenario("06_with_enums",
                        withBase("showEnumTypes", "true")),

                scenario("07_with_enum_content",
                        withBase("showEnumTypes", "true",
                                "showEnumContent", "true")),

                scenario("08_with_enum_assoc",
                        withBase("showEnumTypes", "true",
                                "showEnumAssociations", "true")),

                scenario("09_with_tables",
                        withBase("showTables", "true")),

                scenario("10_with_table_usage",
                        withBase("showTables", "true",
                                "showTableUsage", "true")),

                scenario("11_with_super_type",
                        withBase("addSuperType", "true")),

                scenario("12_with_cardinalities",
                        withBase("showCardinalities", "true")),

                scenario("13_with_target_role",
                        withBase("printTargetRole", "true")),

                scenario("14_with_maven_module",
                        withBase("showMavenModule", "true")),

                scenario("15_with_descriptions",
                        withBase("showDescriptions", "true",
                                "descriptionLocale", "de")),

                scenario("16_with_descriptions_en",
                        withBase("showDescriptions", "true",
                                "descriptionLocale", "en")),

                scenario("17_no_policies",
                        params()),

                scenario("18_sort_alphabetically",
                        withBase("sortAttributesAlphabetically", "true")),

                scenario("19_connector_length_3",
                        withBase("connector", "---",
                                "dottedConnector", "...")),

                scenario("20_all_options",
                        params("showPolicyComponents", "true",
                                "showProductComponents", "true",
                                "packages", "true",
                                "showEnumTypes", "true",
                                "showEnumContent", "true",
                                "showEnumAssociations", "true",
                                "showTables", "true",
                                "showTableUsage", "true",
                                "addSuperType", "true",
                                "addAssociations", "true",
                                "showCardinalities", "true",
                                "printTargetRole", "true",
                                "showMavenModule", "true",
                                "showDescriptions", "true",
                                "descriptionLocale", "de",
                                "sortAttributesAlphabetically", "true"))
        );
    }

    // -------------------------------------------------------------------------
    // Snapshot regeneration (run manually after intentional behaviour changes)
    // -------------------------------------------------------------------------

    @Test
    @Disabled("Run manually to regenerate expected snapshots")
    void regenerate_snapshots() throws Exception {
        var collectionXml = getResourceFile("integration/collection.xml").toPath();
        var xsl = getResourceFile("xsl/ips2plant.xsl").toPath();
        var expectedDir = Path.of("src/test/resources/integration/expected");
        Files.createDirectories(expectedDir);

        scenarios().forEach(args -> {
            var scenarioName = (String) args.get()[0];
            @SuppressWarnings("unchecked")
            var scenarioParams = (Map<String, String>) args.get()[1];
            var output = expectedDir.resolve(scenarioName + ".puml");
            try {
                processor.process(xsl, collectionXml, output, scenarioParams);
                System.out.println("Generated: " + output);
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate snapshot for " + scenarioName, e);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Arguments scenario(String name, Map<String, String> params) {
        return Arguments.of(name, params);
    }

    /** Params starting from the default baseline (showPolicyComponents=true). */
    private static Map<String, String> withBase(String... keyValues) {
        var map = new HashMap<>(Map.of("showPolicyComponents", "true"));
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /** Params with no baseline (e.g. the no_policies scenario). */
    private static Map<String, String> params(String... keyValues) {
        var map = new HashMap<String, String>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

}
