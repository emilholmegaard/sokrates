package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class Dependency{
    public boolean isVirtual;
    public String fileName;
    public String filePath;
    public String md5;
    public String sha1;
    public String sha256;
    public EvidenceCollected evidenceCollected;
    public ArrayList<RelatedDependency> relatedDependencies;
    public ArrayList<Package> packages;
    public ArrayList<Vulnerability> vulnerabilities;
    public String description;
    public ArrayList<VulnerabilityId> vulnerabilityIds;
}