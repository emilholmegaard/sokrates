package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import java.sql.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class DataSource{
    public String name;
    public Date timestamp;
}
