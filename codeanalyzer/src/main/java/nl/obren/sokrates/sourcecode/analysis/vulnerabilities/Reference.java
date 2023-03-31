package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class Reference{
    public String source;
    public String url;
    public String name;
}
