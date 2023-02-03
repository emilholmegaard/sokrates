package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Credits{
    @JsonProperty("NVD") 
    public String nVD;
    @JsonProperty("NPM") 
    public String nPM;
    @JsonProperty("RETIREJS") 
    public String rETIREJS;
    @JsonProperty("OSSINDEX") 
    public String oSSINDEX;
}



























