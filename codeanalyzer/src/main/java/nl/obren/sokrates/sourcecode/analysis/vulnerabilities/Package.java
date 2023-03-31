package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class Package{
    public String id;
    public String confidence;
    public String url;
}

