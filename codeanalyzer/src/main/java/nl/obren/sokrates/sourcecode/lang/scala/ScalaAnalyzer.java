/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.scala;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;

public class ScalaAnalyzer extends LanguageAnalyzer {
    public ScalaAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        CommentsAndEmptyLinesCleaner cleaner = getCommentsAndEmptyLinesCleaner();

        return cleaner.clean(sourceFile.getContent());
    }

    private CommentsAndEmptyLinesCleaner getCommentsAndEmptyLinesCleaner() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();

        cleaner.addCommentBlockHelper("/*", "*/");
        cleaner.addCommentBlockHelper("//", "\n");
        cleaner.addStringBlockHelper("\"", "\\");
        cleaner.addStringBlockHelper("'", "\\");
        return cleaner;
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        CommentsAndEmptyLinesCleaner cleaner = getCommentsAndEmptyLinesCleaner();

        String content = cleaner.cleanKeepEmptyLines(sourceFile.getContent());
        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("package .*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("import .*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[{]", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[}]", content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        CStyleHeuristicUnitsExtractor cStyleHeuristicUnitsExtractor = new ScalaHeuristicUnitsExtractor();
        return cStyleHeuristicUnitsExtractor.extractUnits(sourceFile);
    }


    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new ScalaHeuristicDependenciesExtractor().extractDependencies(sourceFiles, progressFeedback);
    }

    @Override
    public List<String> getFeaturesDescription() {
        List<String> features = new ArrayList<>();

        features.add(FEATURE_ALL_STANDARD_ANALYSES);
        features.add(FEATURE_ADVANCED_CODE_CLEANING);
        features.add(FEATURE_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_CONDITIONAL_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_BASIC_DEPENDENCIES_ANALYSIS);

        return features;
    }
}
