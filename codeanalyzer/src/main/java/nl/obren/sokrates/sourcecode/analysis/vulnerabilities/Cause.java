package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class Cause{
    public String message;
    public ArrayList<String> stackTrace;
    public Cause cause;
}
