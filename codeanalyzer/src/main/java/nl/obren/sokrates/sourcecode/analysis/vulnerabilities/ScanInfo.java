package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class ScanInfo{
    public String engineVersion;
    public ArrayList<DataSource> dataSource;
    public ArrayList<AnalysisException> analysisExceptions;
}