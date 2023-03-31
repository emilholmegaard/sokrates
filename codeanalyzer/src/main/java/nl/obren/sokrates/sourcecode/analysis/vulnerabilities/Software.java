package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class Software{
    public String id;
    public String versionStartIncluding;
    public String versionEndExcluding;
    public String versionEndIncluding;
}