package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class EvidenceCollected{
    public ArrayList<VendorEvidence> vendorEvidence;
    public ArrayList<ProductEvidence> productEvidence;
    public ArrayList<VersionEvidence> versionEvidence;
}
