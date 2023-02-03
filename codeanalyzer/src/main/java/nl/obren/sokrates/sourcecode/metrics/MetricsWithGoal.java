/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.metrics;

import java.util.ArrayList;
import java.util.List;

public class MetricsWithGoal {
    // A name of a goal
    private String goal = "";

    // A description of a goal
    private String description = "";

    // A list of controls
    private List<MetricRangeControl> controls = new ArrayList<>();

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<MetricRangeControl> getControls() {
        return controls;
    }

    public void setControls(List<MetricRangeControl> controls) {
        this.controls = controls;
    }
}
