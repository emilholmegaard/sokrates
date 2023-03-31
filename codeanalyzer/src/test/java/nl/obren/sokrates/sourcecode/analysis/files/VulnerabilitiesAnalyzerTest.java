package nl.obren.sokrates.sourcecode.analysis.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.stream;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.glassfish.jersey.internal.guava.Lists;
import org.junit.jupiter.api.Test;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.VulnerabilitiesAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.vulnerabilities.Dependency;
import nl.obren.sokrates.sourcecode.analysis.vulnerabilities.Package;
import nl.obren.sokrates.sourcecode.analysis.vulnerabilities.Vulnerability;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.VulnerabilityConfig;

import static java.nio.file.FileSystems.getDefault;

public class VulnerabilitiesAnalyzerTest {

    @Test
    void SkipVulnerabilityAnalysis() {
        CodeAnalysisResults analysisResults = new CodeAnalysisResults();
        VulnerabilityConfig vulnerabilityConfig = new VulnerabilityConfig();
        String file = "non_existing_path.json";
        vulnerabilityConfig.setPathToJsonFile(file);
        CodeConfiguration codeConfig = new CodeConfiguration();
        codeConfig.setVulnerability(vulnerabilityConfig);
        analysisResults.setCodeConfiguration(codeConfig);
        File srcRoot = new File("");
        VulnerabilityAnalyzer analyzer = new VulnerabilityAnalyzer(analysisResults, srcRoot);
        ProgressFeedback progressFeedback = new ProgressFeedback();
        analyzer.analyze(progressFeedback);
        Path jsonPath = srcRoot.toPath().resolve(file);
        assertEquals("Dependency file not found: .\\" + jsonPath + "\n", analysisResults.getTextSummary().toString());
    }

    @Test
    void GetNumberOfCVEs() {
        CodeAnalysisResults analysisResults = new CodeAnalysisResults();
        VulnerabilityConfig vulnerabilityConfig = new VulnerabilityConfig();

        String seperator = getDefault().getSeparator();
        // Test file generated January 2023 for zookeeper by OWASP Dependency Check
        String jsonFile = "src" + seperator + "test" + seperator + "java" + seperator + "nl" + seperator + "obren"
                + seperator + "sokrates" + seperator + "sourcecode" + seperator + "analysis" + seperator + "files"
                + seperator + "vulnerability_test_file.json";

        vulnerabilityConfig.setPathToJsonFile(jsonFile);
        CodeConfiguration codeConfig = new CodeConfiguration();
        codeConfig.setVulnerability(vulnerabilityConfig);
        analysisResults.setCodeConfiguration(codeConfig);
        File root = new File("");
        VulnerabilityAnalyzer analyzer = new VulnerabilityAnalyzer(analysisResults, root);
        ProgressFeedback progressFeedback = new ProgressFeedback();
        analyzer.analyze(progressFeedback);

        assertTrue((analysisResults.getTextSummary().indexOf("Analysing / updating vulnerabilities...") != -1));
        assertEquals(0,analysisResults.getVulnerabilitiesAnalysisResults().getTotalCriticalVulnerabilities());
        assertEquals(0,analysisResults.getVulnerabilitiesAnalysisResults().getTotalHighVulnerabilities());
        assertEquals( 1,analysisResults.getVulnerabilitiesAnalysisResults().getTotalMediumVulnerabilities());
        assertEquals(0,analysisResults.getVulnerabilitiesAnalysisResults().getTotalLowVulnerabilities());
        assertEquals(23,analysisResults.getVulnerabilitiesAnalysisResults().getTotalNumberOfThirdPartyDependencies() );

        assertFalse(
                (analysisResults.getTextSummary().indexOf("Error reading file / updating vulnerabilities...") != -1));

        assertTrue((analysisResults.getTextSummary().indexOf("OWASP Dependency file has") != -1));
    }

    @Test
    void GetNumberOfCVEZookeeper() {
        String file = "dependency-check-report-zookeeper.json";
        VulnerabilitiesAnalysisResults results = GetResults(file);

        assertEquals(33, results.getTotalNumberOfThirdPartyDependencies());
        assertEquals(2, results.getTotalNumberOfCves());
        assertEquals(6, results.getTotalVulnerableDependencies());
    }




    @Test
    void GetPackagesZookeeper() {
        String package1 = "pkg:javascript/YUI@3.1.0";
        String package2 = "pkg:javascript/jquery-ui@1.12.1";

        String file = "dependency-check-report-zookeeper.json";
        VulnerabilitiesAnalysisResults results = GetResults(file);

        assertFalse(results.getVulnerableDependenciesToPackage().values().isEmpty());
    assertTrue(results.getVulnerableDependenciesToPackage().values().stream().anyMatch(strArray ->  Arrays.asList(strArray).contains(package1)));
        assertTrue(results.getVulnerableDependenciesToPackage().values().stream().anyMatch(strArray ->  Arrays.asList(strArray).contains(package2)));
      

    }

    @Test
    void GetHighestServerityZookeeper() {
        String file = "dependency-check-report-zookeeper.json";
        VulnerabilitiesAnalysisResults results = GetResults(file);
        String[] severity = results.getVulnerableDependenciesToHighestSeverity().values().toArray(new String[0]);


        assertFalse(Arrays.asList(severity).contains("HIGH"));
        assertTrue(Arrays.asList(severity).contains("MEDIUM"));
        assertFalse(Arrays.asList(severity).contains("LOW"));
        
    }

    @Test
    void GetNumberOfCVEAirflow() {
        String file = "dependency-check-report-airflow.json";
        VulnerabilitiesAnalysisResults results = GetResults(file);

        assertEquals(28, results.getTotalNumberOfThirdPartyDependencies());
        assertEquals(0, results.getTotalNumberOfCves());
        assertEquals(0, results.getTotalVulnerableDependencies());

    }

    @Test
    void GetPackagesAirflow() {
        String package1 = "pkg:javascript/YUI@3.1.0";
        String package2 = "pkg:javascript/jquery-ui@1.12.1";

       String file = "dependency-check-report-airflow.json";
        VulnerabilitiesAnalysisResults results = GetResults(file);
        
        assertTrue(results.getVulnerableDependenciesToPackage().values().isEmpty());
        assertFalse(results.getVulnerableDependenciesToPackage().values().stream().anyMatch(strArray ->  Arrays.asList(strArray).contains(package1)));
        assertFalse(results.getVulnerableDependenciesToPackage().values().stream().anyMatch(strArray ->  Arrays.asList(strArray).contains(package2)));
    }
 
    @Test
    void GetHighestSeverityAirflow() {
        String file = "dependency-check-report-airflow.json";
        VulnerabilitiesAnalysisResults results = GetResults(file);
        String[] severity = results.getVulnerableDependenciesToHighestSeverity().values().toArray(new String[0]);

        assertFalse(Arrays.asList(severity).contains("HIGHEST"));
        assertFalse(Arrays.asList(severity).contains("MEDIUM"));
        assertFalse(Arrays.asList(severity).contains("LOW"));
        
    }

    @Test
    void GetNumberOfCVEAnyproxy() {
        String file = "dependency-check-report-anyproxy.json";
        VulnerabilitiesAnalysisResults results = GetResults(file);

        assertEquals(20492, results.getTotalNumberOfThirdPartyDependencies());
        assertEquals(63, results.getTotalNumberOfCves());
        assertEquals(189, results.getTotalVulnerableDependencies());
    }

    @Test
    void GetPackagesAnyproxy() {
        String package1 = "pkg:npm/%40ampproject%2Fremapping@2.2.0";
        String package2 = "pkg:npm/%40babel%2Fcompat-data@7.20.10";
        String file = "dependency-check-report-anyproxy.json";
        VulnerabilitiesAnalysisResults results = GetResults(file);
        
        assertFalse(results.getVulnerableDependenciesToPackage().values().isEmpty());
        assertTrue(results.getVulnerableDependenciesToPackage().values().stream().anyMatch(strArray ->  Arrays.asList(strArray).contains(package1)));
        assertTrue(results.getVulnerableDependenciesToPackage().values().stream().anyMatch(strArray ->  Arrays.asList(strArray).contains(package2)));

        String vulnerabilityID1 = "cpe:2.3:a:ajv.js:ajv:5.5.2:*:*:*:*:*:*:*";
        String vulnerabilityID2 = "cpe:2.3:a:ajv.js:ajv:6.12.6:*:*:*:*:*:*:*";


        assertFalse(results.getVulnerableDependenciesToVulnerabilityId().values().isEmpty());
        assertTrue(results.getVulnerableDependenciesToVulnerabilityId().values().stream().anyMatch(strArray ->  Arrays.asList(strArray).contains(vulnerabilityID1)));
        assertTrue(results.getVulnerableDependenciesToVulnerabilityId().values().stream().anyMatch(strArray ->  Arrays.asList(strArray).contains(vulnerabilityID2)));


    }

    @Test
    void GetHighestServerityAnyproxy() {
        String file = "dependency-check-report-anyproxy.json";
        VulnerabilitiesAnalysisResults results = GetResults(file);
        String[] severity = results.getVulnerableDependenciesToHighestSeverity().values().toArray(new String[0]);

        assertTrue(Arrays.asList(severity).contains("CRITICAL"));
        assertTrue(Arrays.asList(severity).contains("HIGH"));
        assertTrue(Arrays.asList(severity).contains("MEDIUM"));
        assertTrue(Arrays.asList(severity).contains("MODERATE"));
        
    }

    @Test
    void GetEvidenceCountAnyproxy() {
        String file = "dependency-check-report-anyproxy.json";
        String dependency1 = "cryptiles:2.0.5";
        String dependency2 = "handlebars:^4.0.3";
        VulnerabilitiesAnalysisResults results = GetResults(file);

        assertEquals(20492, results.getTotalNumberOfThirdPartyDependencies());
        assertEquals(63, results.getTotalNumberOfCves());
        assertEquals(189, results.getTotalVulnerableDependencies());


        assertFalse(results.getVulnerableDependenciesToPackage().values().isEmpty());
        assertTrue(results.getVulnerableDependenciesToPackage().containsKey(dependency1));
        assertTrue(results.getVulnerableDependenciesToPackage().containsKey(dependency2));

        assertEquals(5,results.getVulnerableDependenciesToEvidenceCount().get(dependency1));
        assertEquals(12,results.getVulnerableDependenciesToEvidenceCount().get(dependency2));

        assertEquals(2,results.getVulnerableDependenciesToCVECount().get(dependency1));
        assertEquals(14,results.getVulnerableDependenciesToCVECount().get(dependency2));

    }

    private VulnerabilitiesAnalysisResults GetResults(String fileName) {

        CodeAnalysisResults analysisResults = new CodeAnalysisResults();
        VulnerabilityConfig vulnerabilityConfig = new VulnerabilityConfig();

        String seperator = getDefault().getSeparator();
        // Test file generated January 2023 for zookeeper by OWASP Dependency Check
        String jsonFile = "src" + seperator + "test" + seperator + "java" + seperator + "nl" + seperator + "obren"
                + seperator + "sokrates" + seperator + "sourcecode" + seperator + "analysis" + seperator + "files"
                + seperator + fileName;

        vulnerabilityConfig.setPathToJsonFile(jsonFile);
        CodeConfiguration codeConfig = new CodeConfiguration();
        codeConfig.setVulnerability(vulnerabilityConfig);
        analysisResults.setCodeConfiguration(codeConfig);
        File root = new File("");
        VulnerabilityAnalyzer analyzer = new VulnerabilityAnalyzer(analysisResults, root);
        ProgressFeedback progressFeedback = new ProgressFeedback();
        analyzer.analyze(progressFeedback);

        return analysisResults.getVulnerabilitiesAnalysisResults();
    }

    

    

}
