/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

ace.define("ace/snippets/sass",["require","exports","module"], function(require, exports, module) {
"use strict";

exports.snippetText =undefined;
exports.scope = "sass";

});                (function() {
                    ace.require(["ace/snippets/sass"], function(m) {
                        if (typeof module == "object" && typeof exports == "object" && module) {
                            module.exports = m;
                        }
                    });
                })();
