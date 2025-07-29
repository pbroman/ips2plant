package com.github.pbroman.ips2plant;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;

import com.github.pbroman.ips2plant.core.assemble.DomXmlAssembler;
import com.github.pbroman.ips2plant.core.collect.DefaultIpsFileCollector;
import com.github.pbroman.ips2plant.core.xslt.DefaultXsltProcessor;
import com.github.pbroman.ips2plant.core.xslt.SaxonXsltProcessor;
import com.github.pbroman.ips2plant.core.xslt.XalanXsltProcessor;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@TopCommand
@Command(name = "ips2plant", mixinStandardHelpOptions = true)
public class Ips2PlantCommand implements Callable<Integer> {

    private static final int DEFAULT_CONNECTOR_LENGTH = 2;

    private final Map<String, String> xsltParams = new HashMap<>();
    private List<Path> modelDirPaths;
    private Path destination;
    private Path workdir;

    @Override
    public Integer call() throws Exception {
        var runner = new Ips2PlantRunner(
                new DefaultIpsFileCollector(),
                new DomXmlAssembler(),
                new SaxonXsltProcessor());
        runner.execute(modelDirPaths, xsltParams, destination, workdir);
        return 0;
    }


    // This might not be the best way to do it, but it works. :)

    @Option(names = {"-pf", "--package-filter"}, description = "Filter the diagram to a package and it's associations")
    private void setPackageFilter(String option) {
        xsltParams.put("packageFilter", option);
    }

    @Option(names = {"-l", "--connector-length"}, description = "Length of association connectors. Default: " + DEFAULT_CONNECTOR_LENGTH)
    private void setConnectors(int length) {
        xsltParams.put("connector", StringUtils.repeat('-', length));
        xsltParams.put("dottedConnector", StringUtils.repeat('.', length));
    }

    @Option(names = {"-r", "--print-target-role"}, description = "Print the targetRolePlural attribute on the composition arrow.")
    private void printTargetRole(boolean dummy) {
        setTrue("printTargetRole");
    }

    @Option(names = {"-s", "--add-super-type"}, description = "Adds inheritance of super types that are NOT present in the scanned models.")
    private void addSuperType(boolean dummy) {
        setTrue("addSuperType");
    }

    @Option(names = {"-a", "--add-associations"}, description = "Adds associations to classes that are NOT present under the scanned models")
    private void addAssociations(boolean dummy) {
        setTrue("addAssociations");
    }

    @Option(names = {"-t", "--show-tables"}, description = "Show tables")
    private void showTables(boolean dummy) {
        setTrue("showTables");
    }

    @Option(names = {"-tu", "--show-table-usage"}, description = "Show table usage by product component types (including external tables)")
    private void showTableUsage(boolean dummy) {
        setTrue("showTableUsage");
    }

    @Option(names = {"-et", "--show-enum-types"}, description = "Show enum types")
    private void showEnumTypes(boolean dummy) {
        setTrue("showEnumTypes");
    }

    @Option(names = {"-ea", "--show-enum-assoc"}, description = "Show enum associations (including external enums)")
    private void showEnumAssociations(boolean dummy) {
        setTrue("showEnumAssociations");
    }

    @Option(names = {"-pr", "--show-products"}, description = "Show product components")
    private void showProductComponents(boolean dummy) {
        setTrue("showProductComponents");
    }

    @Option(names = {"-k", "--packages"}, description = "Displays all classes in their packages")
    private void showPackages(boolean dummy) {
        setTrue("packages");
    }

    private void setTrue(String param) {
        xsltParams.put(param, "true");
    }

    @Option(names = {"-p", "--paths"}, description = "Path(s) to model directories")
    private void setPaths(List<String> paths) {
        modelDirPaths = paths.stream().map(Path::of).toList();
    }

    @Option(names = {"-o", "--output"}, description = "Output file path (should have .puml suffix)")
    private void setOutput(String output) {
        destination = Path.of(output);
    }

    @Option(names = {"-w", "--workdir"}, description = "Workdir for the collection.xml (optional)")
    private void setWorkdir(String workdir) {
        this.workdir = Path.of(workdir);
    }

}
