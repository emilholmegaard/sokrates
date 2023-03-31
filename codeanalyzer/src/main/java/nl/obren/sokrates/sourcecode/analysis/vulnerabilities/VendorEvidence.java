package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class VendorEvidence{
    public String type;
    public String confidence;
    public String source;
    public String name;
    public String value;
}