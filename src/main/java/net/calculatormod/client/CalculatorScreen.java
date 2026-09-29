package net.calculatormod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CalculatorScreen extends Screen {

    private String display = "0";
    private double currentValue = 0;
    private String pendingOperator = null;
    private boolean startNewNumber = true;

    private int cursorPos = 1;
    private int viewStart = 0;

    private static final int MAX_INPUT_DIGITS = 24;

    private static final int DISPLAY_WIDTH = 140;
    private static final int DISPLAY_HEIGHT = 20;
    private static final int TEXT_PADDING = 6;

    private static final String[] BUTTONS = {
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "0", "C", "=", "+"
    };

    private int equalsPressCount = 0;
    private long rainbowEndTime = 0; // 0 = not active

    private record NumberButtonInfo(String label, int x, int y, int size) {}
    private final List<NumberButtonInfo> numberButtons = new ArrayList<>();

    public CalculatorScreen() {
        super(Text.literal("Calculator"));
    }

    @Override
    protected void init() {
        super.init();
        numberButtons.clear();

        int buttonSize = 30;
        int spacing = 4;
        int cols = 4;
        int gridWidth = cols * buttonSize + (cols - 1) * spacing;

        int startX = (width - gridWidth) / 2;
        int startY = height / 2 - 40;

        for (int i = 0; i < BUTTONS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            String label = BUTTONS[i];

            int x = startX + col * (buttonSize + spacing);
            int y = startY + row * (buttonSize + spacing);

            addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> onButtonPress(label))
                    .dimensions(x, y, buttonSize, buttonSize)
                    .build());

            if (label.matches("[0-9]")) {
                numberButtons.add(new NumberButtonInfo(label, x, y, buttonSize));
            }
        }
    }

    private int getDisplayX() {
        return width / 2 - DISPLAY_WIDTH / 2;
    }

    private int getDisplayY() {
        return height / 2 - 70;
    }

    private void onButtonPress(String label) {
        if (display.equals("Error") && !label.equals("C")) {
            return;
        }

        if (label.equals("=")) {
            equalsPressCount++;
            if (equalsPressCount >= 10) {
                equalsPressCount = 0;
                triggerEasterEgg();
            }
        } else {
            equalsPressCount = 0;
        }

        switch (label) {
            case "C" -> {
                display = "0";
                currentValue = 0;
                pendingOperator = null;
                startNewNumber = true;
                cursorPos = display.length();
                viewStart = 0;
            }
            case "+", "-", "*", "/" -> {
                currentValue = Double.parseDouble(display);
                pendingOperator = label;
                startNewNumber = true;
                cursorPos = display.length();
                viewStart = 0;
            }
            case "=" -> {
                if (pendingOperator != null) {
                    double next = Double.parseDouble(display);

                    if (pendingOperator.equals("/") && next == 0) {
                        display = "Error";
                        pendingOperator = null;
                        startNewNumber = true;
                        cursorPos = display.length();
                        viewStart = 0;
                        return;
                    }

                    currentValue = applyOperator(currentValue, next, pendingOperator);

                    display = (Double.isNaN(currentValue) || Double.isInfinite(currentValue))
                            ? "Error"
                            : formatResult(currentValue);

                    pendingOperator = null;
                    startNewNumber = true;
                    cursorPos = display.length();
                    viewStart = 0;
                }
            }
            default -> {
                if (startNewNumber || display.equals("0")) {
                    display = label;
                    cursorPos = 1;
                    startNewNumber = false;
                } else if (display.length() < MAX_INPUT_DIGITS) {
                    display = display.substring(0, cursorPos) + label + display.substring(cursorPos);
                    cursorPos++;
                }
                adjustView();
            }
        }
    }

    private void triggerEasterEgg() {
        rainbowEndTime = System.currentTimeMillis() + 3000;
        MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.music(SoundEvents.ENTITY_PLAYER_LEVELUP)
        );
    }

    private double applyOperator(double a, double b, String op) {
        return switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;
            default -> b;
        };
    }

    private String formatResult(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private void adjustView() {
        int maxWidth = DISPLAY_WIDTH - TEXT_PADDING * 2;

        if (textRenderer.getWidth(display) <= maxWidth) {
            viewStart = 0;
            return;
        }

        if (viewStart > cursorPos) {
            viewStart = cursorPos;
        }

        while (viewStart < cursorPos
                && textRenderer.getWidth(display.substring(viewStart, cursorPos)) > maxWidth) {
            viewStart++;
        }
    }

    private int getVisibleEnd() {
        int maxWidth = DISPLAY_WIDTH - TEXT_PADDING * 2;
        int end = viewStart;

        while (end < display.length()
                && textRenderer.getWidth(display.substring(viewStart, end + 1)) <= maxWidth) {
            end++;
        }
        return end;
    }

    private int getTextStartX(String visibleText) {
        int displayX = getDisplayX();
        return displayX + DISPLAY_WIDTH - TEXT_PADDING - textRenderer.getWidth(visibleText);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!display.equals("Error")) {
            if (input.key() == GLFW.GLFW_KEY_LEFT) {
                cursorPos = Math.max(0, cursorPos - 1);
                adjustView();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_RIGHT) {
                cursorPos = Math.min(display.length(), cursorPos + 1);
                adjustView();
                return true;
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int displayX = getDisplayX();
        int displayY = getDisplayY();

        double mouseX = click.x();
        double mouseY = click.y();

        boolean insideBox = mouseX >= displayX && mouseX <= displayX + DISPLAY_WIDTH
                && mouseY >= displayY && mouseY <= displayY + DISPLAY_HEIGHT;

        if (insideBox && !display.equals("Error")) {
            int visibleEnd = getVisibleEnd();
            String visibleText = display.substring(viewStart, visibleEnd);
            int textStartX = getTextStartX(visibleText);

            double localX = mouseX - textStartX;

            int newCursor = visibleEnd;
            for (int i = viewStart; i <= visibleEnd; i++) {
                double widthUpTo = textRenderer.getWidth(display.substring(viewStart, i));
                if (widthUpTo >= localX) {
                    newCursor = i;
                    break;
                }
            }

            cursorPos = newCursor;
            adjustView();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int displayX = getDisplayX();
        int displayY = getDisplayY();

        context.fill(displayX, displayY, displayX + DISPLAY_WIDTH, displayY + DISPLAY_HEIGHT, 0xFF222222);

        int borderColor = 0xFFAAAAAA;
        context.fill(displayX, displayY, displayX + DISPLAY_WIDTH, displayY + 1, borderColor);
        context.fill(displayX, displayY + DISPLAY_HEIGHT - 1, displayX + DISPLAY_WIDTH, displayY + DISPLAY_HEIGHT, borderColor);
        context.fill(displayX, displayY, displayX + 1, displayY + DISPLAY_HEIGHT, borderColor);
        context.fill(displayX + DISPLAY_WIDTH - 1, displayY, displayX + DISPLAY_WIDTH, displayY + DISPLAY_HEIGHT, borderColor);

        int visibleEnd = getVisibleEnd();
        String visibleText = display.substring(viewStart, visibleEnd);
        int textStartX = getTextStartX(visibleText);
        int textY = displayY + 6;

        context.drawTextWithShadow(this.textRenderer, visibleText, textStartX, textY, 0xFFFFFFFF);

        if (!display.equals("Error") && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = textStartX + textRenderer.getWidth(display.substring(viewStart, cursorPos));
            context.fill(cursorX, textY - 1, cursorX + 1, textY + textRenderer.fontHeight, 0xFFFFFFFF);
        }

        renderRainbowEasterEgg(context);
    }

    private void renderRainbowEasterEgg(DrawContext context) {
        long now = System.currentTimeMillis();
        if (now >= rainbowEndTime) {
            return; // not active
        }

        for (int i = 0; i < numberButtons.size(); i++) {
            NumberButtonInfo btn = numberButtons.get(i);

            float hue = (((now / 15) + (i * 25)) % 360) / 360f;
            int color = 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 1f, 1f) & 0xFFFFFF);

            int textWidth = textRenderer.getWidth(btn.label());
            int textX = btn.x() + (btn.size() - textWidth) / 2;
            int textY = btn.y() + (btn.size() - textRenderer.fontHeight) / 2 + 1;

            context.drawTextWithShadow(this.textRenderer, btn.label(), textX, textY, color);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}