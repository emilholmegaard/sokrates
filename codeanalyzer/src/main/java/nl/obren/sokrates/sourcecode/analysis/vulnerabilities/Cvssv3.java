package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class Cvssv3{
    public double baseScore;
    public String attackVector;
    public String attackComplexity;
    public String privilegesRequired;
    public String userInteraction;
    public String scope;
    public String confidentialityImpact;
    public String integrityImpact;
    public String availabilityImpact;
    public String baseSeverity;
    public String exploitabilityScore;
    public String impactScore;
    public String version;
}
