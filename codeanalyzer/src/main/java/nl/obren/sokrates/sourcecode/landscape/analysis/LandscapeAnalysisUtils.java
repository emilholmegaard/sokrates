package nl.obren.sokrates.sourcecode.landscape.analysis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class LandscapeAnalysisUtils {
    private static final Log LOG = LogFactory.getLog(LandscapeAnalysisUtils.class);

    public static List<File> findAllSokratesLandscapeConfigFiles(File root) {
        LOG.info("Scanning files...");
        List<File> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(root.getPath()))) {
            paths.filter(path -> isSokratesLandscapeConfigFile(path)).forEach(path -> {
                files.add(path.toFile());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.sort(files, (a, b) ->
                b.getPath().replace("\\", "/").split("/").length
                        - a.getPath().replace("\\", "/").split("/").length);

        return files;
    }

    public static boolean isSokratesLandscapeConfigFile(Path file) {
        return file.endsWith("_sokrates_landscape/config.json");
    }

    public static void main(String args[]) {
        findAllSokratesLandscapeConfigFiles(new File("/Users/zobrenovic/Documents/landscapes/landscapes")).forEach(file -> LOG.info(file.getPath()));
    }
}
