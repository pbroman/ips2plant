package com.github.pbroman.ips2plant.api;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public interface XmlAssembler {

    void assemble(Map<String, File> ipsFiles, Path destination);
}
