package aiplayer;

import arc.scene.ui.TextField;
import mindustry.ui.dialogs.BaseDialog;

public class AICommandConsole extends BaseDialog {
    private final StringBuilder history = new StringBuilder();
    private TextField input;

    public AICommandConsole() {
        super("AI Commands");
        shouldPause = false;
        addCloseButton();
        history.append("AI command console.\nExamples: status, calc, llm on, miners 4.\nNatural language: order построй больше турелей, order атакуй врага.\nAsk LLM: ask как дела с энергией?\nDemolish: demolish here (курсор на строении) или demolish x y.\n");

        cont.defaults().left().pad(4f);
        cont.labelWrap(() -> history.toString()).width(560f).left().row();

        input = new TextField();
        input.setMessageText("status / order построй больше турелей / ask вопрос");
        input.setTextFieldListener((field, c) -> {
            if (c == '\n' || c == '\r') run();
        });
        cont.add(input).width(560f).row();

        cont.table(row -> {
            row.button("Run", this::run).size(80f, 40f);
            row.button("Status", () -> quick("status")).size(90f, 40f);
            row.button("Calc", () -> quick("calc")).size(80f, 40f);
            row.button("LLM on", () -> quick("llm on")).size(90f, 40f);
            row.button("LLM now", () -> quick("llm now")).size(100f, 40f);
        }).row();

        cont.table(row -> {
            row.button("Order", () -> input.setText("order ")).size(90f, 40f);
            row.button("Ask", () -> input.setText("ask ")).size(80f, 40f);
            row.button("Demolish", () -> input.setText("demolish ")).size(110f, 40f);
            row.button("Setup", () -> new AISettingsDialog().show()).size(100f, 40f);
            row.button("Clear", () -> history.setLength(0)).size(90f, 40f);
        }).row();

    }

    private void quick(String command) {
        input.setText(command);
        run();
    }

    private void run() {
        String raw = input.getText();
        if (raw == null || raw.trim().isEmpty()) return;
        append("> " + raw.trim());
        String response;
        try {
            response = AICommandHandler.execute(raw, this::append);
        } catch (Throwable t) {
            response = "Error: " + t;
        }
        if (response != null && !response.isEmpty()) append(response);
        input.setText("");
    }

    private void append(String text) {
        history.append(text).append('\n');
        if (history.length() > 6000) history.delete(0, history.length() - 6000);
    }
}
