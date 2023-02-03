/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributorsImport;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.io.File;
import java.util.List;

import static nl.obren.sokrates.sourcecode.landscape.ContributorConnectionUtils.getPeopleDependencies;
import static nl.obren.sokrates.sourcecode.landscape.ContributorConnectionUtils.getPeopleFileDependencies;

public class ContributorsAnalyzer extends Analyzer {
    private CodeConfiguration codeConfiguration;
    private MetricsList metricsList;
    private CodeAnalysisResults codeAnalysisResults;
    private File sokratesFolder;

    private ContributorsAnalysisResults analysisResults;

    public ContributorsAnalyzer(CodeAnalysisResults results, File sokratesFolder) {
        this.analysisResults = results.getContributorsAnalysisResults();
        this.codeConfiguration = results.getCodeConfiguration();
        this.metricsList = results.getMetricsList();
        codeAnalysisResults = results;
        this.sokratesFolder = sokratesFolder;
    }

    public void analyze() {
        FileHistoryAnalysisConfig fileHistoryAnalysisConfig = codeConfiguration.getFileHistoryAnalysis();
        if (fileHistoryAnalysisConfig.filesHistoryImportPathExists(sokratesFolder)) {
            ProcessingStopwatch.start("analysis/contributors/loading");
            ContributorsImport contributorsImport = fileHistoryAnalysisConfig.getContributors(sokratesFolder, fileHistoryAnalysisConfig);
            analysisResults.setLatestCommitDate(contributorsImport.getLatestCommitDate());
            analysisResults.setContributors(contributorsImport.getContributors());
            analysisResults.setContributorsPerYear(contributorsImport.getContributorsPerYear());
            analysisResults.setContributorsPerMonth(contributorsImport.getContributorsPerMonth());
            analysisResults.setContributorsPerWeek(contributorsImport.getContributorsPerWeek());
            analysisResults.setContributorsPerDay(contributorsImport.getContributorsPerDay());
            ProcessingStopwatch.end("analysis/contributors/loading");
            ProcessingStopwatch.start("analysis/contributors/per extension");
            analysisResults.setCommitsPerExtensions(fileHistoryAnalysisConfig.getCommitsPerExtension(sokratesFolder, fileHistoryAnalysisConfig));
            ProcessingStopwatch.end("analysis/contributors/per extension");

            ProcessingStopwatch.start("analysis/contributors/get people file dependencies");
            analysisResults.setPeopleFileDependencies30Days(getPeopleFileDependencies(codeAnalysisResults, 30));
            analysisResults.setPeopleFileDependencies90Days(getPeopleFileDependencies(codeAnalysisResults, 90));
            analysisResults.setPeopleFileDependencies180Days(getPeopleFileDependencies(codeAnalysisResults, 180));
            analysisResults.setPeopleFileDependencies365Days(getPeopleFileDependencies(codeAnalysisResults, 365));
            ProcessingStopwatch.end("analysis/contributors/get people file dependencies");

            ProcessingStopwatch.start("analysis/contributors/get people dependencies");
            analysisResults.setPeopleDependencies30Days(getPeopleDependencies(codeAnalysisResults, 30));
            analysisResults.setPeopleDependencies90Days(getPeopleDependencies(codeAnalysisResults, 90));
            analysisResults.setPeopleDependencies180Days(getPeopleDependencies(codeAnalysisResults, 180));
            analysisResults.setPeopleDependencies365Days(getPeopleDependencies(codeAnalysisResults, 365));
            ProcessingStopwatch.end("analysis/contributors/get people dependencies");

            addMetrics();
        }
    }

    private void addMetrics() {
        metricsList.addSystemMetric().id("NUMBER_OF_CONTRIBUTORS")
                .value(analysisResults.getContributors().size())
                .description("Number of contributors");
    }

}
