package com.shyeuar.baity.features.radialmenu;

import com.shyeuar.baity.features.radialmenu.data.RadialMenuModels;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.radial.RadialWheelRenderer;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.utils.SoundUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class DynamicRadialMenuScreen extends Screen {

    private static double savedMouseX = -1;
    private static double savedMouseY = -1;

    private final int keybind;
    private final RadialMenuModels.RadialPreset preset;
    private final String layerId;
    private final DynamicRadialMenuScreen parentScreen;

    private int hoveredSection = -1;
    private boolean wasKeyPressed = true;

    public static void setInitialMousePosition(double x, double y) {
        savedMouseX = x;
        savedMouseY = y;
    }

    public DynamicRadialMenuScreen(int keybind, RadialMenuModels.RadialPreset preset, String layerId,
                                   DynamicRadialMenuScreen parentScreen) {
        super(Component.literal("Radial Menu"));
        this.keybind = keybind;
        this.preset = preset;
        this.layerId = layerId;
        this.parentScreen = parentScreen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        if (savedMouseX >= 0 && savedMouseY >= 0 && this.minecraft != null) {
            GLFW.glfwSetCursorPos(this.minecraft.getWindow().handle(), savedMouseX, savedMouseY);
            savedMouseX = -1;
            savedMouseY = -1;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer == null) {
            return;
        }

        float ratio = ClickGuiState.fixedScaleRatio(this.minecraft);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        mouseX = Math.round(ClickGuiState.fixedCoord(mouseX, centerX, ratio));
        mouseY = Math.round(ClickGuiState.fixedCoord(mouseY, centerY, ratio));

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.scale(ratio, ratio);
        pose.translate(-centerX, -centerY);
        try {
            updateHover(centerX, centerY, mouseX, mouseY, layer.slots.size());

            RadialWheelRenderer.drawWheel(context, centerX, centerY);
            int sectionCount = layer.slots.size();
            if (sectionCount > 0) {
                double anglePerSection = 360.0 / sectionCount;
                double startAngle = RadialWheelRenderer.getStartAngle(sectionCount);
                RadialWheelRenderer.drawSectorDividers(context, centerX, centerY, sectionCount, startAngle, anglePerSection);
                if (hoveredSection >= 0 && hoveredSection < sectionCount) {
                    double sectionStart = startAngle + hoveredSection * anglePerSection;
                    RadialWheelRenderer.drawHoveredSector(context, centerX, centerY, sectionStart, sectionStart + anglePerSection);
                }
                for (int i = 0; i < sectionCount; i++) {
                    float[] iconPos = RadialWheelRenderer.sectorCenter(
                            centerX, centerY, startAngle, anglePerSection, i,
                            RadialWheelRenderer.INNER_RADIUS, RadialWheelRenderer.OUTER_RADIUS);
                    RadialSlotRenderer.drawSlotIcon(context, this.font, layer.slots.get(i), iconPos[0], iconPos[1]);
                }
                RadialSlotRenderer.drawSlotLabels(context, this.font, centerX, centerY, startAngle, anglePerSection,
                        sectionCount, layer.slots, hoveredSection);
            }

            if (parentScreen == null) {
                RadialWheelRenderer.drawCenterPlayerHead(context, centerX, centerY);
            } else if (isSecondLevel()) {
                RadialWheelRenderer.drawCenter(context, centerX, centerY, RadialWheelRenderer.CenterStyle.EXIT);
            } else {
                RadialWheelRenderer.drawCenter(context, centerX, centerY, RadialWheelRenderer.CenterStyle.BACK);
            }
        } finally {
            pose.popMatrix();
        }
    }

    @Override
    public void tick() {
        Minecraft client = this.minecraft;
        if (client == null) {
            return;
        }
        long windowHandle = client.getWindow().handle();
        boolean isKeyPressed = KeyMappingUtils.isKeyPressed(windowHandle, keybind);
        if (parentScreen == null) {
            if (!isKeyPressed && wasKeyPressed) {
                activateSelection(this.minecraft, false);
            }
        }
        wasKeyPressed = isKeyPressed;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isInsideWindow) {
        if (click.button() == 0 && parentScreen != null) {
            activateSelection(this.minecraft, true);
            return true;
        }
        return super.mouseClicked(click, isInsideWindow);
    }

    @Override
    public void onClose() {
        RadialMenu.forceClose(this.minecraft);
        super.onClose();
    }

    private int[] transformedMouse(Minecraft client) {
        float ratio = ClickGuiState.fixedScaleRatio(client);
        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = client.getWindow().getGuiScaledHeight() / 2;
        var window = client.getWindow();
        float mouseX = (float) (client.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth());
        float mouseY = (float) (client.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight());
        return new int[]{
                Math.round(ClickGuiState.fixedCoord(mouseX, centerX, ratio)),
                Math.round(ClickGuiState.fixedCoord(mouseY, centerY, ratio))
        };
    }

    private void activateSelection(Minecraft client, boolean fromMouseClick) {
        if (client == null) {
            return;
        }
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer == null) {
            closeRadial(client);
            return;
        }

        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = client.getWindow().getGuiScaledHeight() / 2;
        int[] mouse = transformedMouse(client);
        updateHover(centerX, centerY, mouse[0], mouse[1], layer.slots.size());

        double dx = mouse[0] - centerX;
        double dy = mouse[1] - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= RadialWheelRenderer.INNER_RADIUS + 2) {
            if (fromMouseClick) {
                SoundUtils.playWoodenButton();
            }
            if (isSecondLevel()) {
                closeRadial(client);
            } else if (parentScreen != null) {
                setInitialMousePosition(client.mouseHandler.xpos(), client.mouseHandler.ypos());
                client.gui.setScreen(parentScreen);
            } else {
                closeRadial(client);
            }
            return;
        }

        if (hoveredSection < 0 || hoveredSection >= layer.slots.size()) {
            if (!fromMouseClick) {
                closeRadial(client);
            }
            return;
        }

        RadialMenuModels.RadialSlot slot = layer.slots.get(hoveredSection);

        boolean hasCommand = slot.command != null && !slot.command.isBlank();
        boolean hasChild = slot.childLayerId != null && preset.layers.containsKey(slot.childLayerId);
        boolean playSectorSound = fromMouseClick || parentScreen == null;

        if (hasChild && !hasCommand) {
            if (playSectorSound) {
                SoundUtils.playWoodenButton();
            }
            RadialMenu.forceClose(client);
            setInitialMousePosition(client.mouseHandler.xpos(), client.mouseHandler.ypos());
            client.gui.setScreen(new DynamicRadialMenuScreen(keybind, preset, slot.childLayerId, this));
            return;
        }

        if (hasCommand) {
            RadialMenu.executeCommand(client, slot.command);
        }

        if (hasChild) {
            if (playSectorSound) {
                SoundUtils.playWoodenButton();
            }
            RadialMenu.forceClose(client);
            setInitialMousePosition(client.mouseHandler.xpos(), client.mouseHandler.ypos());
            client.gui.setScreen(new DynamicRadialMenuScreen(keybind, preset, slot.childLayerId, this));
            return;
        }

        if (playSectorSound && hasCommand) {
            SoundUtils.playWoodenButton();
        }
        closeRadial(client);
    }

    private void updateHover(int centerX, int centerY, int mouseX, int mouseY, int sectionCount) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        hoveredSection = -1;
        if (sectionCount <= 0) {
            return;
        }
        if (distance > RadialWheelRenderer.INNER_RADIUS && distance < RadialWheelRenderer.OUTER_RADIUS + 20) {
            double degrees = Math.toDegrees(Math.atan2(dy, dx));
            if (degrees < 0) {
                degrees += 360;
            }
            hoveredSection = RadialWheelRenderer.getSectionFromAngle(degrees, sectionCount);
        }
    }

    private boolean isSecondLevel() {
        return parentScreen != null && parentScreen.parentScreen == null;
    }

    private void closeRadial(Minecraft client) {
        RadialMenu.forceClose(client);
        client.gui.setScreen(null);
    }
}
