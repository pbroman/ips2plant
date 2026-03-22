package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class IpsClassSearcherTest {

    private final IpsClassSearcher searcher = new IpsClassSearcher();

    @TempDir
    Path tempDir;

    @Test
    void search_exactMatch_returnsFile() throws IOException {
        // given
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = searcher.search("Contract", List.of(tempDir));

        // then
        assertThat(result).hasSize(1).containsKey("Contract");
    }

    @Test
    void search_wildcardSuffix_matchesMultiple() throws IOException {
        // given
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");
        Files.writeString(tempDir.resolve("ContractProduct.ipsproductcmpttype"), "<ProductCmptType2/>");
        Files.writeString(tempDir.resolve("Claim.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = searcher.search("Contract*", List.of(tempDir));

        // then
        assertThat(result).hasSize(2)
                .containsKeys("Contract", "ContractProduct");
    }

    @Test
    void search_wildcardBothEnds_matchesSubstring() throws IOException {
        // given
        Files.writeString(tempDir.resolve("MyContract.ipspolicycmpttype"), "<PolicyCmptType/>");
        Files.writeString(tempDir.resolve("ContractType.ipsproductcmpttype"), "<ProductCmptType2/>");
        Files.writeString(tempDir.resolve("Claim.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = searcher.search("*Contract*", List.of(tempDir));

        // then
        assertThat(result).hasSize(2)
                .containsKeys("MyContract", "ContractType");
    }

    @Test
    void search_caseInsensitive() throws IOException {
        // given
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = searcher.search("contract", List.of(tempDir));

        // then
        assertThat(result).hasSize(1).containsKey("Contract");
    }

    @Test
    void search_packagePrefix_doesNotMatchClassName() throws IOException {
        // given
        var subDir = tempDir.resolve("com/example");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = searcher.search("com.example.*", List.of(tempDir));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void search_className_matchesWithinPackage() throws IOException {
        // given
        var subDir = tempDir.resolve("com/example");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = searcher.search("Contract", List.of(tempDir));

        // then
        assertThat(result).hasSize(1).containsKey("com.example.Contract");
    }

    @Test
    void search_wildcardMatchesClassName_notPackagePath() throws IOException {
        // given
        var subDir = tempDir.resolve("contract");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("EngContractType.ipspolicycmpttype"), "<PolicyCmptType/>");
        Files.writeString(subDir.resolve("SomeOtherType.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = searcher.search("*Contract*", List.of(tempDir));

        // then
        assertThat(result).hasSize(1).containsKey("contract.EngContractType");
    }

    @Test
    void search_wildcardPrefix_doesNotMatchByPackage() throws IOException {
        // given
        var subDir = tempDir.resolve("contract");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("EngContractType.ipspolicycmpttype"), "<PolicyCmptType/>");
        Files.writeString(subDir.resolve("SomeOtherType.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when — "contract*" matches class names starting with "contract", not package names
        var result = searcher.search("contract*", List.of(tempDir));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void search_noMatch_returnsEmpty() throws IOException {
        // given
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = searcher.search("Claim", List.of(tempDir));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void search_emptyPattern_returnsEmpty() throws IOException {
        // given
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = searcher.search("", List.of(tempDir));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void search_nullPattern_returnsEmpty() throws IOException {
        // given
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = searcher.search(null, List.of(tempDir));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void wildcardToRegex_convertsCorrectly() {
        // given / when / then
        assertThat(IpsClassSearcher.wildcardToRegex("*Contract*")).isEqualTo(".*Contract.*");
        assertThat(IpsClassSearcher.wildcardToRegex("com.example.*")).isEqualTo("com\\.example\\..*");
        assertThat(IpsClassSearcher.wildcardToRegex("Contract")).isEqualTo("Contract");
    }

    @Test
    void addSupertypes_addsParentTransitively() throws IOException {
        // given: Child -> Parent -> GrandParent
        Files.writeString(tempDir.resolve("Child.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType supertype=\"Parent\"/>");
        Files.writeString(tempDir.resolve("Parent.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType supertype=\"GrandParent\"/>");
        Files.writeString(tempDir.resolve("GrandParent.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        Files.writeString(tempDir.resolve("Unrelated.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");

        var base = new LinkedHashMap<String, File>();
        base.put("Child", tempDir.resolve("Child.ipspolicycmpttype").toFile());

        // when
        var result = searcher.addSupertypes(base, List.of(tempDir));

        // then
        assertThat(result).hasSize(3)
                .containsKeys("Child", "Parent", "GrandParent")
                .doesNotContainKey("Unrelated");
    }

    @Test
    void addSupertypes_noSupertype_returnsOriginal() throws IOException {
        // given
        Files.writeString(tempDir.resolve("Leaf.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");

        var base = new LinkedHashMap<String, File>();
        base.put("Leaf", tempDir.resolve("Leaf.ipspolicycmpttype").toFile());

        // when
        var result = searcher.addSupertypes(base, List.of(tempDir));

        // then
        assertThat(result).hasSize(1).containsKey("Leaf");
    }

    @Test
    void addReferencingClasses_addsClassesThatReference() throws IOException {
        // given
        Files.writeString(tempDir.resolve("Target.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        Files.writeString(tempDir.resolve("Referrer.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType><Association target=\"Target\" associationType=\"comp\"/></PolicyCmptType>");
        Files.writeString(tempDir.resolve("Unrelated.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType><Association target=\"Other\" associationType=\"comp\"/></PolicyCmptType>");

        var base = new LinkedHashMap<String, File>();
        base.put("Target", tempDir.resolve("Target.ipspolicycmpttype").toFile());

        // when
        var result = searcher.addReferencingClasses(base, List.of(tempDir));

        // then
        assertThat(result).hasSize(2)
                .containsKeys("Target", "Referrer")
                .doesNotContainKey("Unrelated");
    }

    @Test
    void addReferencingClasses_noReferences_returnsOriginal() throws IOException {
        // given
        Files.writeString(tempDir.resolve("Lonely.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType/>");
        Files.writeString(tempDir.resolve("Other.ipspolicycmpttype"),
                "<?xml version=\"1.0\"?><PolicyCmptType><Association target=\"SomeoneElse\" associationType=\"ass\"/></PolicyCmptType>");

        var base = new LinkedHashMap<String, File>();
        base.put("Lonely", tempDir.resolve("Lonely.ipspolicycmpttype").toFile());

        // when
        var result = searcher.addReferencingClasses(base, List.of(tempDir));

        // then
        assertThat(result).hasSize(1).containsKey("Lonely");
    }
}