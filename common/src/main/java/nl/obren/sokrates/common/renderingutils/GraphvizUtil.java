/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;


import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class GraphvizUtil {
    private static final Log LOG = LogFactory.getLog(GraphvizUtil.class);
    public static final String ERROR_SVG = "<svg viewBox=\"0 0 240 80\"><text x=\"20\" y=\"35\">ERROR: Graphviz not found.</text></svg>";

    private GraphvizUtil() {

    }

    public static String getSvgFromDot(String dotCode) {
        String svg = getSvgFromDot(dotCode, new String[]{});
        return svg != null ? svg : ERROR_SVG;
    }

    public static String getSvgFromDot(String dotCode, String extraDotArguments[]) {
        File dotFile = null;
        double start = System.currentTimeMillis();
        try {
            LOG.info("Calling dot process... ");
            dotFile = File.createTempFile("grapviz_dot_graph", ".dot");
            FileUtils.writeStringToFile(dotFile, dotCode, UTF_8);
            String svgFromDotFile = getSvgFromDotFileExternal(dotFile, extraDotArguments);
            int svgBeginIndex = svgFromDotFile.indexOf("<svg");
            if (svgBeginIndex >= 0) {
                svgFromDotFile = svgFromDotFile.substring(svgBeginIndex);
            }
            return svgFromDotFile;
        } catch (IOException e) {
            LOG.error(e);
        } finally {
            LOG.info(" Done after " + ((System.currentTimeMillis() - start) / 1000) + " seconds.");
            if (dotFile != null) {
                dotFile.delete();
            }
        }

        return null;
    }

    public static String getSvgFromDotFileExternal(File dotFile, String extraDotArguments[]) {
        File svgFile = null;
        try {
            svgFile = File.createTempFile("dependencies_dot_image", ".svg");
            List<String> dotArguments = getDotParameters(dotFile, svgFile);
            Collections.addAll(dotArguments, extraDotArguments);
            runDot(dotArguments);

            if (svgFile.exists()) {
                return FileUtils.readFileToString(svgFile, UTF_8);
            } else {
                return null;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e);
        } finally {
            if (svgFile != null) {
                svgFile.delete();
            }
        }

        return null;
    }

    private static void runDot(List<String> dotArguments) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(dotArguments.toArray(new String[dotArguments.size()]));
        String graphVizDotPath = GraphvizSettings.getGraphVizDotPath();
        if (graphVizDotPath != null) {
            processBuilder.directory(new File(graphVizDotPath).getParentFile());
            Process theProcess = processBuilder.start();
            theProcess.waitFor();
        }
    }

    private static List<String> getDotParameters(File dotFile, File imgFile) {
        List<String> dotParams = new ArrayList<String>();

        dotParams.add(GraphvizSettings.getGraphVizDotPath());
        dotParams.add("-Tsvg");

        dotParams.add("-o" + imgFile.getAbsolutePath());
        dotParams.add(dotFile.getAbsolutePath());
        return dotParams;
    }
}
