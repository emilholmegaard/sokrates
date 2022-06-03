package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.renderingutils.GraphvizUtil;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.Force3DGraphExporter;
import nl.obren.sokrates.reports.landscape.utils.LandscapeGeneratorUtils;
import nl.obren.sokrates.reports.landscape.utils.TagStats;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.landscape.ProjectTag;
import nl.obren.sokrates.sourcecode.landscape.ProjectTagGroup;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class LandscapeProjectsTagsReport {
    private static final Log LOG = LogFactory.getLog(LandscapeProjectsTagsReport.class);

    private LandscapeAnalysisResults landscapeAnalysisResults;
    private Map<String, TagStats> tagStatsMap = new HashMap<>();
    private File reportsFolder;

    private List<ProjectTagGroup> projectTagGroups = new ArrayList<>();
    private String type;
    private boolean renderLangIcons;

    public LandscapeProjectsTagsReport(LandscapeAnalysisResults landscapeAnalysisResults, List<ProjectTagGroup> projectTagGroups, String type, boolean renderLangIcons) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.projectTagGroups = projectTagGroups;
        this.type = type;
        this.renderLangIcons = renderLangIcons;
    }

    public void saveProjectsReport(RichTextReport report, File reportsFolder, List<ProjectAnalysisResults> projectsAnalysisResults) {
        this.reportsFolder = reportsFolder;

        updateTagMap(projectsAnalysisResults);

        report.startDiv("position: absolute; left: 1px");
        addTagStats(report);
        report.endDiv();
    }

    private void addTagStats(RichTextReport report) {
        renderTagDependencies();

        // addAllTagDependencies(report);

        report.startTable();
        report.addTableHeader("Tag", "# projects", "LOC<br>(main)", "LOC<br>(test)", "LOC<br>(active)", "LOC<br>(new)", "# commits<br>(30 days)", "# contributors<br>(30 days)");
        int index[] = {0};
        projectTagGroups.stream().filter(tagGroup -> tagGroup.getProjectTags().size() > 0).forEach(tagGroup -> {
            int count[] = {0};
            tagGroup.getProjectTags().stream().forEach(projectTag -> {
                if (tagStatsMap.get(projectTag.getKey()) != null) count[0] += 1;
            });
            if (count[0] == 0) {
                return;
            }
            index[0] += 1;
            report.startTableRow();
            report.startMultiColumnTableCell(8, "");
            report.startDiv("border-radius: 9px; padding: 6px; margin-top: 16px; border: 1px solid lightgrey; background-color: " + tagGroup.getColor());
            report.addHtmlContent(tagGroup.getName() + " (" + count[0] + ")");
            if (StringUtils.isNotBlank(tagGroup.getDescription())) {
                report.addHtmlContent("<span style='color: grey;'>: " + tagGroup.getDescription() + "</span>");
            }
            report.startDiv("margin: 5px; font-size: 80%");
            report.addHtmlContent("tag dependencies: ");
            report.addNewTabLink("3D graph (via projects)", "visuals/" + type + "_tags_graph_" + index[0] + "_force_3d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph (excluding projects)", "visuals/" + type + "_tags_graph_" + index[0] + "_direct_force_3d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph (via projects)", "visuals/" + type + "_tags_graph_" + index[0] + ".svg");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph (excluding projects)", "visuals/" + type + "_tags_graph_" + index[0] + "_direct.svg");
            report.addNewTabLink("2D graph (excluding projects)", "visuals/" + type + "_tags_graph_" + index[0] + "_direct.svg");
            report.endDiv();

            report.endTableCell();
            report.endTableRow();
            tagGroup.getProjectTags().stream()
                    .filter(t -> (tagStatsMap.get(t.getKey()) != null))
                    .sorted((a, b) -> tagStatsMap.get(b.getKey()).getProjectsAnalysisResults().size() - tagStatsMap.get(a.getKey()).getProjectsAnalysisResults().size())
                    .forEach(projectTag -> addTagRow(report, projectTag.getTag(), projectTag, tagGroup.getColor()));
        });
        if (tagStatsMap.containsKey("")) {
            report.addMultiColumnTableCell("&nbsp;", 8);
            addTagRow(report, "", new ProjectTag(), "lightgrey");
        }
        report.endTable();


        visualizeTagProjects(report);
    }

    private void renderTagDependencies() {
        int index[] = {0};
        projectTagGroups.forEach(tagGroup -> {
            index[0] += 1;
            String prefix = type + "_tags_graph_" + index[0];
            List<ProjectTag> groupTags = tagGroup.getProjectTags();
            exportTagGraphs(prefix, groupTags);
        });

        List<ProjectTag> allTags = new ArrayList<>();
        projectTagGroups.forEach(tagGroup -> {
            allTags.addAll(tagGroup.getProjectTags());
        });
        String prefix = type + "_tags_graph";
        exportTagGraphs(prefix, allTags);
    }

    private void exportTagGraphs(String prefix, List<ProjectTag> groupTags) {
        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, Set<String>> projectTagsMap = new HashMap<>();
        groupTags.stream().filter(tag -> tagStatsMap.get(tag.getKey()) != null)
                .forEach(tag -> {
                    TagStats stats = tagStatsMap.get(tag.getKey());
                    stats.getProjectsAnalysisResults().forEach(project -> {
                        String name = project.getAnalysisResults().getMetadata().getName();
                        dependencies.add(new ComponentDependency("[" + name + "]", tag.getTag()));
                        if (!projectTagsMap.containsKey(name)) {
                            projectTagsMap.put(name, new HashSet<>());
                        }
                        projectTagsMap.get(name).add(tag.getKey());
                    });
                });
        new Force3DGraphExporter().export3DForceGraph(dependencies, reportsFolder, prefix);

        List<ComponentDependency> directDependencies = new ArrayList<>();
        Map<String, ComponentDependency> directDependenciesMap = new HashMap<>();
        projectTagsMap.values().forEach(projectTags -> {
            projectTags.forEach(tag1 -> {
                projectTags.stream().filter(tag2 -> !tag1.equals(tag2)).forEach(tag2 -> {
                    String key1 = tag1 + "::" + tag2;
                    String key2 = tag2 + "::" + tag1;
                    if (directDependenciesMap.containsKey(key1)) {
                        directDependenciesMap.get(key1).increment(1);
                    } else if (directDependenciesMap.containsKey(key2)) {
                        directDependenciesMap.get(key2).increment(1);
                    } else {
                        ComponentDependency directDependency = new ComponentDependency(
                                tag1 + " (" + tagStatsMap.get(tag1).getProjectsAnalysisResults().size() + ")",
                                tag2 + " (" + tagStatsMap.get(tag2).getProjectsAnalysisResults().size() + ")");
                        directDependencies.add(directDependency);
                        directDependenciesMap.put(key1, directDependency);
                    }
                });
            });
        });

        directDependencies.forEach(d -> d.setCount(d.getCount() / 2));
        new Force3DGraphExporter().export3DForceGraph(directDependencies, reportsFolder, prefix + "_direct");

        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setMaxNumberOfDependencies(200);
        graphvizDependencyRenderer.setTypeGraph();
        graphvizDependencyRenderer.setOrientation("RL");
        List<String> keys = tagStatsMap.keySet().stream().filter(t -> tagStatsMap.get(t) != null).collect(Collectors.toList());
        String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(keys), dependencies);
        String graphvizContentDirect = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), directDependencies);
        try {
            FileUtils.write(new File(reportsFolder, "visuals/" + prefix + ".svg"), GraphvizUtil.getSvgFromDot(graphvizContent), StandardCharsets.UTF_8);
            FileUtils.write(new File(reportsFolder, "visuals/" + prefix + "_direct.svg"), GraphvizUtil.getSvgFromDot(graphvizContentDirect), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.info(e);
        }
    }

    private void visualizeTagProjects(RichTextReport report) {
        report.startDiv("margin: 2px; margin-top: 18px;");
        report.startShowMoreBlock("show visuals...");
        int maxLoc = this.landscapeAnalysisResults.getProjectAnalysisResults().stream()
                .map(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                .reduce((a, b) -> Math.max(a, b)).get();
        int maxCommits = this.landscapeAnalysisResults.getProjectAnalysisResults().stream()
                .map(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                .reduce((a, b) -> Math.max(a, b)).get();
        projectTagGroups.forEach(tagGroup -> {
            tagGroup.getProjectTags().forEach(tag -> {
                visualizeTag(report, maxLoc, maxCommits, tag);
            });
        });
        if (tagStatsMap.containsKey("")) {
            visualizeTag(report, maxLoc, maxCommits, new ProjectTag());
        }
        report.endShowMoreBlock();
        report.endDiv();
    }

    private void visualizeTag(RichTextReport report, int maxLoc, int maxCommits, ProjectTag tag) {
        TagStats stats = tagStatsMap.get(tag.getKey());
        if (stats == null) {
            return;
        }

        String tagName = tag.getKey();
        report.startDiv("margin: 18px;");
        report.addContentInDiv("<b>" + (tagName.isBlank() ? "Untagged" : tagName) + "</b> (" + stats.getProjectsAnalysisResults().size() + ")", "margin-bottom: 5px");
        List<ProjectAnalysisResults> projects = stats.getProjectsAnalysisResults();
        projects.sort((a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days() - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days());
        projects.forEach(project -> {
            int loc = project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
            int barSize = 3 + (int) Math.round(Math.sqrt(4900 * ((double) loc / maxLoc)));
            int commitsCount30Days = project.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days();
            double opacity = commitsCount30Days > 0 ? 0.4 + 0.6 * commitsCount30Days / maxCommits : 0.0;
            report.startNewTabLink(getProjectReportUrl(project), "");
            report.startDivWithLabel(tooltip(project),
                    "border: 1px solid grey; border-radius: 50%;" +
                            "display: inline-block; " +
                            "padding: 0;" +
                            "vertical-align: middle; " +
                            "overflow: none; " +
                            "width: " + (barSize + 2) + "px; " +
                            "height: " + (barSize + 2) + "px; ");
            report.startDiv(" margin: 0;border-radius: 50%;" +
                    "opacity: " + opacity + ";" +
                    "background-color: " + getTabColor(stats.getTag()) + "; " +
                    "border: 1px solid lightgrey; cursor: pointer;" +
                    "width: " + barSize + "px; " +
                    "height: " + barSize + "px; ");
            report.endDiv();
            report.endDiv();
            report.endNewTabLink();
        });
        report.endDiv();
    }

    private void addTagRow(RichTextReport report, String tagName, ProjectTag tag, String color) {
        TagStats stats = tagStatsMap.get(tag.getKey());
        if (stats == null) {
            return;
        }
        report.startTableRow("text-align: center");
        report.startTableCell();
        if (StringUtils.isNotBlank(tagName)) {
            String tooltip = getTagTooltip(tag);

            String htmlFragment = "";
            String style = "vertical-align: top; cursor: help; padding: 4px; border-radius: 6px; border: 1px solid lightgrey; background-color: " + color;

            if (renderLangIcons) {
                String imageHtml = DataImageUtils.getLangDataImageDiv30(tagName);
                htmlFragment = imageHtml + "<div style='margin: 6px; display: inline-block;'>" + tagName + "</div>";
            } else {
                htmlFragment = tagName;
            }

            report.addContentInDivWithTooltip(htmlFragment, tooltip, style);
        } else {
            report.addContentInDiv("Untagged");
        }
        report.endTableCell();
        if (stats != null) {
            List<ProjectAnalysisResults> projectsAnalysisResults = new ArrayList<>(stats.getProjectsAnalysisResults());
            projectsAnalysisResults.sort((a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
            int count = projectsAnalysisResults.size();
            report.startTableCell("text-align: left;");
            report.startShowMoreBlock("<b>" + count + "</b>" + (count == 1 ? " project" : " projects"));
            projectsAnalysisResults.forEach(project -> {
                CodeAnalysisResults projectAnalysisResults = project.getAnalysisResults();
                String projectReportUrl = getProjectReportUrl(project);
                report.addContentInDiv(
                        "<a href='" + projectReportUrl + "' target='_blank' style='margin-left: 10px'>" + projectAnalysisResults.getMetadata().getName() + "</a> "
                                + "<span color='lightgrey'>(<b>"
                                + FormattingUtils.formatCount(projectAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode(), "-") + "</b> LOC)</span>");
            });
            report.endShowMoreBlock();
            report.endTableCell();
            report.addTableCell(FormattingUtils.formatCount(projectsAnalysisResults
                    .stream()
                    .mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                    .sum()), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(projectsAnalysisResults
                    .stream()
                    .mapToInt(p -> p.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode())
                    .sum(), "-"), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(LandscapeAnalysisResults.getLoc1YearActive(projectsAnalysisResults), "-"), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(LandscapeAnalysisResults.getLocNew(projectsAnalysisResults), "-"), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(projectsAnalysisResults
                    .stream()
                    .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                    .sum(), "-"), "text-align: center");
            int recentContributorCount = getRecentContributorCount(projectsAnalysisResults);
            if (recentContributorCount > 0) {
                report.addTableCell("<div style='vertical-align: middle; display: inline-block'>" + FormattingUtils.formatCount(recentContributorCount, "-") + "</div><div style='vertical-align: middle; display: inline-block'>" + LandscapeReportGenerator.DEVELOPER_SVG_ICON + "</div>", "text-align: center; vertical-align: middle");
            } else {
                report.addTableCell("-", "text-align: center; vertical-align: middle");
            }
        } else {
            report.addTableCell("");
            report.addTableCell("");
            report.addTableCell("");
            report.addTableCell("");
            report.addTableCell("");
        }

        report.endTableRow();
    }

    private String getTagTooltip(ProjectTag tag) {
        String tooltip = "";

        if (tag.getPatterns().size() > 0) {
            tooltip += "includes projects with names like:\n  - " + tag.getPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getExcludePatterns().size() > 0) {
            tooltip += "excludes projects with names like:\n  - " + tag.getExcludePatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getPathPatterns().size() > 0) {
            tooltip += "includes projects with at least one file matching:\n  - " + tag.getPathPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getExcludePathPatterns().size() > 0) {
            tooltip += "excludes projects with at least one file matching:\n  - " + tag.getExcludePathPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getMainExtensions().size() > 0) {
            tooltip += "includes projects with main extensions:\n  - " + tag.getMainExtensions().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getAnyExtensions().size() > 0) {
            tooltip += "includes projects with any file with extensions:\n  - " + tag.getMainExtensions().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        return tooltip;
    }

    private void updateTagMap(List<ProjectAnalysisResults> projects) {
        projects.forEach(project -> {
            List<NumericMetric> linesOfCodePerExtension = project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension();
            linesOfCodePerExtension.sort((a, b) -> b.getValue().intValue() - a.getValue().intValue());
            String mainTech = linesOfCodePerExtension.size() > 0 ? linesOfCodePerExtension.get(0).getName().replaceAll(".*[.]", "") : "";
            List<ProjectTag> tags = new ArrayList<>();
            projectTagGroups.forEach(tagGroup -> tags.addAll(tagGroup.getProjectTags()));

            boolean tagged[] = {false};

            tags.forEach(tag -> {
                if (isTagged(project, mainTech, tag)) {
                    String key = tag.getKey();
                    if (!tagStatsMap.containsKey(key)) {
                        tagStatsMap.put(key, new TagStats(tag));
                    }
                    tagStatsMap.get(key).getProjectsAnalysisResults().add(project);
                    tagged[0] = true;
                }
            });

            if (!tagged[0]) {
                if (!tagStatsMap.containsKey("")) {
                    tagStatsMap.put("", new TagStats(new ProjectTag()));
                }
                tagStatsMap.get("").getProjectsAnalysisResults().add(project);
            }

        });
    }

    private boolean isTagged(ProjectAnalysisResults project, String mainTech, ProjectTag tag) {
        String name = project.getAnalysisResults().getMetadata().getName();
        return !tag.excludesMainTechnology(mainTech) &&
                ((tag.matchesName(name) && !tag.excludeName(name)) || tag.matchesMainTechnology(mainTech) || tag.matchesAnyTechnology(LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension())) || tag.matchesPath(project.getFiles()));
    }

    private String getTabColor(ProjectTag tag) {
        return tag.getGroup() != null && StringUtils.isNotBlank(tag.getGroup().getColor()) ? tag.getGroup().getColor() : "#99badd";
    }

    private String getProjectReportFolderUrl(ProjectAnalysisResults projectAnalysis) {
        return landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/";
    }

    private String getProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "index.html";
    }

    private String tooltip(ProjectAnalysisResults project) {
        CodeAnalysisResults analysis = project.getAnalysisResults();
        return analysis.getMetadata().getName() + "\n\n" +
                analysis.getContributorsAnalysisResults().getCommitsCount30Days() + " commits (30 days)" + "\n" +
                analysis.getContributorsAnalysisResults().getContributors()
                        .stream().filter(contributor -> contributor.getCommitsCount30Days() > 0).count() + " contributors (30 days)" + "\n" +
                FormattingUtils.formatCount(analysis.getMainAspectAnalysisResults().getLinesOfCode()) + " LOC";
    }

    private int getRecentContributorCount(List<ProjectAnalysisResults> projectsAnalysisResults) {
        Set<String> ids = new HashSet<>();
        projectsAnalysisResults.forEach(project -> {
            project.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream()
                    .filter(c -> c.getCommitsCount30Days() > 0).forEach(c -> ids.add(c.getEmail()));
        });
        return ids.size();
    }

}