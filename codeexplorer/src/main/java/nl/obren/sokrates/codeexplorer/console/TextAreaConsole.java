/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.console;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.codeexplorer.common.ProgressFeedbackPane;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TextAreaConsole extends BorderPane implements Console {
    private TextArea textArea = new TextArea();
    private ProgressFeedbackPane progressFeedbackPane = new ProgressFeedbackPane();
    private ProgressFeedback progressFeedback;
    private long startTime = System.currentTimeMillis();

    public TextAreaConsole() {
        progressFeedback = createProgressFeedback();

        setTop(progressFeedbackPane);
        setCenter(textArea);
    }

    private String getPrefix() {
        long timeInSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long minutes = timeInSeconds / 60;
        long seconds = timeInSeconds % 60;
        DecimalFormat decimalFormat = new DecimalFormat("00");
        decimalFormat.setDecimalFormatSymbols( new DecimalFormatSymbols(Locale.ENGLISH));
        return decimalFormat.format(minutes) + ":" + decimalFormat.format(seconds) + "\t";
    }

    private ProgressFeedback createProgressFeedback() {
        return new ProgressFeedback() {
            public void clear() {
                startTime = System.currentTimeMillis();
                Platform.runLater(() -> textArea.clear());
            }

            public void start() {
                progressFeedbackPane.getProgressFeedback().start();
            }

            public void end() {
                progressFeedbackPane.getProgressFeedback().end();
            }

            public synchronized void setText(String text) {
                Platform.runLater(() -> log(getPrefix() + text));
            }

            public void setDetailedText(String text) {
                progressFeedbackPane.getProgressFeedback().setText(text);
            }

            public boolean canceled() {
                if (progressFeedbackPane.getProgressFeedback().canceled()) {
                    setText("Cancelling processing...");
                }
                return progressFeedbackPane.getProgressFeedback().canceled();
            }

            public void progress(int currentValue, int endValue) {
                progressFeedbackPane.getProgressFeedback().progress(currentValue, endValue);
            }
        };
    }

    @Override
    public void log(String text) {
        textArea.appendText(text + "\n");
    }

    public ProgressFeedback getProgressFeedback() {
        return progressFeedback;
    }

}
