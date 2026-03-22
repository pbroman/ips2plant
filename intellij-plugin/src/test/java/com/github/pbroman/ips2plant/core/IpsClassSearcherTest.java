package com.github.pbroman.ips2plant.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class IpsClassSearcherTest {

    private final IpsClassSearcher searcher = new IpsClassSearcher();

    @TempDir
    Path tempDir;

    @Test
    void search_exactMatch_returnsFile() throws IOException {
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        var result = searcher.search("Contract", List.of(tempDir));

        assertThat(result).hasSize(1).containsKey("Contract");
    }

    @Test
    void search_wildcardSuffix_matchesMultiple() throws IOException {
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");
        Files.writeString(tempDir.resolve("ContractProduct.ipsproductcmpttype"), "<ProductCmptType2/>");
        Files.writeString(tempDir.resolve("Claim.ipspolicycmpttype"), "<PolicyCmptType/>");

        var result = searcher.search("Contract*", List.of(tempDir));

        assertThat(result).hasSize(2)
                .containsKeys("Contract", "ContractProduct");
    }

    @Test
    void search_wildcardBothEnds_matchesSubstring() throws IOException {
        Files.writeString(tempDir.resolve("MyContract.ipspolicycmpttype"), "<PolicyCmptType/>");
        Files.writeString(tempDir.resolve("ContractType.ipsproductcmpttype"), "<ProductCmptType2/>");
        Files.writeString(tempDir.resolve("Claim.ipspolicycmpttype"), "<PolicyCmptType/>");

        var result = searcher.search("*Contract*", List.of(tempDir));

        assertThat(result).hasSize(2)
                .containsKeys("MyContract", "ContractType");
    }

    @Test
    void search_caseInsensitive() throws IOException {
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        var result = searcher.search("contract", List.of(tempDir));

        assertThat(result).hasSize(1).containsKey("Contract");
    }

    @Test
    void search_withPackage_matchesOnlyClassName() throws IOException {
        var subDir = tempDir.resolve("com/example");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        // Searching by package prefix should NOT match (only class name is searched)
        var result = searcher.search("com.example.*", List.of(tempDir));
        assertThat(result).isEmpty();

        // Searching by class name should match
        var result2 = searcher.search("Contract", List.of(tempDir));
        assertThat(result2).hasSize(1).containsKey("com.example.Contract");
    }

    @Test
    void search_wildcardDoesNotMatchPackagePath() throws IOException {
        var subDir = tempDir.resolve("contract");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("EngContractType.ipspolicycmpttype"), "<PolicyCmptType/>");
        Files.writeString(subDir.resolve("SomeOtherType.ipspolicycmpttype"), "<PolicyCmptType/>");

        // "contract*" should match EngContractType (class name contains "contract" at start? no)
        // but should NOT match SomeOtherType just because it's in the "contract" package
        var result = searcher.search("*Contract*", List.of(tempDir));
        assertThat(result).hasSize(1).containsKey("contract.EngContractType");

        // "contract*" should not match anything — no class NAME starts with "contract"
        // (EngContractType starts with "Eng", SomeOtherType starts with "Some")
        // but case insensitive, so it would match EngContractType? No — "contract*" means starts with "contract"
        // EngContractType does not start with "contract" (it starts with "Eng")
        var result2 = searcher.search("contract*", List.of(tempDir));
        assertThat(result2).isEmpty();
    }

    @Test
    void search_noMatch_returnsEmpty() throws IOException {
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        var result = searcher.search("Claim", List.of(tempDir));

        assertThat(result).isEmpty();
    }

    @Test
    void search_emptyPattern_returnsEmpty() throws IOException {
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        var result = searcher.search("", List.of(tempDir));

        assertThat(result).isEmpty();
    }

    @Test
    void search_nullPattern_returnsEmpty() throws IOException {
        Files.writeString(tempDir.resolve("Contract.ipspolicycmpttype"), "<PolicyCmptType/>");

        var result = searcher.search(null, List.of(tempDir));

        assertThat(result).isEmpty();
    }

    @Test
    void wildcardToRegex_convertsCorrectly() {
        assertThat(IpsClassSearcher.wildcardToRegex("*Contract*")).isEqualTo(".*Contract.*");
        assertThat(IpsClassSearcher.wildcardToRegex("com.example.*")).isEqualTo("com\\.example\\..*");
        assertThat(IpsClassSearcher.wildcardToRegex("Contract")).isEqualTo("Contract");
    }
}
