/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;

import java.util.List;

public class PrependOperation extends StringOperation {
    public PrependOperation() {
        super("prepend");
    }

    public PrependOperation(List<String> params) {
        this();
        this.setParams(params);
    }

    @Override
    public String exec(String input) {
        StringBuilder prefix = new StringBuilder();

        getParams().forEach(prefix::append);

        return prefix.toString() + input;
    }
}
