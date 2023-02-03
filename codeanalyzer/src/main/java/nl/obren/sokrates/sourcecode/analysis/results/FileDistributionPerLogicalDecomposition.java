/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.ArrayList;
import java.util.List;

public class FileDistributionPerLogicalDecomposition {
    private String name = "";
    private List<RiskDistributionStats> fileSizeDistributionPerComponent = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RiskDistributionStats> getFileSizeDistributionPerComponent() {
        return fileSizeDistributionPerComponent;
    }

    public void setFileSizeDistributionPerComponent(List<RiskDistributionStats> fileSizeDistributionPerComponent) {
        this.fileSizeDistributionPerComponent = fileSizeDistributionPerComponent;
    }
}
