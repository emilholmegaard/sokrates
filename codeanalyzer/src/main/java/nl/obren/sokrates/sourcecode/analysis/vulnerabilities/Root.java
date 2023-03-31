package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class Root{
    public String reportSchema;
    public ScanInfo scanInfo;
    public ProjectInfo projectInfo;
    public ArrayList<Dependency> dependencies;
}
