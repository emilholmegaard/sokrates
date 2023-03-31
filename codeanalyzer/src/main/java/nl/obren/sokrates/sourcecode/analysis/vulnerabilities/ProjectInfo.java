package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class ProjectInfo{
    public String name;
    public String reportDate;
    public Credits credits;
}
