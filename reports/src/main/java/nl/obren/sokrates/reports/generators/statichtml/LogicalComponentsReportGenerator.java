/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.ReportConstants;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.dataexporters.DataExportUtils;
import nl.obren.sokrates.reports.dataexporters.DataExporter;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.reports.utils.ScopesRenderer;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.*;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyEvidence;
import nl.obren.sokrates.sourcecode.dependencies.DependencyUtils;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import nl.obren.sokrates.sourcecode.filehistory.TemporalDependenciesHelper;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LogicalComponentsReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;
    private final boolean forceSkipStaticDependencies;
    private boolean elaborate = true;
    private RichTextReport report;
    private int dependencyVisualCounter = 1;
    private int graphCounter = 1;

    public LogicalComponentsReportGenerator(CodeAnalysisResults codeAnalysisResults, boolean forceSkipStaticDependencies) {
        this.codeAnalysisResults = codeAnalysisResults;
        this.forceSkipStaticDependencies = forceSkipStaticDependencies;
    }

    public void addCodeOrganizationToReport(RichTextReport report) {
        this.report = report;
        addSummary();
        // addErrors();
        addFooter();
    }

    private void addFooter() {
        report.addLineBreak();
        report.addHorizontalLine();
        report.addParagraph(RichTextRenderingUtils.italic(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())));
    }

    private void addSummary() {
        if (forceSkipStaticDependencies) {
            report.addParagraph("An overview of source code logical components.", "margin-top: 12px; color: grey");
        } else {
            report.addParagraph("An overview of static code dependencies among source code logical components.", "margin-top: 12px; color: grey");
        }
        report.startSection("Intro", "");
        if (elaborate) {
            appendIntroduction();
        }
        report.endSection();

        report.startSection("Logical Decompositions Overview", "");
        List<LogicalDecompositionAnalysisResults> logicalDecompositionsAnalysisResults = codeAnalysisResults.getLogicalDecompositionsAnalysisResults();
        int size = logicalDecompositionsAnalysisResults.size();
        report.addParagraph("Analyzed system has <b>" + size + "</b> logical decomposition" + (size > 1 ? "s" : "") + ":");
        report.startUnorderedList();
        logicalDecompositionsAnalysisResults.forEach(logicalDecomposition -> {
            int componentsCount = logicalDecomposition.getComponents().size();
            report.addListItem(logicalDecomposition.getLogicalDecomposition().getName() + " (" + componentsCount + " component" + (componentsCount > 1 ? "s" : "") + ")");
        });
        report.endUnorderedList();
        report.endSection();

        report.startTabGroup();
        boolean active[] = {true};
        logicalDecompositionsAnalysisResults.forEach(logicalDecomposition -> {
            LogicalDecomposition decomposition = logicalDecomposition.getLogicalDecomposition();
            report.addTab(decomposition.getName(), decomposition.getName(), active[0]);
            active[0] = false;
        });
        report.endTabGroup();

        int[] sectionIndex = {1};
        active[0] = true;
        logicalDecompositionsAnalysisResults.forEach(logicalDecomposition -> {
            report.startTabContentSection(logicalDecomposition.getLogicalDecomposition().getName(), active[0]);
            analyzeLogicalDecomposition(sectionIndex[0], logicalDecomposition);
            active[0] = false;
            sectionIndex[0]++;
            report.endTabContentSection();
        });
        report.endSection();
    }

    private void analyzeLogicalDecomposition(int sectionIndex, LogicalDecompositionAnalysisResults logicalDecomposition) {
        report.addLevel2Header("Logical Decomposition #" + sectionIndex + ": " + logicalDecomposition.getKey().toUpperCase(), "margin-bottom: 0;");
        report.addParagraph(getDecompositionDescription(logicalDecomposition), "color: grey; margin-bottom: 24px;");
        report.startDiv("margin-top: -8px; margin-bottom: 18px;");
        report.addHtmlContent("<a target='_blank' href='visuals/bubble_chart_components_" + (sectionIndex) + ".html'>Bubble Chart</a> | ");
        report.addHtmlContent("<a target='_blank' href='visuals/tree_map_components_" + (sectionIndex) + ".html'>Tree Map</a>");
        report.endDiv();

        List<NumericMetric> fileCountPerComponent = logicalDecomposition.getFileCountPerComponent();
        List<NumericMetric> linesOfCodePerComponent = logicalDecomposition.getLinesOfCodePerComponent();

        ScopesRenderer renderer = new ScopesRenderer();
        renderer.setLinesOfCodeInMain(codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode());

        renderer.setTitle("Component Sizes (Lines of Code)");
        renderer.setDescription("");
        renderer.setFileCountPerComponent(fileCountPerComponent);

        renderer.setLinesOfCode(linesOfCodePerComponent);
        renderer.setMaxFileCount(codeAnalysisResults.getMaxFileCount());
        renderer.setMaxLinesOfCode(codeAnalysisResults.getMaxLinesOfCode());
        List<AspectAnalysisResults> components = logicalDecomposition.getComponents();
        String filePathPrefix = DataExportUtils.getComponentFilePrefix(logicalDecomposition.getKey());
        renderer.setAspectsFileListPaths(components.stream().map(aspect -> aspect.getAspect().getFileSystemFriendlyName(filePathPrefix)).collect(Collectors.toList()));
        renderer.renderReport(report, "The \"" + logicalDecomposition.getLogicalDecomposition().getName() + "\" logical decomposition has <b>" + logicalDecomposition.getLogicalDecomposition().getComponents().size() + "</b> components.");

        List<MetaRule> metaComponents = logicalDecomposition.getLogicalDecomposition().getMetaComponents();
        if (metaComponents.size() > 0) {
            report.startSubSection("Meta-Rules for Componentization", "");
            report.addListItem("The following explicit meta-rules for components are defined:");
            describeMetaRules(metaComponents);
            report.endSection();
        }

        if (!forceSkipStaticDependencies) {
            addStaticDependencies(logicalDecomposition);
        } else {
            if (codeAnalysisResults.getContributorsAnalysisResults().getCommitsCount() > 0) {
                report.startSubSection("Component Commits", "Components ordered by number of commits");
                addCommitsSection(logicalDecomposition);
                addCommitsTrendSection(sectionIndex, logicalDecomposition.getKey());
                addTemporalDependenciesSection(logicalDecomposition);
                report.endSection();
            }
        }

    }

    private void addStaticDependencies(LogicalDecompositionAnalysisResults logicalDecomposition) {
        List<ComponentDependency> componentDependencies = logicalDecomposition.getComponentDependencies();
        report.startSubSection("Static Dependencies", "Dependencies among components are <b>static</b> code dependencies among files in different components.");
        if (componentDependencies != null && componentDependencies.size() > 0) {
            addComponentDependenciesSection(logicalDecomposition, componentDependencies);
        } else {
            report.addParagraph("No component dependencies found.");
        }
        report.endSection();
    }

    private void addTemporalDependenciesSection(LogicalDecompositionAnalysisResults logicalDecomposition) {
        List<FilePairChangedTogether> filePairsChangedTogether180Days = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether180Days();

        addDependenciesSection(filePairsChangedTogether180Days);
    }

    private void addDependenciesSection(List<FilePairChangedTogether> filePairsChangedTogether) {
        report.startDiv("margin: 10px;");
        codeAnalysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecompositionAnalysisResults -> {
            String logicalDecompositionKey = logicalDecompositionAnalysisResults.getKey();

            report.startSubSection("Dependencies between components in same commits (past 180 days)",
                    "The number on the lines shows the number of shared commits.");

            report.addNewTabLink("See detailed temporal dependencies report...", "FileTemporalDependencies.html");
            report.addLineBreak();
            report.addLineBreak();
            TemporalDependenciesHelper helper = new TemporalDependenciesHelper();
            List<ComponentDependency> componentDependencies = helper.extractComponentDependencies(logicalDecompositionKey, filePairsChangedTogether);
            renderComponentDependencies(report, componentDependencies);

            report.endSection();
        });
        report.endDiv();
    }

    private void renderComponentDependencies(RichTextReport report, List<ComponentDependency> dependencies) {
        if (dependencies.size() > 0) {
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");
            graphvizDependencyRenderer.setArrowColor("#00688b");
            graphvizDependencyRenderer.setCyclicArrowColor("#a0a0a0");
            graphvizDependencyRenderer.setMaxNumberOfDependencies(50);
            String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), dependencies);

            String graphId = "logical_decomposition_file_changed_together_dependencies_" + graphCounter++;
            report.addGraphvizFigure(graphId, "File changed together in different components", graphvizContent);
        } else {
            report.addParagraph("No temporal dependencies found.");
        }
    }

    private void addCommitsSection(LogicalDecompositionAnalysisResults logicalDecomposition) {
        List<NumericMetric> fileCountPerComponent = logicalDecomposition.getFileCountPerComponent();
        List<NumericMetric> linesOfCodePerComponent = new CommitTrendsExtractors(codeAnalysisResults).getTotalCommits(logicalDecomposition.getKey());
        ScopesRenderer renderer = new ScopesRenderer();
        renderer.setLinesOfCodeInMain(codeAnalysisResults.getContributorsAnalysisResults().getCommitsCount());

        renderer.setTitle("Total Commits per Component");
        renderer.setDescription("");
        renderer.setMetric("commits");
        renderer.setDescribe(false);
        renderer.setActiveColor("grey");
        renderer.setFileCountPerComponent(fileCountPerComponent);

        renderer.setLinesOfCode(linesOfCodePerComponent);
        renderer.setMaxFileCount(codeAnalysisResults.getMaxFileCount());
        renderer.setMaxLinesOfCode(codeAnalysisResults.getContributorsAnalysisResults().getCommitsCount());
        List<AspectAnalysisResults> components = logicalDecomposition.getComponents();
        String filePathPrefix = DataExportUtils.getComponentFilePrefix(logicalDecomposition.getKey());
        renderer.setAspectsFileListPaths(components.stream().map(aspect -> aspect.getAspect().getFileSystemFriendlyName(filePathPrefix)).collect(Collectors.toList()));
        renderer.renderReport(report, "All commits, some commits may include files from multiple components.");
    }

    private void addCommitsTrendSection(int sectionIndex, String key) {
        report.startSubSection("Yearly File Updates Trend per Components", "The number of file changes in commits");
        report.addContentInDiv(ReportConstants.ANIMATION_SVG_ICON, "display: inline-block; vertical-align: middle; margin: 4px;");
        report.addHtmlContent("animated commit history: ");
        report.addNewTabLink("all time cumulative", "visuals/racing_charts_component_commits_" + sectionIndex + ".html?tickDuration=600");
        report.addHtmlContent(" | ");
        report.addNewTabLink("12 months window", "visuals/racing_charts_component_commits_12_months_window_" + sectionIndex + ".html?tickDuration=600");

        report.startTable();
        report.startTableRow();
        report.startTableCell("border: none");
        List<HistoryPerExtension> historyPerExtensionPerYear = new ArrayList<>();
        Map<String, Map<String, Integer>> commitsPerYear = new CommitTrendsExtractors(codeAnalysisResults).getCommitsPerYear(key);
        commitsPerYear.keySet().forEach(component -> {
            Map<String, Integer> componentYears = commitsPerYear.get(component);
            componentYears.keySet().forEach(year -> {
                int count = componentYears.get(year);
                historyPerExtensionPerYear.add(new HistoryPerExtension(component, year, count));
            });
        });
        List<String> componentNames = new ArrayList<>(commitsPerYear.keySet());
        HistoryPerLanguageGenerator.getInstanceCommits(historyPerExtensionPerYear, componentNames).addHistoryPerComponent(report);
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addLineBreak();
        report.addLineBreak();
        report.endSection();
    }

    private void addComponentDependenciesSection(LogicalDecompositionAnalysisResults logicalDecomposition, List<ComponentDependency> componentDependencies) {
        report.startUnorderedList();
        report.addListItem("Analyzed system has <b>" + componentDependencies.size() + "</b> links (arrows) between components.");
        report.addListItem("The number on the arrow represents the number of files from referring component that depend on files in referred component.");
        report.addListItem("These " + componentDependencies.size() + " links contain <a href='../data/text/" + DataExporter.dependenciesFileNamePrefix("", "", logicalDecomposition.getKey()) + ".txt'><b>" + DependencyUtils.getDependenciesCount(componentDependencies) + "</b> dependencies</a>.");
        int cyclicDependencyPlacesCount = DependencyUtils.getCyclicDependencyPlacesCount(componentDependencies);
        int cyclicDependencyCount = DependencyUtils.getCyclicDependencyCount(componentDependencies);
        if (cyclicDependencyPlacesCount > 0) {
            String numberOfPlacesText = cyclicDependencyPlacesCount == 1
                    ? "is <b>1</b> place"
                    : "are <b>" + cyclicDependencyPlacesCount + "</b> places";
            report.addListItem("There " + numberOfPlacesText + " (" + (cyclicDependencyPlacesCount * 2) + " links) with <b>cyclic</b> dependencies (<b>" + cyclicDependencyCount + "</b> " +
                    "file dependencies).");
        }

        describeDependencyFinder(logicalDecomposition);

        report.endUnorderedList();

        List<String> componentNames = new ArrayList<>();
        logicalDecomposition.getComponents().forEach(c -> componentNames.add(c.getName()));
        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        RenderingOptions renderingOptions = logicalDecomposition.getLogicalDecomposition().getRenderingOptions();
        graphvizDependencyRenderer.setMaxNumberOfDependencies(renderingOptions.getMaxNumberOfDependencies());
        graphvizDependencyRenderer.setOrientation(renderingOptions.getOrientation());
        graphvizDependencyRenderer.setReverseDirection(renderingOptions.isReverseDirection());

        boolean renderWithoutDependencies = renderingOptions.isRenderComponentsWithoutDependencies();
        int linkThreshold = logicalDecomposition.getLogicalDecomposition().getDependencyLinkThreshold();
        List<ComponentDependency> dependenciesAboveThreshold = componentDependencies.stream().filter(d -> d.getCount() >= linkThreshold).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> componentsAboveThreshold = componentNames.stream()
                .filter(c -> renderWithoutDependencies || isComponentInDependency(dependenciesAboveThreshold, c))
                .collect(Collectors.toCollection(ArrayList::new));
        List<ComponentGroup> componentGroups = getComponentGroups(logicalDecomposition, dependenciesAboveThreshold, componentsAboveThreshold);
        String graphId = addDependencyGraphVisuals(dependenciesAboveThreshold, componentsAboveThreshold, componentGroups, graphvizDependencyRenderer);
        report.addLineBreak();
        report.addNewTabLink("Open 2D force graph...", "visuals/force_2d_" + graphId + ".html");
        report.addLineBreak();
        report.addNewTabLink("Open 3D force graph...", "visuals/force_3d_" + graphId + ".html");

        if (componentGroups.size() > 0) {
            report.addLevel4Header("Group Dependencies");
            List<ComponentDependency> groupDependencies = getGroupDependencies(dependenciesAboveThreshold, componentGroups);
            addDependencyGraphVisuals(groupDependencies,
                    componentsAboveThreshold.stream().filter(c -> c.equalsIgnoreCase(getGroup(c, componentGroups))).collect(Collectors.toCollection(ArrayList::new)),
                    new ArrayList<>(), graphvizDependencyRenderer);
            report.addLineBreak();
        }

        report.addLineBreak();
        report.addLineBreak();
        addMoreDetailsSection(logicalDecomposition, componentDependencies);
        report.addLineBreak();
        report.addLineBreak();

        renderIndirectDependencies(componentNames, graphvizDependencyRenderer, renderingOptions, renderWithoutDependencies, dependenciesAboveThreshold);
        report.addLineBreak();
        report.addLineBreak();
        report.addLineBreak();
    }

    private List<ComponentGroup> getComponentGroups(LogicalDecompositionAnalysisResults logicalDecomposition, List<ComponentDependency> dependencies, List<String> components) {
        List<ComponentGroup> componentGroups = new ArrayList<>();
        List<String> allNames = new ArrayList<>();
        Consumer<String> stringConsumer = component -> {
            if (!allNames.contains(component)) {
                allNames.add(component);
            }
        };
        components.forEach(stringConsumer);
        dependencies.forEach(dependency -> {
            stringConsumer.accept(dependency.getFromComponent());
            stringConsumer.accept(dependency.getToComponent());
        });

        List<String> componentsInAnyGroup = new ArrayList<>();
        logicalDecomposition.getLogicalDecomposition().getGroups().forEach(groupingRule -> {
            List<String> componentsInGroupFiltered = allNames.stream().filter(name -> RegexUtils.matchesEntirely(groupingRule.getPattern(), name)).collect(Collectors.toCollection(ArrayList::new));
            List<String> componentsInGroup = new ArrayList<>();
            componentsInGroupFiltered.forEach(c -> {
                if (!componentsInAnyGroup.contains(c)) {
                    componentsInGroup.add(c);
                    componentsInAnyGroup.add(c);
                }
            });
            if (componentsInGroup.size() > 0) {
                componentGroups.add(new ComponentGroup(groupingRule.getName(), componentsInGroup));
            }
        });

        return componentGroups;
    }

    private List<ComponentDependency> getGroupDependencies(List<ComponentDependency> dependencies, List<ComponentGroup> componentGroups) {
        List<ComponentDependency> groupDependencies = new ArrayList<>();
        Map<String, ComponentDependency> groupDependenciesMap = new HashMap<>();

        dependencies.forEach(dependency -> {
            String from = getGroup(dependency.getFromComponent(), componentGroups);
            String to = getGroup(dependency.getToComponent(), componentGroups);

            String key1 = from + "::" + to;
            String key2 = to + "::" + from;

            if (groupDependenciesMap.containsKey(key1)) {
                groupDependenciesMap.get(key1).setCount(groupDependenciesMap.get(key1).getCount() + dependency.getCount());
            } else if (groupDependenciesMap.containsKey(key2)) {
                groupDependenciesMap.get(key2).setCount(groupDependenciesMap.get(key1).getCount() + dependency.getCount());
            } else {
                ComponentDependency newDependency = new ComponentDependency(from, to);
                newDependency.setCount(dependency.getCount());
                groupDependencies.add(newDependency);
                groupDependenciesMap.put(key1, newDependency);
            }
        });

        return groupDependencies;
    }

    private String getGroup(String component, List<ComponentGroup> componentGroups) {
        for (ComponentGroup componentGroup : componentGroups) {
            if (componentGroup.getComponentNames().contains(component)) {
                return componentGroup.getName() + " (" + componentGroup.getComponentNames().size() + ")";
            }
        }

        return component;
    }

    private void renderIndirectDependencies(List<String> componentNames, GraphvizDependencyRenderer graphvizDependencyRenderer, RenderingOptions renderingOptions, boolean renderWithoutDependencies, List<ComponentDependency> dependencies) {
        if (renderingOptions.isRenderIndirectDependencies()) {
            report.startSubSection("Indirect Dependencies", "Dependecies via shared target or source components.");
            renderIndirectDependencies("Shared Targets", componentNames, graphvizDependencyRenderer, renderWithoutDependencies, getIndirectViaTargetsDependencies(dependencies));
            renderIndirectDependencies("Shared Sources", componentNames, graphvizDependencyRenderer, renderWithoutDependencies, getIndirectViaSourcesDependencies(dependencies));

            if (renderingOptions.isRenderInternalIndirectDependencies()) {
                renderInternalsOfIndirectDependencies("Targets", componentNames, graphvizDependencyRenderer, renderWithoutDependencies, getDependenciesWithSharedTargets(dependencies));
                renderInternalsOfIndirectDependencies("Sources", componentNames, graphvizDependencyRenderer, renderWithoutDependencies, getDependenciesWithSharedSources(dependencies));
            }
            report.endSection();
        }
    }

    private void renderIndirectDependencies(String type, List<String> componentNames, GraphvizDependencyRenderer graphvizDependencyRenderer, boolean renderWithoutDependencies, List<ComponentDependency> indirectDependencies) {
        if (indirectDependencies.size() > 0) {
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");

            report.addLevel3Header("Indirect Dependencies (" + type + ")");
            report.startShowMoreBlock("show details...");
            report.addParagraph("Dependencies via " + type.toLowerCase() + "  components.", "color: grey");
            addDependencyGraphVisuals(indirectDependencies, componentNames.stream()
                    .filter(c -> renderWithoutDependencies || isComponentInDependency(indirectDependencies, c))
                    .collect(Collectors.toCollection(ArrayList::new)), new ArrayList<>(), graphvizDependencyRenderer);

            report.endShowMoreBlock();
            report.addLineBreak();
            report.addLineBreak();
            report.addLineBreak();
        }
    }

    private void renderInternalsOfIndirectDependencies(String type, List<String> componentNames, GraphvizDependencyRenderer graphvizDependencyRenderer, boolean renderWithoutDependencies, List<ComponentDependency> sharedTargetDependencies) {
        if (sharedTargetDependencies.size() > 0) {
            graphvizDependencyRenderer.setType("digraph");
            graphvizDependencyRenderer.setArrow("->");
            report.addLevel3Header("Indirect Dependencies With Shared " + type + " Visible");
            report.startShowMoreBlock("show details...");
            report.addParagraph("Shared target components made visible.", "color: grey");
            addDependencyGraphVisuals(sharedTargetDependencies, componentNames.stream()
                    .filter(c -> renderWithoutDependencies || isComponentInDependency(sharedTargetDependencies, c))
                    .collect(Collectors.toCollection(ArrayList::new)), new ArrayList<>(), graphvizDependencyRenderer);
            report.endShowMoreBlock();
            report.addLineBreak();
            report.addLineBreak();
            report.addLineBreak();
        }
    }

    private List<ComponentDependency> getIndirectViaTargetsDependencies(List<ComponentDependency> dependencies) {
        ArrayList<ComponentDependency> indirect = new ArrayList<>();
        Map<String, ComponentDependency> map = new HashMap<>();

        List<String> alreadyCovered = new ArrayList<>();

        getMergedDependencies(dependencies).forEach(dependency1 -> {
            dependencies.stream()
                    .filter(dependency2 -> !dependency1.getFromComponent().equals(dependency2.getFromComponent()))
                    .filter(dependency2 -> dependency1.getToComponent().equalsIgnoreCase(dependency2.getToComponent()))
                    .forEach(dependency2 -> {
                        String coverKey1 = dependency1.getFromComponent() + " --" + dependency1.getToComponent() + "--> " + dependency2.getFromComponent();
                        String coverKey2 = dependency2.getFromComponent() + " --" + dependency1.getToComponent() + "--> " + dependency1.getFromComponent();
                        if (!(alreadyCovered.contains(coverKey1) || alreadyCovered.contains(coverKey2))) {
                            alreadyCovered.add(coverKey1);
                            addIndirectInternalDependency(indirect, map, dependency1.getFromComponent(), dependency2.getFromComponent());
                        }
                    });
        });

        return indirect;
    }

    private List<ComponentDependency> getMergedDependencies(List<ComponentDependency> dependencies) {
        Map<String, ComponentDependency> mergeMap = new HashMap<>();
        List<ComponentDependency> mergedDependencies = new ArrayList<>();
        dependencies.forEach(dependency -> {
            String key = dependency.getFromComponent() + " -> " + dependency.getToComponent();
            if (mergeMap.containsKey(key)) {
                mergeMap.get(key).increment(dependency.getCount());
            } else {
                ComponentDependency newDependency = new ComponentDependency();
                newDependency.setFromComponent(dependency.getFromComponent());
                newDependency.setToComponent(dependency.getToComponent());
                newDependency.setCount(1);
                mergedDependencies.add(newDependency);
                mergeMap.put(key, newDependency);
            }
        });
        return mergedDependencies;
    }

    private List<ComponentDependency> getIndirectViaSourcesDependencies(List<ComponentDependency> dependencies) {
        ArrayList<ComponentDependency> indirect = new ArrayList<>();
        Map<String, ComponentDependency> map = new HashMap<>();

        List<String> alreadyCovered = new ArrayList<>();

        getMergedDependencies(dependencies).forEach(dependency1 -> {
            dependencies.stream()
                    .filter(dependency2 -> !dependency1.getToComponent().equals(dependency2.getToComponent()))
                    .filter(dependency2 -> dependency1.getFromComponent().equalsIgnoreCase(dependency2.getFromComponent()))
                    .forEach(dependency2 -> {
                        String coverKey1 = dependency1.getToComponent() + " --" + dependency1.getFromComponent() + "--> " + dependency2.getToComponent();
                        String coverKey2 = dependency2.getToComponent() + " --" + dependency1.getFromComponent() + "--> " + dependency1.getToComponent();
                        if (!(alreadyCovered.contains(coverKey1) || alreadyCovered.contains(coverKey2))) {
                            alreadyCovered.add(coverKey1);
                            addIndirectInternalDependency(indirect, map, dependency1.getToComponent(), dependency2.getToComponent());
                        }
                    });
        });

        return indirect;
    }

    private void addIndirectInternalDependency(ArrayList<ComponentDependency> indirect, Map<String, ComponentDependency> map, String component1, String component2) {
        String key1 = component1 + "::" + component2;
        String key2 = component2 + "::" + component1;

        if (map.containsKey(key1)) {
            map.get(key1).increment(1);
        } else if (map.containsKey(key2)) {
            map.get(key2).increment(1);
        } else {
            ComponentDependency indirectDependency = new ComponentDependency(component1, component2);
            map.put(key1, indirectDependency);
            indirect.add(indirectDependency);
        }
    }

    private List<ComponentDependency> getDependenciesWithSharedTargets(List<ComponentDependency> dependencies) {
        Map<String, Set<String>> map = new HashMap<>();
        dependencies.forEach(d -> {
            String to = d.getToComponent();
            if (!map.containsKey(to)) {
                map.put(to, new HashSet<>());
            }
            map.get(to).add(d.getFromComponent());
        });
        return dependencies.stream().filter(d -> map.get(d.getToComponent()).size() > 1).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<ComponentDependency> getDependenciesWithSharedSources(List<ComponentDependency> dependencies) {
        Map<String, Set<String>> map = new HashMap<>();
        dependencies.forEach(d -> {
            String from = d.getFromComponent();
            if (!map.containsKey(from)) {
                map.put(from, new HashSet<>());
            }
            map.get(from).add(d.getToComponent());
        });
        return dependencies.stream().filter(d -> map.get(d.getFromComponent()).size() > 1).collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean isComponentInDependency(List<ComponentDependency> dependencies, String component) {
        for (ComponentDependency dependency : dependencies) {
            if (dependency.getFromComponent().equalsIgnoreCase(component)) {
                return true;
            }
            if (dependency.getToComponent().equalsIgnoreCase(component)) {
                return true;
            }
        }

        return false;
    }

    private String addDependencyGraphVisuals(List<ComponentDependency> componentDependencies, List<String> componentNames, List<ComponentGroup> componentGroups, GraphvizDependencyRenderer graphvizDependencyRenderer) {
        String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(componentNames, componentDependencies, componentGroups);
        String graphId = "dependencies_" + dependencyVisualCounter++;
        report.startDiv("max-height: 600px; overflow-y: scroll; overflow-x: scroll;");
        report.addGraphvizFigure(graphId, "", graphvizContent);
        report.endDiv();
        report.addLineBreak();
        report.addLineBreak();
        VisualizationTools.addDownloadLinks(report, graphId);

        return graphId;
    }

    private void addMoreDetailsSection(LogicalDecompositionAnalysisResults logicalDecomposition, List<ComponentDependency> componentDependencies) {
        report.startShowMoreBlock("Show more details about dependencies...");
        report.startDiv("width: 100%; overflow-x: auto; max-height: 600px");
        report.startTable();
        report.addTableHeader("From Component<br/>&nbsp;--> To Component", "From Component<br/>(files with dependencies)", "Details");
        Collections.sort(componentDependencies, (o1, o2) -> o2.getCount() - o1.getCount());
        componentDependencies.forEach(componentDependency -> {
            addDependencyRow(logicalDecomposition, componentDependency);
        });
        report.endTable();
        report.endDiv();
        report.endShowMoreBlock();
    }

    private void describeDependencyFinder(LogicalDecompositionAnalysisResults logicalDecomposition) {
        List<DependencyFinderPattern> rules = logicalDecomposition.getLogicalDecomposition().getDependenciesFinder().getRules();
        if (rules.size() > 0) {
            report.addListItem("The following explicit rules for finding dependencies are defined:");
            report.startUnorderedList();
            rules.forEach(rule -> {
                SourceFileFilter filter = new SourceFileFilter(rule.getPathPattern(), rule.getContentPattern());
                report.addListItem("\"" + rule.getComponent() + "\" <== " + filter.toString());
            });
            report.endUnorderedList();
        }
        List<? extends MetaRule> metaRules = logicalDecomposition.getLogicalDecomposition().getDependenciesFinder().getMetaRules();
        if (metaRules.size() > 0) {
            report.addListItem("The following explicit meta-rules for finding dependencies are defined:");
            describeMetaRules(metaRules);
        }
    }

    private void describeMetaRules(List<? extends MetaRule> metaRules) {
        report.startUnorderedList();
        metaRules.forEach(rule -> {
            SourceFileFilter filter = new SourceFileFilter(rule.getPathPattern(), rule.getContentPattern());
            report.addListItem(filter.toString());
            report.startUnorderedList();
            rule.getNameOperations().forEach(op -> {
                StringBuilder params = new StringBuilder();
                op.getParams().forEach(param -> {
                    if (params.length() > 0) params.append(", ");
                    params.append("\"" + param + "\"");
                });
                report.addListItem(op.getOp() + " (" + params.toString() + ")");
            });
            report.endUnorderedList();
        });
        report.endUnorderedList();
    }

    private void addDependencyRow(LogicalDecompositionAnalysisResults logicalDecomposition, ComponentDependency componentDependency) {
        report.startTableRow();
        report.addTableCell(componentDependency.getFromComponent() + "<br/>&nbsp&nbsp;-->&nbsp" + componentDependency.getToComponent()
        );
        report.addHtmlContent("<td>");
        int locFromDuplications = componentDependency.getLocFrom();
        NamedSourceCodeAspect fromComponentByName = logicalDecomposition.getLogicalDecomposition().getComponentByName(componentDependency.getFromComponent());
        String percentageHtmlFragment = null;
        int dependencyCount = componentDependency.getCount();
        if (fromComponentByName != null) {
            percentageHtmlFragment = getFromDependencyCoverageSvg(locFromDuplications, fromComponentByName, dependencyCount);
        }

        report.addShowMoreBlock("",
                "<textarea style='width:90%; height: 20em;'>"
                        + componentDependency.getEvidence().stream().map(DependencyEvidence::getPathFrom).collect(Collectors.joining("\n")) +
                        "</textarea>",
                (percentageHtmlFragment != null ? "" + percentageHtmlFragment : dependencyCount + " files (" + locFromDuplications + " LOC)<br/>")
        );
        report.addHtmlContent("</td>");
        report.addTableCell("<a href='../data/text/" + DataExporter.dependenciesFileNamePrefix(componentDependency.getFromComponent(), componentDependency.getToComponent(), logicalDecomposition.getKey()) + ".txt'><b>" + dependencyCount + "</b> source " + (dependencyCount == 1 ? "file" : "files") + "</a>");
        report.endTableRow();
    }

    private String getFromDependencyCoverageSvg(int locFromDuplications, NamedSourceCodeAspect fromComponentByName, int dependencyCount) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(320);
        chart.setMaxBarWidth(100);
        chart.setBarHeight(14);
        chart.setBarStartXOffset(2);
        chart.setSmallerFontSize();

        double percentage = 100.0 * locFromDuplications / fromComponentByName.getLinesOfCode();
        String percentageText = FormattingUtils.getFormattedPercentage(percentage) + "%";

        String textRight = dependencyCount + " "
                + (dependencyCount == 1 ? "file" : "files") + ", "
                + locFromDuplications + " LOC (" + percentageText + ")";

        return chart.getPercentageSvg(percentage, "", textRight);
    }

    private String getDecompositionDescription(LogicalDecompositionAnalysisResults logicalDecomposition) {
        int numberOfComponents = logicalDecomposition.getComponents().size();
        StringBuilder description = new StringBuilder();
        LogicalDecomposition definition = logicalDecomposition.getLogicalDecomposition();
        if (definition.getComponentsFolderDepth() > 0) {
            if (definition.getMinComponentsCount() <= 1) {
                description.append("The decompositions is based on the folder structure at <b>level " + definition.getComponentsFolderDepth() + "</b> (relative to the source code root).");
            } else {
                description.append("The decompositions is based on the folder structure (relative to the source code root), with automatically defined folder depth to have at least " + definition.getMinComponentsCount() + " components.");
            }
        } else if (logicalDecomposition.getComponents() != null && numberOfComponents > 0) {
            description.append("The \"" + definition.getName() + "\" logical decomposition in based on <b>" + numberOfComponents + "</b> explicitly defined components.");
        }
        return description.toString();
    }

    private void appendIntroduction() {
        report.addParagraph(getShortIntro());
        report.addHtmlContent(getLongIntro());

        if (!forceSkipStaticDependencies) {
            report.startShowMoreBlock("Learn more...");
            report.startUnorderedList();
            report.addListItem("To learn more about good practices on componentization and dependencies, Sokrates recommends the following resources:");
            report.startUnorderedList();
            report.addListItem("<a target='_blank' href='https://www.martinfowler.com/ieeeSoftware/coupling.pdf'>Reduce Coupling</a>, MartinFlower.com (IEEE Software article)");
            report.addListItem("<a target='_blank' href='https://sourcemaking.com/refactoring/smells/couplers'>Couplers Code Smells</a>, sourcemaking.com");
            report.endUnorderedList();
            report.endUnorderedList();
        }

        report.endShowMoreBlock();
    }

    private String getShortIntro() {
        String shortIntro = "";
        if (forceSkipStaticDependencies) {
            shortIntro += "<b>Logical decomposition</b> is a representation of the organization of the <b>main</b> source code, where every and each file is put in exactly one <b>logical component</b>.";
        } else {
            shortIntro = "<b>Static code dependencies</b> refer to the relationships between different modules, libraries, or components in a software system that are established at compile-time rather than at runtime. These dependencies are determined by the code itself, meaning that one module directly references another through imports, includes, or explicit references in the code. Examples include:";
        }
        return shortIntro;
    }

    private String getLongIntro() {
        String longIntro = "";
        if (forceSkipStaticDependencies) {
            longIntro = "<ul>\n";
            longIntro += "<li>A software system can have <b>one</b> or <b>more</b> logical decompositions.</li>\n";
            longIntro += "<li>A logical decomposition can be defined in two ways in Sokrates.</li>\n";
            longIntro += "<li>First approach is based on the <b>folders structure</b>. " +
                    "Components are mapped to folders at defined <b>folder depth</b> relative to the source code root.</li>\n";
            longIntro += "<li>Second approach is based on <b>explicit</b> definition of each component. In such explicit definitions, components are explicitly <b>named</b> and their files are selected based on explicitly defined path and content <b>filters</b>.</li>\n";
            longIntro += "<li>A logical decomposition is considered <b>invalid</b> if a file is selected into <b>two or more components</b>. " +
                    "This constraint is introduced in order to facilitate measuring of <b>dependencies</b> among components.</li>\n";
            longIntro += "<li>Files not assigned to any component are put into a special \"<b>Unclassified</b>\" component.</li>\n";

            longIntro += "</ul>\n";
        } else {
            longIntro = "<ul>\n";
            longIntro += "<li>Library and Module Imports</li>\n";
            longIntro += "<li>Class Dependencies (Inheritance & Composition)</li>\n";
            longIntro += "<li>Function and Method Calls</li>\n";
            longIntro += "<li>Dependency Injection Without Runtime Resolution</li>\n";

            longIntro += "</ul>\n";
        }

        return longIntro;
    }


}
