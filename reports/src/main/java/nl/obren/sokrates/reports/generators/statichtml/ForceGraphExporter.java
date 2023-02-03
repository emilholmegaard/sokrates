package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DLink;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DNode;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DObject;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ForceGraphExporter {

    public static final int MAX_DEPENDECIES_GRAPH_SIZE = 10000;

    public static String export3DForceGraph(List<ComponentDependency> componentDependencies, File reportsFolder, String graphId) {
        Force3DObject force3DObject = new Force3DObject();
        Map<String, Integer> names = new HashMap<>();
        componentDependencies.stream().limit(MAX_DEPENDECIES_GRAPH_SIZE).forEach(dependency -> {
            String from = dependency.getFromComponent();
            String to = dependency.getToComponent();
            if (names.containsKey(from)) {
                names.put(from, names.get(from) + 1);
            } else {
                names.put(from, 1);
            }
            if (names.containsKey(to)) {
                names.put(to, names.get(to) + 1);
            } else {
                names.put(to, 1);
            }
            force3DObject.getLinks().add(new Force3DLink(from, to, dependency.getCount()));
            force3DObject.getLinks().add(new Force3DLink(to, from, dependency.getCount()));
        });
        names.keySet().forEach(key -> {
            force3DObject.getNodes().add(new Force3DNode(key, names.get(key)));
        });
        String visualsPath = "html/visuals";
        File folder = new File(reportsFolder, visualsPath);
        folder.mkdirs();
        String fileName = graphId + "_force_3d.html";
        try {
            FileUtils.write(new File(folder, fileName), new VisualizationTemplate().render3DForceGraph(force3DObject), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "visuals/" + fileName;
    }
}
