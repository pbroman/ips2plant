package com.github.pbroman.ips2plant.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class IpsFileCollectorTest {

    private final IpsFileCollector collector = new IpsFileCollector();

    @TempDir
    Path tempDir;

    @Test
    void collect_emptyDirectory_returnsEmptyMap() {
        // given
        var dirs = List.of(tempDir);

        // when
        var result = collector.collect(dirs);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void collect_singleIpsFile_returnsClassNameAndFile() throws IOException {
        // given
        var file = tempDir.resolve("MyPolicy.ipspolicycmpttype");
        Files.writeString(file, "<PolicyCmptType/>");

        // when
        var result = collector.collect(List.of(tempDir));

        // then
        assertThat(result).hasSize(1)
                .containsKey("MyPolicy");
        assertThat(result.get("MyPolicy")).isEqualTo(file.toFile());
    }

    @Test
    void collect_nestedIpsFile_derivesPackagedClassName() throws IOException {
        // given
        var subDir = tempDir.resolve("com/example");
        Files.createDirectories(subDir);
        var file = subDir.resolve("Contract.ipspolicycmpttype");
        Files.writeString(file, "<PolicyCmptType/>");

        // when
        var result = collector.collect(List.of(tempDir));

        // then
        assertThat(result).containsKey("com.example.Contract");
    }

    @Test
    void collect_multipleFileTypes_collectsAll() throws IOException {
        // given
        Files.writeString(tempDir.resolve("A.ipspolicycmpttype"), "<PolicyCmptType/>");
        Files.writeString(tempDir.resolve("B.ipsproductcmpttype"), "<ProductCmptType2/>");
        Files.writeString(tempDir.resolve("C.ipsenumtype"), "<EnumType/>");
        Files.writeString(tempDir.resolve("D.ipstablestructure"), "<TableStructure/>");

        // when
        var result = collector.collect(List.of(tempDir));

        // then
        assertThat(result).hasSize(4)
                .containsKeys("A", "B", "C", "D");
    }

    @Test
    void collect_nonIpsFiles_areIgnored() throws IOException {
        // given
        Files.writeString(tempDir.resolve("README.md"), "hello");
        Files.writeString(tempDir.resolve("pom.xml"), "<project/>");
        Files.writeString(tempDir.resolve("Main.java"), "class Main {}");

        // when
        var result = collector.collect(List.of(tempDir));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void collect_unsupportedIpsFileTypes_areIgnored() throws IOException {
        // given — IPS file types not in the supported list
        Files.writeString(tempDir.resolve("Something.ipssrcfile"), "<IpsSrcFile/>");
        Files.writeString(tempDir.resolve("Test.ipstest"), "<Test/>");
        Files.writeString(tempDir.resolve("Product.ipsproductcmpt"), "<ProductCmpt/>");
        // supported file should still be collected
        Files.writeString(tempDir.resolve("Policy.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = collector.collect(List.of(tempDir));

        // then
        assertThat(result).hasSize(1).containsKey("Policy");
    }

    @Test
    void collect_multipleDirectories_mergesResults() throws IOException {
        // given
        var dir1 = tempDir.resolve("dir1");
        var dir2 = tempDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        Files.writeString(dir1.resolve("A.ipspolicycmpttype"), "<PolicyCmptType/>");
        Files.writeString(dir2.resolve("B.ipspolicycmpttype"), "<PolicyCmptType/>");

        // when
        var result = collector.collect(List.of(dir1, dir2));

        // then
        assertThat(result).hasSize(2)
                .containsKeys("A", "B");
    }
}