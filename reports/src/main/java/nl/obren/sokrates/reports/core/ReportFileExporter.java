/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;

public class ReportFileExporter {
    private static String htmlReportsSubFolder = "html";

    public static void exportHtml(File folder, String subFolder, RichTextReport report, String customHeaderFragment) {
        htmlReportsSubFolder = subFolder;
        File htmlReportsFolder = getHtmlReportsFolder(folder);
        String reportFileName = getReportFileName(report);
        export(htmlReportsFolder, report, reportFileName, customHeaderFragment);
    }

    private static void export(File folder, RichTextReport report, String reportFileName, String customHeaderFragment) {
        File reportFile = new File(folder, reportFileName);
        try {
            PrintWriter out = new PrintWriter(reportFile);
            String reportsHtmlHeader = ReportConstants.REPORTS_HTML_HEADER.replace("<title></title>", "<title>" +
                    report.getDisplayName().replaceAll("<.*?>", "").replaceAll("\\[.*?\\]", "") + "</title>");
            reportsHtmlHeader = reportsHtmlHeader.replace("<!-- CUSTOM HEADER FRAGMENT -->", customHeaderFragment);
            if (report.isEmbedded()) {
                reportsHtmlHeader = reportsHtmlHeader.replace(" ${margin-left}", "0");
                reportsHtmlHeader = reportsHtmlHeader.replace(" ${margin-right}", "0");
            } else {
                reportsHtmlHeader = reportsHtmlHeader.replace(" ${margin-left}", "5%");
                reportsHtmlHeader = reportsHtmlHeader.replace(" ${margin-right}", "5%");
            }
            reportsHtmlHeader = minimize(reportsHtmlHeader);
            out.println(reportsHtmlHeader + "\n<body><div id=\"report\">\n" + "\n");
            new ReportRenderer().render(report, getReportRenderingClient(out, folder));
            out.println("</div>\n</body>\n</html>");
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static String minimize(String html) {
        html = StringUtils.replace(html, "  ", " ");
        html = StringUtils.replace(html, "\n\n", "\n");
        return html;
    }

    private static ReportRenderingClient getReportRenderingClient(PrintWriter out, File reportsFolder) {
        return new ReportRenderingClient() {
            @Override
            public void append(String text) {
                out.println(text);
            }

            @Override
            public File getVisualsExportFolder() {
                File visualsFolder = new File(reportsFolder, "visuals");
                visualsFolder.mkdirs();
                return visualsFolder;
            }
        };
    }

    private static String getReportFileName(RichTextReport report) {
        return report.getFileName();
    }

    public static void exportReportsIndexFile(File reportsFolder, CodeAnalysisResults analysisResults, File sokratesConfigFolder) {
        List<String[]> reportList = getReportsList(analysisResults, sokratesConfigFolder);

        File htmlExportFolder = getHtmlReportsFolder(reportsFolder);

        Metadata metadata = analysisResults.getCodeConfiguration().getMetadata();
        String title = metadata.getName();
        RichTextReport indexReport = new RichTextReport(title, "", metadata.getLogoLink());
        if (StringUtils.isNotBlank(metadata.getDescription())) {
            indexReport.addContentInDiv(metadata.getDescription(), "margin-left: 2px; margin-top: 6px; color: grey; font-size: 90%");
        }

        appendLinks(indexReport, analysisResults);

        indexReport.addContentInDiv("", "height; 10px;");

        int mainLoc = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
        int mainFilesCount = analysisResults.getMainAspectAnalysisResults().getFilesCount();
        int secondaryLoc = analysisResults.getTestAspectAnalysisResults().getLinesOfCode()
                + analysisResults.getBuildAndDeployAspectAnalysisResults().getLinesOfCode()
                + analysisResults.getGeneratedAspectAnalysisResults().getLinesOfCode()
                + analysisResults.getOtherAspectAnalysisResults().getLinesOfCode();
        int secondaryFilesCount = analysisResults.getTestAspectAnalysisResults().getFilesCount()
                + analysisResults.getBuildAndDeployAspectAnalysisResults().getFilesCount()
                + analysisResults.getGeneratedAspectAnalysisResults().getFilesCount()
                + analysisResults.getOtherAspectAnalysisResults().getFilesCount();

        addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumber(mainLoc), "lines of main code", FormattingUtils.getSmallTextForNumber(mainFilesCount) + " files", MAIN_LOC_COLOR, "main lines of code");
        addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumber(secondaryLoc), "lines of other code", FormattingUtils.getSmallTextForNumber(secondaryFilesCount) + " files", TEST_LOC_COLOR, "test, build & deployment, generated, all other code in scope");
        ContributorsAnalysisResults contributorsAnalysisResults = analysisResults.getContributorsAnalysisResults();
        if (contributorsAnalysisResults.getCommitsCount() > 0) {
            int notChanged = (int) analysisResults.getFilesHistoryAnalysisResults().getOverallFileChangeDistribution().getVeryHighRiskValue();
            int notChangedPerc = (int) analysisResults.getFilesHistoryAnalysisResults().getOverallFileChangeDistribution().getVeryHighRiskPercentage();
            int old = (int) analysisResults.getFilesHistoryAnalysisResults().getOverallFileFirstModifiedDistribution().getVeryHighRiskValue();
            int oldPerc = (int) analysisResults.getFilesHistoryAnalysisResults().getOverallFileFirstModifiedDistribution().getVeryHighRiskPercentage();
            addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumber(mainLoc - notChanged), "main code touched", "1 year (" + FormattingUtils.getFormattedPercentage(100 - notChangedPerc) + "%)", MAIN_LOC_FRESH_COLOR, "");
            addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumber(mainLoc - old), "new main code", "1 year (" + FormattingUtils.getFormattedPercentage(100 - oldPerc) + "%)", MAIN_LOC_FRESH_COLOR, "");
            int recentContributors = (int) analysisResults.getContributorsAnalysisResults().getContributors().stream().filter(c -> c.getCommitsCount30Days() > 0).count();
            addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumber(recentContributors), "recent contributors", "past 30 days", PEOPLE_COLOR, "");
        }
        StringBuilder icons = new StringBuilder("");
        addIconsMainCode(analysisResults, icons);
        indexReport.startSubSection("File Extensions in Main Code", "");
        indexReport.addHtmlContent(icons.toString());
        indexReport.endSection();

        indexReport.startSubSection("Analysis Summary", "");
        indexReport.startDiv("margin-top: -10px");
        summarize(indexReport, analysisResults);
        indexReport.endDiv();
        indexReport.endSection();

        if (contributorsAnalysisResults.getCommitsCount() > 0) {
            List<ContributionTimeSlot> contributorsPerYear = contributorsAnalysisResults.getContributorsPerYear();
            indexReport.startSubSection("Commits Trend " + "<a href='Commits.html'  title='metrics &amp; goals details' style='margin-left: 8px; vertical-align: top'>" + getDetailsIcon() + "</a>", "");

            indexReport.addParagraph("Latest commit date: " + contributorsAnalysisResults.getLatestCommitDate() + "",
                    "color: grey; font-size: 80%; margin-bottom: 2px;");
            indexReport.addParagraph("Reference analysis date: " + DateUtils.getAnalysisDate() + "",
                    "color: grey; font-size: 80%;");

            indexReport.startTable();
            indexReport.startTableRow();

            indexReport.startTableCell("border: none; vertical-align: top;");

            long contributorsCount = contributorsAnalysisResults.getContributors().stream().filter(c -> c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count();
            indexReport.startDiv("margin-top: 8px; width: 70px; height: 81px; background-color: darkgrey; border-radius: 5px; vertical-align: middle; text-align: center");
            indexReport.addContentInDiv(FormattingUtils.getSmallTextForNumber(contributorsAnalysisResults.getCommitsCount30Days()),
                    "padding-top: 12px; font-size: 26px;");
            indexReport.addContentInDiv("commits<br>(30 days)", "color: black; font-size: 60%");


            indexReport.startDiv("margin-top: 32px; width: 70px; height: 81px; background-color: skyblue; border-radius: 5px; vertical-align: middle; text-align: center");
            indexReport.addContentInDiv(FormattingUtils.getSmallTextForNumber((int) contributorsCount),
                    "padding-top: 12px; font-size: 26px;");
            indexReport.addContentInDiv("contributors<br>(30 days)", "color: black; font-size: 60%");

            indexReport.endDiv();

            indexReport.endTableCell();
            indexReport.startTableCell("border: none");
            ContributorsReportUtils.addContributorsPerTimeSlot(indexReport, contributorsPerYear, 20, true, 8);
            indexReport.endTableCell();
            indexReport.endTableRow();
            indexReport.endTable();

            indexReport.startDiv("font-size: 80%");
            indexReport.startShowMoreBlock("show commits trend per language");
            indexReport.startTable();
            indexReport.startTableRow();
            indexReport.addTableCell("Commits", "border: none");
            indexReport.startTableCell("border: none");
            List<HistoryPerExtension> historyPerExtensionPerYear = analysisResults.getFilesHistoryAnalysisResults().getHistoryPerExtensionPerYear();
            List<String> extensions = analysisResults.getMainAspectAnalysisResults().getExtensions();
            HistoryPerLanguageGenerator.getInstanceCommits(historyPerExtensionPerYear, extensions).addHistoryPerLanguage(indexReport);
            indexReport.endTableCell();
            indexReport.endTableRow();
            indexReport.startTableRow();
            indexReport.addTableCell("&nbsp;", "border: none");
            indexReport.addTableCell("&nbsp;", "border: none");
            indexReport.endTableRow();
            indexReport.startTableRow();
            indexReport.addTableCell("Contributors", "border: none");
            indexReport.startTableCell("border: none");
            HistoryPerLanguageGenerator.getInstanceContributors(historyPerExtensionPerYear, extensions).addHistoryPerLanguage(indexReport);
            indexReport.endTableCell();
            indexReport.endTableRow();
            indexReport.endTable();
            indexReport.endTabContentSection();
            indexReport.endShowMoreBlock();
            indexReport.endDiv();
            indexReport.endSection();
        }

        indexReport.startSubSection("Reports", "");
        for (String[] report : reportList) {
            addReportFragment(htmlExportFolder, indexReport, report);
        }
        indexReport.endSection();
        String dateOfUpdate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String referenceDate = new SimpleDateFormat("yyyy-MM-dd").format(DateUtils.getCalendar().getTime());
        indexReport.addParagraph("generated by <a target='_blank' href='https://sokrates.dev/'>sokrates.dev</a> " +
                        " (<a href='../data/config.json' target='_blank'>configuration</a>)" +
                        " on " + dateOfUpdate + (!referenceDate.equals(dateOfUpdate) ? "; reference date: " + referenceDate : ""),
                "color: grey; font-size: 80%; margin-left: 10px; margin-bottom: 30px");
        export(htmlExportFolder, indexReport, "index.html", analysisResults.getCodeConfiguration().getAnalysis().getCustomHtmlReportHeaderFragment());
    }

    private static void addInfoBlockWithColor(RichTextReport report, String mainValue, String subtitle, String extra, String color, String tooltip) {
        String style = "border-radius: 12px;";

        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 130px; height: 93px;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 36px;";

        report.startDiv(style, tooltip);
        String specialColor = mainValue.equals("<b>0</b>") ? " color: grey;" : "";
        report.addHtmlContent("<div style='font-size: 40px; margin-top: 10px;" + specialColor + "'>" + mainValue + "</div>");
        report.addHtmlContent("<div style='color: #434343; font-size: 12px;" + specialColor + "'>" + subtitle + "</div>");
        report.addHtmlContent("<div style='color: #434343; font-size: 11px;color: grey'>" + extra + "</div>");
        report.endDiv();
    }

    private static void addIconsMainCode(CodeAnalysisResults analysisResults, StringBuilder summary) {
        List<NumericMetric> extensions = analysisResults.getMainAspectAnalysisResults().getLinesOfCodePerExtension();
        summary.append("<div style='margin-bottom: 20px;'>");
        Set<String> alreadyAddedImage = new HashSet<>();
        extensions.forEach(ext -> {
            String lang = ext.getName().toUpperCase().replace("*.", "").trim();
            String image = DataImageUtils.getLangDataImage(lang);
            if (image == null || !alreadyAddedImage.contains(image)) {
                int loc = ext.getValue().intValue();
                int fontSize = loc >= 1000 ? 20 : 13;
                int width = loc >= 1000 ? 42 : 30;
                summary.append("<div style='width: " + width + "px; text-align: center; display: inline-block; border-radius: 5px; background-color: #f0f0f0; padding: 8px; margin-right: 4px;'>"
                        + DataImageUtils.getLangDataImageDiv30(lang)
                        + "<div style='margin-top: 3px; font-size: " + fontSize + "px'>" + FormattingUtils.getSmallTextForNumber(loc) + "</div>"
                        + "<div style='font-size: 10px; white-space: no-wrap; overflow: hidden; color: grey;'>" + lang.toLowerCase() + "</div>"
                        + "</div>");
                if (image != null) {
                    alreadyAddedImage.add(image);
                }
            }
        });
        summary.append("</div>");
    }

    private static String getDetailsIcon() {
        return getIconSvg("details", 22);
    }

    private static void addExplorersSection(RichTextReport indexReport) {
        indexReport.startSection("Explorers", "");
        for (String[] explorer : getExplorersList()) {
            addExplorerFragment(indexReport, explorer);
        }
        indexReport.endSection();
    }


    private static void summarize(RichTextReport indexReport, CodeAnalysisResults analysisResults) {
        new SummaryUtils().summarize(analysisResults, indexReport);
    }

    private static void appendLinks(RichTextReport report, CodeAnalysisResults analysisResults) {
        List<Link> links = analysisResults.getCodeConfiguration().getMetadata().getLinks();
        if (links.size() > 0) {
            report.startDiv("font-size: 80%; margin-top: 8px; margin-left: 2px;");
            links.forEach(link -> {
                if (links.indexOf(link) > 0) {
                    report.addHtmlContent(" | ");
                }
                report.addNewTabLink(link.getLabel(), link.getHref());
            });
            report.endDiv();
        }
    }


    private static File getHtmlReportsFolder(File reportsFolder) {
        File htmlExportFolder = new File(reportsFolder, htmlReportsSubFolder);
        htmlExportFolder.mkdirs();
        return htmlExportFolder;
    }

    public static String getIconSvg(String icon) {
        return getIconSvg(icon, 80);
    }

    public static String getIconSvg(String icon, int size) {
        String svg = HtmlTemplateUtils.getResource("/icons/" + icon + ".svg");
        svg = svg.replaceAll("height='.*?'", "height='" + size + "px'");
        svg = svg.replaceAll("width='.*?'", "width='" + size + "px'");
        return svg;
    }

    private static void addReportFragment(File reportsFolder, RichTextReport indexReport, String[] report) {
        String reportFileName = report[0];
        String reportTitle = report[1];
        File reportFile = new File(reportsFolder, reportFileName);
        boolean showReport = reportFile.exists() && StringUtils.isNotBlank(reportFileName);

        if (showReport) {
            indexReport.addHtmlContent("<a style='text-decoration: none' href=\"" + reportFileName + "\">");
            indexReport.addHtmlContent("<div class='group' style='padding: 10px; margin: 10px; width: 180px; height: 200px; text-align: center; display: inline-block; vertical-align: top'>");
            indexReport.startDiv("font-size:90%; color:deepskyblue");
            indexReport.addHtmlContent("Analysis Report");
        } else {
            indexReport.addHtmlContent("<div class='group' style='padding: 10px; margin: 10px; width: 180px; height: 200px; text-align: center; display: inline-block; vertical-align: top; opacity: 0.4'>");
            indexReport.startDiv("font-size:90%;");
            indexReport.addHtmlContent("Analysis Report");
        }

        indexReport.endDiv();
        indexReport.startDiv("padding: 20px;");
        if (StringUtils.isNotBlank(report[2])) {
            indexReport.addHtmlContent(getIconSvg(report[2]));
        } else {
            indexReport.addHtmlContent(ReportConstants.REPORT_SVG_ICON);
        }
        indexReport.endDiv();
        if (showReport) {
            indexReport.startDiv("color:blue; ");
            indexReport.addHtmlContent("<b>" + reportTitle + "</b>");
            indexReport.endDiv();
            indexReport.addHtmlContent("</a>");
        } else {
            indexReport.startDiv("");
            indexReport.addHtmlContent("<b>" + reportTitle + "</b>");
            indexReport.endDiv();
        }
        indexReport.endDiv();
    }

    private static void addExplorerFragment(RichTextReport indexReport, String explorer[]) {
        indexReport.addHtmlContent("<div class='group' style='padding: 10px; margin: 10px; width: 180px; height: 200px; text-align: center; display: inline-block'>");

        indexReport.startDiv("font-size:90%; color:deepskyblue");
        indexReport.addHtmlContent("Interactive Explorer");
        indexReport.endDiv();

        indexReport.startDiv("padding: 20px;");
        indexReport.addHtmlContent(ReportConstants.REPORT_SVG_ICON);
        indexReport.endDiv();

        indexReport.startDiv("font-size:100%; color:blue; ");
        indexReport.addHtmlContent("<b><a style='text-decoration: none' href=\"../explorers/" + explorer[0] + "\">" + explorer[1] + "</a></b>");
        indexReport.endDiv();

        indexReport.startDiv("margin-top: 10px; font-size: 90%; color: lightgrey");
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        indexReport.addHtmlContent(format.format(new Date()));
        indexReport.endDiv();

        indexReport.addHtmlContent("</div>");
    }

    private static List<String[]> getReportsList(CodeAnalysisResults analysisResults, File sokratesConfigFolder) {
        List<String[]> list = new ArrayList<>();

        CodeConfiguration config = analysisResults.getCodeConfiguration();
        boolean mainExists = analysisResults.getMainAspectAnalysisResults().getFilesCount() > 0;
        boolean showHistoryReport = mainExists && config.getFileHistoryAnalysis().filesHistoryImportPathExists(sokratesConfigFolder);
        boolean showDuplication = mainExists && !config.getAnalysis().isSkipDuplication();
        boolean showDependencies = mainExists && !config.getAnalysis().isSkipDependencies();
        boolean showTrends = mainExists && config.getTrendAnalysis().getReferenceAnalyses(sokratesConfigFolder).size() > 0;
        boolean showVulnerabilities = mainExists && !config.getVulnerability().isSkipVulnerabilities();
        boolean showConcerns = mainExists && config.countAllConcernsDefinitions() > 1;
        boolean showControls = mainExists && config.getGoalsAndControls().size() > 0;
        boolean showUnits = mainExists && analysisResults.getUnitsAnalysisResults().getTotalNumberOfUnits() > 0;

        File findingsFile = CodeConfigurationUtils.getDefaultSokratesFindingsFile(sokratesConfigFolder);

        boolean showFindings = false;

        if (findingsFile.exists()) {
            showFindings = FileUtils.sizeOf(findingsFile) > 10;
        }

        list.add(new String[]{"SourceCodeOverview.html", "Source Code Overview", "codebase"});
        if (mainExists) {
            list.add(new String[]{"Components.html", showDependencies ? "Components and Dependencies" : "Components", "dependencies"});
        }

        if (showDuplication) {
            list.add(new String[]{"Duplication.html", "Duplication", "duplication"});
        }

        if (mainExists) {
            list.add(new String[]{"FileSize.html", "File Size", "file_size"});
        }
        if (showHistoryReport) {
            list.add(new String[]{"FileAge.html", "File Age & Freshness", "file_history"});
            list.add(new String[]{"FileChangeFrequency.html", "File Change Frequency", "change"});
            list.add(new String[]{"FileTemporalDependencies.html", "Temporal Dependencies", "temporal_dependency"});
            list.add(new String[]{"Commits.html", "Commits", "commits"});
            list.add(new String[]{"Contributors.html", "Contributors", "contributors"});
        }
        if (showUnits) {
            list.add(new String[]{"UnitSize.html", "Unit Size", "unit_size"});
            list.add(new String[]{"ConditionalComplexity.html", "Conditional Complexity", "conditional"});
        }
        if (showConcerns) {
            list.add(new String[]{"FeaturesOfInterest.html", "Features of Interest", "cross_cutting_concerns"});
        }
        list.add(new String[]{"Metrics.html", "All Metrics", "metrics"});
        if (showTrends) {
            list.add(new String[]{"Trend.html", "Trend", "trend"});
        }
        if (showControls) {
            list.add(new String[]{"Controls.html", "Goals & Controls", "goal"});
        }

        if (showFindings) {
            list.add(new String[]{"Notes.html", "Notes & Findings", "notes"});
        }
        if (showVulnerabilities) {
            list.add(new String[]{"Vulnerabilities.html", "Vulnerabilities", "vulnerabilities"});
        }

        if (!mainExists) {
            list.add(new String[]{"", showDependencies ? "Components and Dependencies" : "Components", "dependencies"});
        }
        if (!showDuplication) {
            list.add(new String[]{"", "Duplication", "duplication"});
        }
        if (!mainExists) {
            list.add(new String[]{"", "File Size", "file_size"});
        }
        if (!showHistoryReport) {
            list.add(new String[]{"", "File Age & Freshness", "file_history"});
            list.add(new String[]{"", "File Change Frequency", "change"});
            list.add(new String[]{"", "Temporal Dependencies", "temporal_dependency"});
            list.add(new String[]{"", "Contributors", "contributors"});
        }
        if (!showUnits) {
            list.add(new String[]{"", "Unit Size", "unit_size"});
            list.add(new String[]{"", "Conditional Complexity", "conditional"});
        }
        if (!showConcerns) {
            list.add(new String[]{"", "Features of Interest", "cross_cutting_concerns"});
        }
        if (!showTrends) {
            list.add(new String[]{"", "Trend", "trend"});
        }
        if (!showControls) {
            list.add(new String[]{"", "Goals & Controls", "goal"});
        }

        if (!showFindings) {
            list.add(new String[]{"", "Notes & Findings", "notes"});
        }
        if (!showVulnerabilities) {
            list.add(new String[]{"", "Vulnerabilities", "vulnerabilities"});
        }


        return list;
    }

    private static String[][] getExplorersList() {
        return new String[][]{
                {"MainFiles.html", "Files"},
                {"Units.html", "Units"},
                {"Duplicates.html", "Duplicates"},
                {"Dependencies.html", "Dependencies"}
        };
    }
}
