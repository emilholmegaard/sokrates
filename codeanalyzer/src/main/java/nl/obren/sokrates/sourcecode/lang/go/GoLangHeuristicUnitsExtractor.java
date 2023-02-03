/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.go;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

public class GoLangHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    @Override
    public boolean isUnitSignature(String line) {
        return RegexUtils.matchesEntirely(".*func .*[(].*", line);
    }
}
