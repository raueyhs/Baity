package com.shyeuar.baity.features.nucleusscanner;

public enum NucleusFamily {
    AMBER_CRYSTAL("amber crystal", "Amber Crystal", "orange"),
    AMETHYST_CRYSTAL("amethyst crystal", "Amethyst Crystal", "purple"),
    JADE_CRYSTAL("jade crystal", "Jade Crystal", "green"),
    SAPPHIRE_CRYSTAL("sapphire crystal", "Sapphire Crystal", "cyan"),
    TOPAZ_CRYSTAL("topaz crystal", "Topaz Crystal", "yellow"),
    CORLEONE("corleone", "corleone", "lime"),
    FAIRY_GROTTO("fairy grotto", "fairy grotto", "magenta"),
    GOLDEN_DRAGON("golden dragon", "golden dragon", "white"),
    KEY_GUARDIAN("key guardian", "key guardian", "purple"),
    ODAWA("odawa", "odawa", "gray"),
    PETE("pete", "pete", "brown"),
    XALX("xalx", "xalx", "olive");

    private final String key;
    private final String displayName;
    private final String defaultColorName;

    NucleusFamily(String key, String displayName, String defaultColorName) {
        this.key = key;
        this.displayName = displayName;
        this.defaultColorName = defaultColorName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public String defaultColorName() {
        return defaultColorName;
    }
}
