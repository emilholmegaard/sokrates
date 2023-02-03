/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;
import nl.obren.sokrates.sourcecode.analysis.vulnerabilities.Dependency;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;


public class VulnerabilitiesAnalysisResults {

    private int totalNumberOfCves;
    private int totalNumberOfThridPartyDependencies;

    private int totalCriticalVulnerbilities;
    private int totalHighVulnerbilities;
    private int totalMediumVulnerbilities;
    private int totalLowVulnerbilities;

    private  Dependency[]  dependencies;



    private RiskDistributionStats vulnerabilitiesRiskDistribution = new RiskDistributionStats("system");

    public int getTotalNumberOfCves() {
        return this.totalNumberOfCves;
    }

    public void setTotalNumberOfCves(int totalNumberOfCves) {
        this.totalNumberOfCves = totalNumberOfCves;
    }

    public int getTotalNumberOfThridPartyDependencies() {
        return this.totalNumberOfThridPartyDependencies;
    }

    public void setTotalNumberOfThridPartyDependencies(int totalNumberOfThridPartyDependencies) {
        this.totalNumberOfThridPartyDependencies = totalNumberOfThridPartyDependencies;
    }

    public RiskDistributionStats getVulnerabilitiesRiskDistribution() {
        return this.vulnerabilitiesRiskDistribution;
    }

    public void setVulnerabilitiesRiskDistribution(RiskDistributionStats vulnerabilitiesRiskDistribution) {
        this.vulnerabilitiesRiskDistribution = vulnerabilitiesRiskDistribution;
    } 
    
    
    public int getTotalCriticalVulnerbilities() {
        return this.totalCriticalVulnerbilities;
    }

    public void setTotalCriticalVulnerbilities(int totalCriticalVulnerbilities) {
        this.totalCriticalVulnerbilities = totalCriticalVulnerbilities;
    }

    public int getTotalHighVulnerbilities() {
        return this.totalHighVulnerbilities;
    }

    public void setTotalHighVulnerbilities(int totalHighVulnerbilities) {
        this.totalHighVulnerbilities = totalHighVulnerbilities;
    }

    public int getTotalMediumVulnerbilities() {
        return this.totalMediumVulnerbilities;
    }

    public void setTotalMediumVulnerbilities(int totalMediumVulnerbilities) {
        this.totalMediumVulnerbilities = totalMediumVulnerbilities;
    }

    public int getTotalLowVulnerbilities() {
        return this.totalLowVulnerbilities;
    }

    public void setTotalLowVulnerbilities(int totalLowVulnerbilities) {
        this.totalLowVulnerbilities = totalLowVulnerbilities;
    }
    
    public Dependency[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(Dependency[] dependencies) {
        this.dependencies = dependencies;
    }
}
