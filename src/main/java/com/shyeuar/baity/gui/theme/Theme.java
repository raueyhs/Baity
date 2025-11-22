package com.shyeuar.baity.gui.theme;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import java.awt.*;

@Environment(EnvType.CLIENT)
public class Theme {
    public Color BG   = new Color(18, 18, 20);     // 主背景
    public Color BG_2 = new Color(28, 28, 32);    // 次级面板背景
    public Color BG_3 = new Color(98, 74, 255);    // 强调色（蓝紫）
    public Color Modules = new Color(26, 26, 30);  // 卡片背景
    public Color FONT_C = new Color(245, 245, 248); // 主文本
    public Color FONT   = new Color(164, 168, 176); // 次文本
    
    public void setDark(){
        BG   = new Color(18, 18, 20);
        BG_2 = new Color(28, 28, 32);
        BG_3 = new Color(98, 74, 255);
        Modules = new Color(26, 26, 30);
        FONT   = new Color(164, 168, 176);
        FONT_C = new Color(245, 245, 248);
    }
}