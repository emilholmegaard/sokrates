/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.SourceCodeFiles;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.ConcernsGroup;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.core.AnalysisConfig;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.core.TagRule;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomExtensionConventions;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomScopingConventions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ScopeCreator {
    private static final Log LOG = LogFactory.getLog(ScopeCreator.class);

    private File srcRoot;
    private File confFile;
    private CustomScopingConventions customScopingConventions;

    public ScopeCreator(File srcRoot, File confFile, CustomScopingConventions customScopingConventions) {
        this.srcRoot = srcRoot;
        this.confFile = confFile;
        this.customScopingConventions = customScopingConventions;
    }

    public void createScopeFromConventions(String name, String description, String logoLink, Link link) throws IOException {
        List<String> extensions = getExtensions();

        CodeConfiguration codeConfiguration = getCodeConfiguration(extensions);

        String analysisName = StringUtils.capitalize(srcRoot.getCanonicalFile().getName().toLowerCase());
        codeConfiguration.getMetadata().setName(analysisName);

        LOG.info("Scanning source files...");

        AnalysisConfig analysis = codeConfiguration.getAnalysis();
        SourceCodeFiles sourceCodeFiles = getSourceCodeFiles(extensions, analysis);

        if (customScopingConventions == null || !customScopingConventions.isIgnoreStandardScopingConventions()) {
            expandScopeWithConventions(codeConfiguration, sourceCodeFiles);
        }
        if (customScopingConventions != null) {
            expandScopeWithCustomConventions(codeConfiguration, sourceCodeFiles);
            if (customScopingConventions.isIgnoreStandardControls()) {
                codeConfiguration.getGoalsAndControls().clear();
            }
            codeConfiguration.getGoalsAndControls().addAll(customScopingConventions.getGoalsAndControls());
        }

        if (StringUtils.isNotBlank(name)) {
            codeConfiguration.getMetadata().setName(name);
        }
        if (StringUtils.isNotBlank(description)) {
            codeConfiguration.getMetadata().setDescription(description);
        }
        if (StringUtils.isNotBlank(logoLink)) {
            codeConfiguration.getMetadata().setLogoLink(logoLink);
        }
        if (link != null) {
            codeConfiguration.getMetadata().getLinks().add(link);
        }

        saveScope(codeConfiguration);
    }

    private void expandScopeWithCustomConventions(CodeConfiguration codeConfiguration, SourceCodeFiles sourceCodeFiles) {
        List<SourceFile> sourceFiles = sourceCodeFiles.getFilesInBroadScope();
        ConventionUtils.addConventions(customScopingConventions.getIgnoredFilesConventions(), codeConfiguration.getIgnore(), sourceFiles);
        ConventionUtils.addConventions(customScopingConventions.getTestFilesConventions(), codeConfiguration.getTest().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(customScopingConventions.getGeneratedFilesConventions(), codeConfiguration.getGenerated().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(customScopingConventions.getBuildAndDeploymentFilesConventions(), codeConfiguration.getBuildAndDeployment().getSourceFileFilters(), sourceFiles);
        ConventionUtils.addConventions(customScopingConventions.getOtherFilesConventions(), codeConfiguration.getOther().getSourceFileFilters(), sourceFiles);

        List<ConcernsGroup> concernGroups = codeConfiguration.getConcernGroups();
        if (customScopingConventions.isRemoveStandardConcerns()) {
            concernGroups.clear();
        }

        if (customScopingConventions.getConcerns().size() > 0) {
            if (concernGroups.size() == 0) {
                concernGroups.add(new ConcernsGroup("general"));
            }

            concernGroups.get(0).getConcerns().addAll(customScopingConventions.getConcerns());
        }

        codeConfiguration.getFileHistoryAnalysis().getIgnoreContributors().addAll(customScopingConventions.getIgnoreContributors());
        codeConfiguration.getFileHistoryAnalysis().getBots().addAll(customScopingConventions.getBots());

        codeConfiguration.setAnalysis(customScopingConventions.getAnalysis());
        List<TagRule> tagRules = customScopingConventions.getTagRules();
        if (tagRules.size() > 0) {
            codeConfiguration.setTagRules(tagRules);
        }
        codeConfiguration.setFileHistoryAnalysis(customScopingConventions.getFileHistoryAnalysis());
        codeConfiguration.setTrendAnalysis(customScopingConventions.getTrendAnalysis());
        codeConfiguration.getMetadata().setLogoLink(customScopingConventions.getLogoLink());

        LogicalDecomposition logicalDecomposition = codeConfiguration.getLogicalDecompositions().get(0);
        logicalDecomposition.setComponentsFolderDepth(customScopingConventions.getComponentsFolderDepth());
        logicalDecomposition.setMinComponentsCount(customScopingConventions.getMinComponentsCount());
    }

    private List<String> getExtensions() {
        ExtensionGroupExtractor extractor = new ExtensionGroupExtractor();
        extractor.extractExtensionsInfo(srcRoot);

        return getExtensions(extractor);
    }

    private List<String> getExtensions(ExtensionGroupExtractor extractor) {
        List<String> extensions = new ArrayList<>();
        extractor.getExtensionsList()
                .stream()
                .filter(e -> shouldIncludeExtension(e.getExtension()))
                .forEach(extensionGroup -> {
                    extensions.add(extensionGroup.getExtension());
                });
        return extensions;
    }

    private boolean shouldIncludeExtension(String extension) {
        if (customScopingConventions != null) {
            CustomExtensionConventions customExtensions = customScopingConventions.getExtensions();
            if (customExtensions.getOnlyInclude().size() > 0) {
                for (String onlyInclude : customExtensions.getOnlyInclude()) {
                    if (onlyInclude.equalsIgnoreCase(extension)) {
                        return true;
                    }
                }
                return false;
            }
            if (customExtensions.getAlwaysExclude().size() > 0) {
                for (String alwaysExclude : customExtensions.getAlwaysExclude()) {
                    if (alwaysExclude.equalsIgnoreCase(extension)) {
                        return false;
                    }
                }
            }
        }
        return ExtensionGroupExtractor.isKnownSourceCodeExtension(extension);
    }

    private CodeConfiguration getCodeConfiguration(List<String> extensions) {
        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();
        codeConfiguration.setExtensions(extensions);
        return codeConfiguration;
    }

    private SourceCodeFiles getSourceCodeFiles(List<String> extensions, AnalysisConfig analysisConfig) {
        SourceCodeFiles sourceCodeFiles = getSourceCodeFiles();
        sourceCodeFiles.createBroadScope(extensions, new ArrayList<>(), analysisConfig);
        return sourceCodeFiles;
    }

    private SourceCodeFiles getSourceCodeFiles() {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.load(srcRoot, new ProgressFeedback() {
        });
        return sourceCodeFiles;
    }

    private void expandScopeWithConventions(CodeConfiguration codeConfiguration, SourceCodeFiles sourceCodeFiles) {
        ScopingConventions scopingConventions = new ScopingConventions();
        scopingConventions.addConventions(codeConfiguration, sourceCodeFiles.getFilesInBroadScope());
    }

    private void saveScope(CodeConfiguration codeConfiguration) throws IOException {
        String json = new JsonGenerator().generate(codeConfiguration);
        if (confFile == null) {
            confFile = CodeConfigurationUtils.getDefaultSokratesConfigFile(srcRoot);
        }
        FileUtils.writeStringToFile(confFile, json, StandardCharsets.UTF_8);
    }

}
