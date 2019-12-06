package nl.obren.sokrates.sourcecode.cleaners;

import nl.obren.sokrates.sourcecode.CleaningResult;

import java.util.ArrayList;
import java.util.List;

public class CommentsAndEmptyLinesCleaner {
    private List<CodeBlockParsingHelper> codeBlockParsingHelpers = new ArrayList<>();
    private String content;
    private CodeBlockParsingHelper activeHelper = null;

    private int currentIndex = 0;

    public CommentsAndEmptyLinesCleaner() {
    }

    public CommentsAndEmptyLinesCleaner(String singleLineCommentStart, String blockCommentStart, String blockCommentEnd) {
        addCommentBlockHelper(singleLineCommentStart, "\n", "");
        addCommentBlockHelper(blockCommentStart, blockCommentEnd, "");
    }

    public CommentsAndEmptyLinesCleaner(String singleLineCommentStart, String blockCommentStart, String blockCommentEnd, String stringDelimiter, String stringEscapeMarker) {
        addCommentBlockHelper(singleLineCommentStart, "\n", "");
        addCommentBlockHelper(blockCommentStart, blockCommentEnd, "");
        addStringBlockHelper(stringDelimiter, stringDelimiter, stringEscapeMarker);
    }

    private void addStringBlockHelper(String startMarker, String endMarker, String escapeMarker) {
        codeBlockParsingHelpers.add(new CodeBlockParsingHelper(startMarker, endMarker, escapeMarker, false));
    }

    private void addCommentBlockHelper(String startMarker, String endMarker, String escapeMarker) {
        codeBlockParsingHelpers.add(new CodeBlockParsingHelper(startMarker, endMarker, escapeMarker, true));
    }

    public CleanedContent clean(String originalContent) {
        this.content = SourceCodeCleanerUtils.normalizeLineEnds(originalContent);

        while (true) {
            activeHelper = null;
            final int index[] = {-1};
            this.codeBlockParsingHelpers.forEach(helper -> {
                int helperIndex = helper.getStringStartIndex(content, currentIndex);

                if (helperIndex >= 0 && (index[0] == -1 || helperIndex <= index[0])) {
                    index[0] = helperIndex;
                    activeHelper = helper;
                }
            });

            if (activeHelper != null) {
                CleaningResult cleaningResult = activeHelper.cleanOrSkip(content, index[0]);
                content = cleaningResult.getContent();
                currentIndex = cleaningResult.getCurrentIndex();
            } else {
                break;
            }
        }

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

}
