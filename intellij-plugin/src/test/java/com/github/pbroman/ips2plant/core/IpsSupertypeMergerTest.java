package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;

import static org.assertj.core.api.Assertions.assertThat;

class IpsSupertypeMergerTest {

    @TempDir
    Path tempDir;

    private File write(String fqn, String attrs, String... children) throws IOException {
        var file = tempDir.resolve("src").resolve(fqn + ".ipspolicycmpttype");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "<PolicyCmptType " + attrs + ">" + String.join("", children) + "</PolicyCmptType>");
        return file.toFile();
    }

    private static String attr(String name) {
        return "<Attribute name=\"" + name + "\" datatype=\"String\"/>";
    }

    private static String assoc(String type, String role, String target) {
        return "<Association associationType=\"" + type + "\" targetRoleSingular=\"" + role + "\" target=\"" + target + "\"/>";
    }

    private Element mergedRoot(File merged) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(merged).getDocumentElement();
    }

    private Map<String, File> merge(Map<String, File> files, Map<String, File> all) throws IOException {
        return new IpsSupertypeMerger().merge(files, all, Files.createDirectories(tempDir.resolve("out")));
    }

    @Test
    void merge_appendsInheritedAttributesAndAssociationsMarkedAsInherited() throws Exception {
        // given
        var top = write("Top", "", attr("topAttr"), assoc("comp", "Limit", "Limit"));
        var mid = write("Mid", "supertype=\"Top\"", attr("midAttr"));
        var leaf = write("Leaf", "supertype=\"Mid\"", attr("leafAttr"));
        var all = Map.of("Top", top, "Mid", mid, "Leaf", leaf);

        // when
        var merged = merge(Map.of("Leaf", leaf), all);

        // then
        var root = mergedRoot(merged.get("Leaf"));
        var attributes = root.getElementsByTagName("Attribute");
        assertThat(attributes.getLength()).isEqualTo(3);
        assertThat(((Element) attributes.item(0)).getAttribute("name")).isEqualTo("leafAttr");
        assertThat(((Element) attributes.item(0)).hasAttribute("inherited")).isFalse();
        assertThat(((Element) attributes.item(1)).getAttribute("name")).isEqualTo("midAttr");
        assertThat(((Element) attributes.item(1)).getAttribute("inherited")).isEqualTo("true");
        assertThat(((Element) attributes.item(2)).getAttribute("name")).isEqualTo("topAttr");
        var association = (Element) root.getElementsByTagName("Association").item(0);
        assertThat(association.getAttribute("inherited")).isEqualTo("true");
        assertThat(association.getAttribute("target")).isEqualTo("Limit");
    }

    @Test
    void merge_ownDeclarationWinsOverInheritedOnes() throws Exception {
        // given
        var base = write("Base", "", attr("name"), assoc("comp", "Variant", "BaseVariant"));
        var child = write("Child", "supertype=\"Base\"", attr("name"), assoc("comp", "Variant", "SpecialVariant"));

        // when
        var merged = merge(Map.of("Child", child), Map.of("Base", base, "Child", child));

        // then
        var root = mergedRoot(merged.get("Child"));
        assertThat(root.getElementsByTagName("Attribute").getLength()).isEqualTo(1);
        assertThat(root.getElementsByTagName("Association").getLength()).isEqualTo(1);
        assertThat(((Element) root.getElementsByTagName("Association").item(0)).getAttribute("target"))
                .isEqualTo("SpecialVariant");
    }

    @Test
    void merge_stopsAtUnknownSupertypeAndCycles() throws Exception {
        // given
        var a = write("A", "supertype=\"B\"", attr("a"));
        var b = write("B", "supertype=\"A\"", attr("b"));
        var c = write("C", "supertype=\"ext.Missing\"", attr("c"));

        // when
        var merged = merge(new LinkedHashMap<>(Map.of("A", a, "C", c)), Map.of("A", a, "B", b, "C", c));

        // then
        assertThat(mergedRoot(merged.get("A")).getElementsByTagName("Attribute").getLength()).isEqualTo(2);
        assertThat(mergedRoot(merged.get("C")).getElementsByTagName("Attribute").getLength()).isEqualTo(1);
    }

    @Test
    void merge_passesNonPolicyFilesThrough() throws Exception {
        // given
        var product = tempDir.resolve("Prod.ipsproductcmpttype");
        Files.writeString(product, "<ProductCmptType2/>");

        // when
        var merged = merge(Map.of("Prod", product.toFile()), Map.of());

        // then
        assertThat(merged.get("Prod")).isEqualTo(product.toFile());
    }

    @Test
    void merge_retargetsAssociationToUnselectedAbstractClassOntoSelectedConcreteSubtypes() throws Exception {
        // given
        var abstractDeductible = write("ShdDeductible", "abstract=\"true\"");
        var deductible = write("LiaDeductible", "supertype=\"ShdDeductible\"");
        var shdAgreement = write("ShdAgreement", "", assoc("comp", "Deductible", "ShdDeductible"));
        var agreement = write("LiaAgreement", "supertype=\"ShdAgreement\"");
        var all = Map.of("ShdDeductible", abstractDeductible, "LiaDeductible", deductible,
                "ShdAgreement", shdAgreement, "LiaAgreement", agreement);

        // when
        var merged = merge(Map.of("LiaAgreement", agreement, "LiaDeductible", deductible), all);

        // then
        var associations = mergedRoot(merged.get("LiaAgreement")).getElementsByTagName("Association");
        assertThat(associations.getLength()).isEqualTo(1);
        var association = (Element) associations.item(0);
        assertThat(association.getAttribute("target")).isEqualTo("LiaDeductible");
        assertThat(association.getAttribute("inherited")).isEqualTo("true");
    }

    @Test
    void merge_keepsAssociationToSelectedAbstractClass() throws Exception {
        // given
        var abstractDeductible = write("ShdDeductible", "abstract=\"true\"");
        var deductible = write("LiaDeductible", "supertype=\"ShdDeductible\"");
        var agreement = write("LiaAgreement", "", assoc("comp", "Deductible", "ShdDeductible"));
        var all = Map.of("ShdDeductible", abstractDeductible, "LiaDeductible", deductible, "LiaAgreement", agreement);

        // when
        var merged = merge(Map.of("LiaAgreement", agreement, "LiaDeductible", deductible,
                "ShdDeductible", abstractDeductible), all);

        // then
        var association = (Element) mergedRoot(merged.get("LiaAgreement")).getElementsByTagName("Association").item(0);
        assertThat(association.getAttribute("target")).isEqualTo("ShdDeductible");
    }
}
