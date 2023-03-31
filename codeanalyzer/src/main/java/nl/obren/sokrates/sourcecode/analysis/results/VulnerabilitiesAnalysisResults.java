/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import java.util.ArrayList;
import java.util.HashMap;
import nl.obren.sokrates.sourcecode.analysis.vulnerabilities.Dependency;
import nl.obren.sokrates.sourcecode.analysis.vulnerabilities.Root;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;


public class VulnerabilitiesAnalysisResults {

    private int totalNumberOfCves;
    private int totalNumberOfThirdPartyDependencies;
    private int totalVulnerableDependencies;

    private String[] vulnerableDependencies;
    private HashMap<String, String[]> vulnerableDependenciesToPackage;
    private HashMap<String, String> vulnerableDependenciesToPackageUrl;
    private HashMap<String, String[]> vulnerableDependenciesToVulnerabilityId;
    private HashMap<String, String> vulnerableDependenciesToHighestSeverity;
    private HashMap<String, Integer> vulnerableDependenciesToCVECount;
    private HashMap<String, Integer> vulnerableDependenciesToEvidenceCount;

    private int totalCriticalVulnerabilities;
    private int totalHighVulnerabilities;
    private int totalMediumVulnerabilities;
    private int totalModerateVulnerabilities;
    private int totalLowVulnerabilities;

    private ArrayList<Dependency> dependencies;
    private Root root;

    private RiskDistributionStats vulnerabilitiesRiskDistribution = new RiskDistributionStats("system");

    public int getTotalNumberOfCves() {
        return this.totalNumberOfCves;
    }

    public void setTotalNumberOfCves(int totalNumberOfCves) {
        this.totalNumberOfCves = totalNumberOfCves;
    }

    public int getTotalNumberOfThirdPartyDependencies() {
        return this.totalNumberOfThirdPartyDependencies;
    }

    public void setTotalNumberOfThirdPartyDependencies(int totalNumberOfThirdPartyDependencies) {
        this.totalNumberOfThirdPartyDependencies = totalNumberOfThirdPartyDependencies;
    }

    public int getTotalVulnerableDependencies() {
        return this.totalVulnerableDependencies;
    }

    public void setTotalVulnerableDependencies(int totalVulnerableDependencies) {
        this.totalVulnerableDependencies = totalVulnerableDependencies;
    }

    public RiskDistributionStats getVulnerabilitiesRiskDistribution() {
        return this.vulnerabilitiesRiskDistribution;
    }

    public void setVulnerabilitiesRiskDistribution(RiskDistributionStats vulnerabilitiesRiskDistribution) {
        this.vulnerabilitiesRiskDistribution = vulnerabilitiesRiskDistribution;
    }

    public int getTotalCriticalVulnerabilities() {
        return this.totalCriticalVulnerabilities;
    }

    public void setTotalCriticalVulnerabilities(int totalCriticalVulnerabilities) {
        this.totalCriticalVulnerabilities = totalCriticalVulnerabilities;
    }

    public int getTotalHighVulnerabilities() {
        return this.totalHighVulnerabilities;
    }

    public void setTotalHighVulnerabilities(int totalHighVulnerabilities) {
        this.totalHighVulnerabilities = totalHighVulnerabilities;
    }

    public int getTotalMediumVulnerabilities() {
        return this.totalMediumVulnerabilities;
    }

    public void setTotalMediumVulnerabilities(int totalMediumVulnerabilities) {
        this.totalMediumVulnerabilities = totalMediumVulnerabilities;
    }

    public int getTotalModerateVulnerabilities() {
        return this.totalModerateVulnerabilities;
    }

    public void setTotalModerateVulnerabilities(int totalModerateVulnerabilities) {
        this.totalModerateVulnerabilities = totalModerateVulnerabilities;
    }

    public int getTotalLowVulnerabilities() {
        return this.totalLowVulnerabilities;
    }

    public void setTotalLowVulnerabilities(int totalLowVulnerabilities) {
        this.totalLowVulnerabilities = totalLowVulnerabilities;
    }

    public ArrayList<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(ArrayList<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public Root getRoot() {
        return root;
    }

    public void setRoot(Root root) {
        this.root = root;
    }

    public String[] getVulnerableDependencies() {
        if (this.vulnerableDependencies == null) {
            this.vulnerableDependencies = new String[0];
        }
        return this.vulnerableDependencies;
    }

    public void setVulnerableDependencies(String[] vulnerableDependencies) {
        this.vulnerableDependencies = vulnerableDependencies;
    }

    public HashMap<String, String[]> getVulnerableDependenciesToPackage() {
        if (this.vulnerableDependenciesToPackage == null) {
            this.vulnerableDependenciesToPackage = new HashMap<String, String[]>();
        }
        return this.vulnerableDependenciesToPackage;
    }

    public void setVulnerableDependenciesToPackage(HashMap<String, String[]> vulnerableDependenciesToPackage) {
        this.vulnerableDependenciesToPackage = vulnerableDependenciesToPackage;
    }

    public HashMap<String, String> getVulnerableDependenciesToPackageUrl() {
        if (this.vulnerableDependenciesToPackageUrl == null) {
            this.vulnerableDependenciesToPackageUrl = new HashMap<String, String>();
        }
        return this.vulnerableDependenciesToPackageUrl;
    }

    public void setVulnerableDependenciesToPackageUrl(HashMap<String, String> vulnerableDependenciesToPackageUrl) {
        this.vulnerableDependenciesToPackageUrl = vulnerableDependenciesToPackageUrl;
    }

    public HashMap<String, String[]> getVulnerableDependenciesToVulnerabilityId() {
        if (this.vulnerableDependenciesToVulnerabilityId == null) {
            this.vulnerableDependenciesToVulnerabilityId = new HashMap<String, String[]>();
        }
        return this.vulnerableDependenciesToVulnerabilityId;
    }

    public void setVulnerableDependenciesToVulnerabilityId(
            HashMap<String, String[]> vulnerableDependenciesToVulnerabilityId) {
        this.vulnerableDependenciesToVulnerabilityId = vulnerableDependenciesToVulnerabilityId;
    }

    public HashMap<String, String> getVulnerableDependenciesToHighestSeverity() {
        if (this.vulnerableDependenciesToHighestSeverity == null) {
            this.vulnerableDependenciesToHighestSeverity = new HashMap<String, String>();
        }
        return this.vulnerableDependenciesToHighestSeverity;
    }

    public void setVulnerableDependenciesToHighestSeverity(
            HashMap<String, String> vulnerableDependenciesToHighestSeverity) {
        this.vulnerableDependenciesToHighestSeverity = vulnerableDependenciesToHighestSeverity;
    }

    public HashMap<String, Integer> getVulnerableDependenciesToCVECount() {
        if (this.vulnerableDependenciesToCVECount == null) {
            this.vulnerableDependenciesToCVECount = new HashMap<String, Integer>();
        }
        return this.vulnerableDependenciesToCVECount;
    }

    public void setVulnerableDependenciesToCVECount(
            HashMap<String, Integer> vulnerableDependenciesToCVECount) {
        this.vulnerableDependenciesToCVECount = vulnerableDependenciesToCVECount;
    }

    public HashMap<String, Integer> getVulnerableDependenciesToEvidenceCount() {
        if (this.vulnerableDependenciesToEvidenceCount == null) {
            this.vulnerableDependenciesToEvidenceCount = new HashMap<String, Integer>();
        }
        return this.vulnerableDependenciesToEvidenceCount;
    }

    public void setVulnerableDependenciesToEvidenceCount(
            HashMap<String, Integer> vulnerableDependenciesToEvidenceCount) {
        this.vulnerableDependenciesToEvidenceCount = vulnerableDependenciesToEvidenceCount;
    }
}
