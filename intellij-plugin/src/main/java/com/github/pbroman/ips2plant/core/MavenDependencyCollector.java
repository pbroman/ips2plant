package com.github.pbroman.ips2plant.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.openapi.diagnostic.Logger;

/**
 * Extracts IPS model files from Maven dependency JARs.
 * <p>
 * Given a directory containing a pom.xml, resolves the Maven classpath
 * and scans each JAR for IPS model files (model&#47;**&#47;*.ips*).
 * Matching files are extracted to a temporary directory so they can be
 * included in the PlantUML collection XML.
 */
public class MavenDependencyCollector {

    private static final Logger LOG = Logger.getInstance(MavenDependencyCollector.class);

    private static final String MODEL_PREFIX = "model/";
    private static final Pattern JAR_ARTIFACT_PATTERN = Pattern.compile("([a-zA-Z][a-zA-Z0-9.]*(?:-[a-zA-Z][a-zA-Z0-9.]*)*)-\\d.*\\.jar$");

    private final Set<String> resolvedArtifacts = new HashSet<>();

    public record DependencyModel(String artifactId, Path modelDir, Path tempRoot) {}

    /**
     * Finds dependency JARs containing IPS model files and extracts them.
     *
     * @param pomDir directory containing pom.xml
     * @param excludeArtifactIds artifact IDs to exclude (e.g. project-internal modules)
     * @return list of dependency models with artifactId and extracted model directory
     */
    public List<DependencyModel> collectFromDependencies(Path pomDir, Set<String> excludeArtifactIds) throws IOException, InterruptedException {
        LOG.info("collectFromDependencies: resolving classpath for " + pomDir);
        var classpath = resolveMavenClasspath(pomDir);
        if (classpath.isEmpty()) {
            LOG.warn("collectFromDependencies: empty classpath for " + pomDir);
            return List.of();
        }

        var jarPaths = parseClasspath(classpath);
        LOG.info("collectFromDependencies: found " + jarPaths.size() + " JARs in classpath");

        var result = new ArrayList<DependencyModel>();

        for (var jarPath : jarPaths) {
            var artifactId = extractArtifactId(jarPath);
            if (excludeArtifactIds.contains(artifactId)) {
                LOG.info("collectFromDependencies: skipping project-internal dependency " + artifactId);
                continue;
            }
            if (!resolvedArtifacts.add(artifactId)) {
                LOG.info("collectFromDependencies: skipping already resolved artifact " + artifactId);
                continue;
            }
            var extracted = extractIpsFilesFromJar(jarPath);
            if (extracted != null) {
                LOG.info("collectFromDependencies: extracted IPS models from " + artifactId + " -> " + extracted);
                result.add(new DependencyModel(artifactId, extracted, extracted.getParent()));
            }
        }

        return result;
    }

    static String extractArtifactId(String jarPath) {
        var fileName = Path.of(jarPath).getFileName().toString();
        Matcher m = JAR_ARTIFACT_PATTERN.matcher(fileName);
        if (m.find()) {
            return m.group(1);
        }
        // Fallback: strip .jar extension
        return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    String resolveMavenClasspath(Path pomDir) throws IOException, InterruptedException {
        var pomFile = pomDir.resolve("pom.xml");
        if (!Files.isRegularFile(pomFile)) {
            LOG.warn("resolveMavenClasspath: pom.xml not found at " + pomFile);
            return "";
        }

        var outputFile = Files.createTempFile("ips2plant-cp-", ".txt");
        try {
            var mvnCmd = findMvnCommand();
            LOG.info("resolveMavenClasspath: running " + mvnCmd + " dependency:build-classpath -f " + pomFile);

            var command = new ArrayList<String>();
            // On Windows, .cmd/.bat files must be run via cmd.exe
            if (mvnCmd.endsWith(".cmd") || mvnCmd.endsWith(".bat")) {
                command.add("cmd.exe");
                command.add("/c");
            }
            command.add(mvnCmd);
            command.add("dependency:build-classpath");
            command.add("-DincludeScope=compile");
            command.add("-DincludeGroupIds=de.faktorzehn");
            command.add("-Dmdep.outputFile=" + outputFile);
            command.add("-q");
            command.add("-f");
            command.add(pomFile.toString());

            var process = new ProcessBuilder(command)
                    .directory(pomDir.toFile())
                    .redirectErrorStream(true)
                    .start();

            // Consume process output to prevent blocking
            readProcessOutput(process.getInputStream());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                LOG.warn("resolveMavenClasspath: mvn exited with code " + exitCode);
                return "";
            }

            if (!Files.isRegularFile(outputFile) || Files.size(outputFile) == 0) {
                LOG.info("resolveMavenClasspath: no dependencies found for " + pomDir);
                return "";
            }

            var classpath = Files.readString(outputFile).trim();
            LOG.info("resolveMavenClasspath: classpath length=" + classpath.length());
            return classpath;
        } finally {
            Files.deleteIfExists(outputFile);
        }
    }

    private String findMvnCommand() {
        var os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");
        String mvnName = isWindows ? "mvn.cmd" : "mvn";

        // 1. Check MAVEN_HOME / M2_HOME env vars
        for (var envVar : new String[]{"MAVEN_HOME", "M2_HOME"}) {
            var home = System.getenv(envVar);
            if (home != null && !home.isBlank()) {
                var mvnCmd = Path.of(home, "bin", mvnName);
                if (Files.isRegularFile(mvnCmd)) {
                    LOG.info("findMvnCommand: using " + envVar + ": " + mvnCmd);
                    return mvnCmd.toString();
                }
            }
        }

        // 2. Search PATH entries directly (IntelliJ may not have mvn on its inherited PATH)
        var pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            pathEnv = System.getenv("Path");
        }
        if (pathEnv != null) {
            for (var dir : pathEnv.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                var candidate = Path.of(dir, mvnName);
                if (Files.isRegularFile(candidate)) {
                    LOG.info("findMvnCommand: found on PATH: " + candidate);
                    return candidate.toString();
                }
            }
        }

        // 3. Scan common install locations
        var userHome = System.getProperty("user.home");
        String[] searchRoots;
        if (isWindows) {
            searchRoots = new String[]{
                    userHome + "\\AppData\\Local\\Programs",
                    "C:\\Program Files",
                    "C:\\Program Files (x86)",
                    "C:\\tools",
            };
        } else {
            searchRoots = new String[]{
                    "/usr/local",
                    "/opt/homebrew",
                    "/usr/share/maven",
                    "/opt/maven",
                    userHome + "/.sdkman/candidates/maven/current",
            };
        }
        for (var root : searchRoots) {
            // Check if mvn exists directly in bin/ under this root
            var directCandidate = Path.of(root, "bin", mvnName);
            if (Files.isRegularFile(directCandidate)) {
                LOG.info("findMvnCommand: found at " + directCandidate);
                return directCandidate.toString();
            }
            // Scan for apache-maven-* subdirectories
            var rootDir = new File(root);
            if (!rootDir.isDirectory()) continue;
            var children = rootDir.listFiles((d, name) -> name.toLowerCase().startsWith("apache-maven"));
            if (children != null) {
                for (var child : children) {
                    var candidate = child.toPath().resolve("bin").resolve(mvnName);
                    if (Files.isRegularFile(candidate)) {
                        LOG.info("findMvnCommand: found in " + root + ": " + candidate);
                        return candidate.toString();
                    }
                }
            }
        }

        LOG.warn("findMvnCommand: could not find Maven, falling back to '" + mvnName + "'");
        return mvnName;
    }

    private String readProcessOutput(InputStream inputStream) throws IOException {
        var sb = new StringBuilder();
        try (var reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    List<String> parseClasspath(String classpath) {
        var result = new ArrayList<String>();
        if (classpath == null || classpath.isBlank()) {
            return result;
        }
        var separator = File.pathSeparator;
        for (var entry : classpath.split(java.util.regex.Pattern.quote(separator))) {
            var trimmed = entry.trim();
            if (trimmed.endsWith(".jar") && Files.isRegularFile(Path.of(trimmed))) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Scans a JAR for IPS model files under model/ and extracts them to a temp dir.
     *
     * @return temp directory path, or null if the JAR contains no IPS model files
     */
    Path extractIpsFilesFromJar(String jarPath) throws IOException {
        var jarFile = new File(jarPath);
        if (!jarFile.isFile()) {
            return null;
        }

        List<JarEntry> ipsEntries = new ArrayList<>();

        try (var jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var name = entry.getName();
                if (!entry.isDirectory()
                        && name.startsWith(MODEL_PREFIX)
                        && IpsFileCollector.isSupportedIpsFile(name)) {
                    ipsEntries.add(entry);
                }
            }

            if (ipsEntries.isEmpty()) {
                return null;
            }

            // Extract to temp dir
            var tempDir = Files.createTempDirectory("ips2plant-dep-");
            var modelDir = tempDir.resolve("model");

            for (var entry : ipsEntries) {
                // entry name is like "model/contract/Contract.ipspolicycmpttype"
                // We want to strip the "model/" prefix and place under tempDir/model/
                var targetFile = tempDir.resolve(entry.getName());
                Files.createDirectories(targetFile.getParent());
                try (var is = jar.getInputStream(entry)) {
                    Files.copy(is, targetFile);
                }
            }

            return modelDir;
        }
    }
}
