/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import nl.obren.sokrates.sourcecode.filehistory.TemporalDependenciesHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileTemporalDependenciesReportGenerator {
    private static final Log LOG = LogFactory.getLog(FileTemporalDependenciesReportGenerator.class);

    private final CodeAnalysisResults codeAnalysisResults;
    // private int graphCounter = 1;
    private File reportsFolder;

    public FileTemporalDependenciesReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addTemporalDependenciesToReport(File reportsFolder, RichTextReport report) {
        this.reportsFolder = reportsFolder;
        report.addParagraph("A temporal dependency occurs when developers change two or more files " +
                "at the same time (i.e. they are a part of the same commit).", "margin-top: 12px; color: grey");

        int maxTemporalDependenciesDepthDays = codeAnalysisResults.getCodeConfiguration().getAnalysis().getMaxTemporalDependenciesDepthDays();

        report.startTabGroup();
        report.addTab("30_days", "Past 30 Days", true);
        if (maxTemporalDependenciesDepthDays >= 90) {
            report.addTab("90_days", "Past 3 Months", false);
        }
        if (maxTemporalDependenciesDepthDays >= 180) {
            report.addTab("180_days", "Past 6 Months", false);
        }
        if (maxTemporalDependenciesDepthDays > 180) {
            report.addTab("all_time", "Past " + maxTemporalDependenciesDepthDays + " Days", false);
        }
        report.endTabGroup();

        List<FilePairChangedTogether> filePairsChangedTogether = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether();
        List<FilePairChangedTogether> filePairsChangedTogether30Days = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether30Days();
        List<FilePairChangedTogether> filePairsChangedTogether90Days = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether90Days();
        List<FilePairChangedTogether> filePairsChangedTogether180Days = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether180Days();

        addTabContentSection(report, "30_days", filePairsChangedTogether30Days, true);
        if (maxTemporalDependenciesDepthDays >= 90) {
            addTabContentSection(report, "90_days", filePairsChangedTogether90Days, false);
        }
        if (maxTemporalDependenciesDepthDays >= 180) {
            addTabContentSection(report, "180_days", filePairsChangedTogether180Days, false);
        }
        if (maxTemporalDependenciesDepthDays > 180) {
            addTabContentSection(report, "all_time", filePairsChangedTogether, false);
        }
    }

    private void addTabContentSection(RichTextReport report, String id, List<FilePairChangedTogether> filePairs, boolean active) {
        report.startTabContentSection(id, active);
        addFileChangedTogetherList(report, filePairs);
        addDependenciesSection(report, filePairs, id);
        report.endTabContentSection();

    }

    private void addFileChangedTogetherList(RichTextReport report, List<FilePairChangedTogether> filePairs) {
        if (filePairs.size() == 0) {
            report.addParagraph("No file pairs changed together.");
            return;
        }
        final int maxListSize = codeAnalysisResults.getCodeConfiguration().getAnalysis().getMaxTopListSize();
        if (filePairs.size() > maxListSize) {
            filePairs = filePairs.subList(0, maxListSize);
        }
        report.addLineBreak();
        report.startSubSection("Files Most Frequently Changed Together (Top " + filePairs.size() + ")", "");
        report.addParagraph("<a href='../data/text/temporal_dependencies.txt' target='_blank'>data...</a>");
        addTable(report, filePairs);
        report.endSection();
    }

    private void addFileChangedTogetherInDifferentFoldersList(RichTextReport report, List<FilePairChangedTogether> filePairsChangedTogether) {
        List<FilePairChangedTogether> filePairs = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogetherInDifferentFolders(filePairsChangedTogether);
        if (filePairs.size() > 20) {
            filePairs = filePairs.subList(0, 20);
        }
        report.startSection("Files from Different Folders Most Frequently Changed Together (Top " + filePairs.size() + ")", "");
        report.addParagraph("<a href='../data/text/temporal_dependencies_different_folders.txt' target='_blank'>data...</a>");
        addTable(report, filePairs);
        report.endSection();
    }

    private void addTable(RichTextReport report, List<FilePairChangedTogether> filePairs) {
        report.startDiv("max-height: 400px; overflow-y: auto");
        report.startTable();
        report.addTableHeader("Pairs", "# same commits", "# commits 1", "# commits 2", "latest commit");
        filePairs.forEach(filePair -> {
            report.startTableRow();

            report.addTableCell(filePair.getSourceFile1().getRelativePath() + "<br/>" + filePair.getSourceFile2().getRelativePath());

            int commitsCount = filePair.getCommits().size();
            report.addTableCell("" + commitsCount, "text-align: center");
            int commitsCountFile1 = filePair.getCommitsCountFile1();
            report.addTableCell("" + commitsCountFile1 +
                    (commitsCountFile1 > 0 && commitsCountFile1 >= commitsCount
                            ? " (" + FormattingUtils.getFormattedPercentage(100.0 * commitsCount / commitsCountFile1) + "%)"
                            : ""));
            int commitsCountFile2 = filePair.getCommitsCountFile2();
            report.addTableCell("" + commitsCountFile2
                    + (commitsCountFile2 > 0 && commitsCountFile2 >= commitsCount
                    ? " (" + FormattingUtils.getFormattedPercentage(100.0 * commitsCount / commitsCountFile2) + "%)"
                    : ""));
            report.addTableCell("" + filePair.getLatestCommit());

            report.endTableRow();
        });
        report.endTable();
        report.endDiv();
    }

    private void addDependenciesSection(RichTextReport report, List<FilePairChangedTogether> filePairsChangedTogether, String id) {
        report.startDiv("margin: 10px;");

        report.startSubSection("Dependencies between files in same commits", "The number on the lines shows the number of shared commits.");
        renderFileDependencies(report, filePairsChangedTogether, id);
        report.endSection();
        report.endDiv();

        report.startDiv("margin: 10px;");
        int index[] = {0};
        codeAnalysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecompositionAnalysisResults -> {
            index[0] += 1;
            String logicalDecompositionKey = logicalDecompositionAnalysisResults.getKey();

            report.startSubSection("Dependencies between components in same commits (" + logicalDecompositionKey + ")",
                    "The number on the lines shows the number of shared commits.");

            TemporalDependenciesHelper helper = new TemporalDependenciesHelper();
            List<ComponentDependency> componentDependencies = helper.extractComponentDependencies(logicalDecompositionKey, filePairsChangedTogether);
            renderComponentDependencies(report, componentDependencies, "logical_decomposition_" + index[0] + "_" + id);

            report.endSection();
        });
        report.endDiv();
    }


    private void renderFileDependencies(RichTextReport report, List<FilePairChangedTogether> filePairsChangedTogether, String suffix) {
        ProcessingStopwatch.start("reporting/temporal dependencies/extract dependencies");
        TemporalDependenciesHelper dependenciesHelper = new TemporalDependenciesHelper();
        List<ComponentDependency> dependencies = dependenciesHelper.extractFileDependencies(filePairsChangedTogether);
        LOG.info("Extracted " + dependencies.size() + " dependencies");
        ProcessingStopwatch.end("reporting/temporal dependencies/extract dependencies");

        renderComponentDependencies(report, dependencies, "files_" + suffix);

        ProcessingStopwatch.start("reporting/temporal dependencies/extract dependencies with commits");
        List<ComponentDependency> dependenciesWithCommits = dependenciesHelper.extractDependenciesWithCommits(filePairsChangedTogether);
        LOG.info("Extracted " + dependenciesWithCommits.size() + " dependencies with commits");
        ProcessingStopwatch.end("reporting/temporal dependencies/extract dependencies with commits");
        if (dependenciesWithCommits.size() > 0) {
            ProcessingStopwatch.start("reporting/temporal dependencies/export graph");
            String graphId = "file_changed_together_dependencies_with_commits_components_" + suffix;
            Pair<String,String> force3DGraphFilePath = ForceGraphExporter.export3DForceGraph(dependenciesWithCommits, reportsFolder, graphId);
            report.addNewTabLink("Open 2D force graph (file dependencies with commits)...", force3DGraphFilePath.getFirst());
            report.addNewTabLink("Open 3D force graph (file dependencies with commits)...", force3DGraphFilePath.getSecond());
            report.addLineBreak();
            ProcessingStopwatch.end("reporting/temporal dependencies/export graph");
        }
    }

    private void renderComponentDependencies(RichTextReport report, List<ComponentDependency> dependencies, String suffix) {
        if (dependencies.size() > 0) {
            ProcessingStopwatch.start("reporting/temporal dependencies/graphviz");
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");
            graphvizDependencyRenderer.setArrowColor("#00688b");
            graphvizDependencyRenderer.setCyclicArrowColor("#a0a0a0");
            graphvizDependencyRenderer.setMaxNumberOfDependencies(50);
            String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), dependencies);

            String graphId = "file_changed_together_dependencies_" + suffix;
            report.addGraphvizFigure(graphId, "File changed together in different components", graphvizContent);

            VisualizationTools.addDownloadLinks(report, graphId);
            report.addLineBreak();
            Pair<String,String> force3DGraphFilePath = ForceGraphExporter.export3DForceGraph(dependencies, reportsFolder, graphId);
            report.addNewTabLink("Open 2D force graph (file dependencies)...", force3DGraphFilePath.getFirst());
            report.addNewTabLink("Open 3D force graph (file dependencies)...", force3DGraphFilePath.getSecond());
            report.addLineBreak();
            ProcessingStopwatch.end("reporting/temporal dependencies/graphviz");
        } else {
            report.addParagraph("No temporal dependencies found.");
        }
    }


}
