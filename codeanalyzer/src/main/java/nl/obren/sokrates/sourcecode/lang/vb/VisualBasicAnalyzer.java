/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.vb;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VisualBasicAnalyzer extends LanguageAnalyzer {
    public VisualBasicAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        CommentsAndEmptyLinesCleaner cleaner = getCommentsAndEmptyLinesCleaner();

        return cleaner.clean(sourceFile.getContent());
    }

    protected CommentsAndEmptyLinesCleaner getCommentsAndEmptyLinesCleaner() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();

        cleaner.addCommentBlockHelper("\n REM ", "\n");
        cleaner.addCommentBlockHelper("'", "\n");
        cleaner.addStringBlockHelper("\"", "\"");

        return cleaner;
    }

    protected CommentsAndEmptyLinesCleaner getCommentsAndEmptyLinesCleanerExtraString() {
        CommentsAndEmptyLinesCleaner cleaner = getCommentsAndEmptyLinesCleaner();
        cleaner.getCodeBlockParsers().forEach(codeBlockParser -> codeBlockParser.setRemoveWhenCleaning(true));
        return cleaner;
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        CommentsAndEmptyLinesCleaner cleaner = getCommentsAndEmptyLinesCleaner();

        String content = cleaner.cleanKeepEmptyLines(sourceFile.getContent());
        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[ ]*End [A-Z][a-z]+[ ]*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[ ]*Imports .*", content);

        return SourceCodeCleanerUtils.cleanSingleLineCommentsAndEmptyLines(content, Arrays.asList("'", "REM "));
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        return new VisualBasicHeuristicUnitsExtractor(this).extractUnits(sourceFile);
    }

    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new DependenciesAnalysis();
    }

    @Override
    public List<String> getFeaturesDescription() {
        List<String> features = new ArrayList<>();

        features.add(FEATURE_ALL_STANDARD_ANALYSES);
        features.add(FEATURE_ADVANCED_CODE_CLEANING);
        features.add(FEATURE_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_CONDITIONAL_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_NO_DEPENDENCIES_ANALYSIS);

        return features;
    }
}
