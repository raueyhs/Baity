package com.shyeuar.baity.gui.modules;

import com.shyeuar.baity.gui.values.ModuleCategory;
import com.shyeuar.baity.gui.values.Option;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ModuleManager {
    private static final ArrayList<Module> modules = new ArrayList<>();
    
    public static void init() {
        Module smolPeopleModule = new Module("SmolPeople", "SmolPeople", ModuleCategory.FUN);
        smolPeopleModule.addValue(new Option("render third-person-back crosshair", "render third-person-back crosshair", true, ModuleCategory.FUN));
        modules.add(smolPeopleModule);
        
        Module blockAnimationModule = new Module("BlockAnimation", "BlockAnimation", ModuleCategory.FUN);
        modules.add(blockAnimationModule);
        
        Module pepCatModule = new Module("PepCat", "PepCat", ModuleCategory.FUN);
        pepCatModule.setEnabled(true); 
        modules.add(pepCatModule);
        
        Module reminderModule = new Module("Reminder", "Reminder", ModuleCategory.QOL);
        reminderModule.addValue(new Option("cookie buff reminder", "cookie buff reminder", true, ModuleCategory.QOL));
        reminderModule.addValue(new Option("god potion reminder", "god potion reminder", true, ModuleCategory.QOL));
        reminderModule.addValue(new Option("meowalert", "meowalert", true, ModuleCategory.QOL));
        modules.add(reminderModule);
        
        Module clickGUI = new Module("ClickGUI", "ClickGUI", ModuleCategory.HUD);
        clickGUI.setEnabled(true); 
        modules.add(clickGUI);

        Module playerEspModule = new Module("PlayerESP", "PlayerESP", ModuleCategory.RENDER);
        playerEspModule.addValue(new Option("show distance", "show distance", true, ModuleCategory.RENDER)); 
        playerEspModule.addValue(new Option("show own nametag", "show own nametag", false, ModuleCategory.RENDER)); 
        modules.add(playerEspModule);
    }
    
    public static List<Module> getModules() {
        return modules;
    }
    
    public static List<Module> getModulesByCategory(ModuleCategory category) {
        return modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public static Module getModuleByName(String name) {
        return modules.stream()
                .filter(module -> module.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}

