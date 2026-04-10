package com.github.pbroman.ips2plant.core;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XsltProcessorTest {

    private final XsltProcessor processor = new XsltProcessor();

    @Test
    void transform_emptyCollection_producesStartEndUml() {
        // given
        var xml = "<collection/>";

        // when
        var result = processor.transform(xml, Map.of());

        // then
        assertThat(result).startsWith("@startuml")
                .endsWith("@enduml");
    }

    @Test
    void transform_singlePolicyCmptType_producesClassDiagram() {
        // given
        var xml = """
                <collection>
                    <PolicyCmptType className="com.example.Contract">
                        <Attribute name="premium" attributeType="changeable" datatype="Money"/>
                    </PolicyCmptType>
                </collection>
                """;

        // when
        var result = processor.transform(xml, Map.of("showPolicyComponents", "true"));

        // then
        assertThat(result).contains("@startuml")
                .contains("Contract")
                .contains("premium")
                .contains("@enduml");
    }

    @Test
    void transform_withAddAssociations_rendersCompositionArrow() {
        // given
        var xml = """
                <collection>
                    <PolicyCmptType className="com.A">
                        <Association target="com.B" targetRolePlural="bs"
                            associationType="comp" minCardinality="0" maxCardinality="*"/>
                    </PolicyCmptType>
                </collection>
                """;
        var params = Map.of("connector", "----", "addAssociations", "true", "showPolicyComponents", "true");

        // when
        var result = processor.transform(xml, params);

        // then
        assertThat(result).contains("*----")
                .contains("B");
    }

    @Test
    void transform_invalidXml_throwsException() {
        // given
        var invalidXml = "not xml";

        // when/then
        assertThatThrownBy(() -> processor.transform(invalidXml, Map.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("XSLT transformation failed");
    }

    @Test
    void transform_productCmptTypeWithShowProductComponents_includesProduct() {
        // given
        var xml = """
                <collection>
                    <ProductCmptType2 className="com.example.Product"/>
                </collection>
                """;
        var params = Map.of("showProductComponents", "true");

        // when
        var result = processor.transform(xml, params);

        // then
        assertThat(result).contains("Product");
    }

    @Test
    void transform_productCmptTypeWithoutFlag_excludesProduct() {
        // given
        var xml = """
                <collection>
                    <ProductCmptType2 className="com.example.Product"/>
                </collection>
                """;

        // when
        var result = processor.transform(xml, Map.of());

        // then
        assertThat(result).doesNotContain("Product");
    }
}