package nl.obren.sokrates.sourcecode.cleaners;

import nl.obren.sokrates.sourcecode.lang.cpp.CAnalyzer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CodeBlockParsingHelperTest {

    @Test
    public void getStringStartIndex() {
        CodeBlockParsingHelper stringParsing = new CodeBlockParsingHelper("'", "'", "\\", false);

        assertEquals(10, stringParsing.getStringEndIndex("Line with 'string content' ", 0));
        assertEquals(10, stringParsing.getStringEndIndex("Line with 'string content' ", 2));
        assertEquals(10, stringParsing.getStringEndIndex("Line with 'string content' ", 10));
        assertEquals(25, stringParsing.getStringEndIndex("Line with 'string content' ", 11));
    }

    @Test
    public void getStringEndIndex() {
        CodeBlockParsingHelper stringParsing = new CodeBlockParsingHelper("'", "'", "\\", false);

        assertEquals(10, stringParsing.getStringEndIndex("Line with 'string \\'content\\'' ", 0));
        assertEquals(29, stringParsing.getStringEndIndex("Line with 'string \\'content\\'' ", 11));
        assertEquals(29, stringParsing.getStringEndIndex("Line with 'string \\'content\\'' ", 12));
        assertEquals(31, stringParsing.getStringEndIndex("Line with 'string \n \\'content\\'' ", 13));

        stringParsing = new CodeBlockParsingHelper("\"\"\"", "\"\"\"", "", false);

        String content = "Line with \"\"\"string\n\"content\" \"\"\" ";

        assertEquals(10, stringParsing.getStringEndIndex(content, 0));
        assertEquals(30, stringParsing.getStringEndIndex(content, 11));
        assertEquals(30, stringParsing.getStringEndIndex(content, 12));
        assertEquals(30, stringParsing.getStringEndIndex(content, 13));

        stringParsing = new CodeBlockParsingHelper("@\"", "\"", "\"", false);

        content = "Line with @\"string\n\"\"content\"\" \" ";

        assertEquals(11, stringParsing.getStringEndIndex(content, 0));
        assertEquals(11, stringParsing.getStringEndIndex(content, 11));
        assertEquals(31, stringParsing.getStringEndIndex(content, 12));
        assertEquals(31, stringParsing.getStringEndIndex(content, 13));
    }

    @Test
    public void cleanOrSkip() {
        CodeBlockParsingHelper helperSkip = new CodeBlockParsingHelper("'", "'", "\\", false);
        CodeBlockParsingHelper helperClean = new CodeBlockParsingHelper("'", "'", "\\", true);

        String content = "Line with 'string \\'content\\'' ";

        int startIndex = helperSkip.getStringStartIndex(content, 0);

        assertEquals(content, helperSkip.cleanOrSkip(content, startIndex).getContent());
        assertEquals(30, helperSkip.cleanOrSkip(content, startIndex).getCurrentIndex());

        assertEquals("Line with  ", helperClean.cleanOrSkip(content, startIndex).getContent());
        assertEquals(10, helperClean.cleanOrSkip(content, startIndex).getCurrentIndex());

    }

    @Test
    public void cleanOrSkipMultiLine() {
        CodeBlockParsingHelper helperSkip = new CodeBlockParsingHelper("'", "'", "\\", false);
        CodeBlockParsingHelper helperClean = new CodeBlockParsingHelper("'", "'", "\\", true);

        String contentMultiLine = "Line with 'string \\'multi\nline\ncontent\\'' ";

        int startIndex = helperSkip.getStringStartIndex(contentMultiLine, 0);

        assertEquals(contentMultiLine, helperSkip.cleanOrSkip(contentMultiLine, startIndex).getContent());
        assertEquals(41, helperSkip.cleanOrSkip(contentMultiLine, startIndex).getCurrentIndex());

        assertEquals("Line with \n\n ", helperClean.cleanOrSkip(contentMultiLine, startIndex).getContent());
        assertEquals(12, helperClean.cleanOrSkip(contentMultiLine, startIndex).getCurrentIndex());

    }

    @Test
    public void cleanOrSkipOneLineComment() {
        CodeBlockParsingHelper helper = new CodeBlockParsingHelper("//", "\n", "", true);

        String content = "Line 1 // comment 1\nLine 2 // comment 2\n";

        int startIndex = helper.getStringStartIndex(content, 0);

        String result1 = "Line 1 \nLine 2 // comment 2\n";

        assertEquals(result1, helper.cleanOrSkip(content, startIndex).getContent());
        assertEquals(8, helper.cleanOrSkip(content, startIndex).getCurrentIndex());

        startIndex = helper.getStringStartIndex(result1, 0);

        assertEquals("Line 1 \nLine 2 \n", helper.cleanOrSkip(result1, startIndex).getContent());
        assertEquals(16, helper.cleanOrSkip(content, startIndex).getCurrentIndex());
    }
}
