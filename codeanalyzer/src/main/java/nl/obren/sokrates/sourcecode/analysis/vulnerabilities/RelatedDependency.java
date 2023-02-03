package nl.obren.sokrates.sourcecode.analysis.vulnerabilities;

import java.util.ArrayList;

public class RelatedDependency{
    public boolean isVirtual;
    public String fileName;
    public String filePath;
    public String sha256;
    public String sha1;
    public String md5;
    public ArrayList<PackageId> packageIds;
}
