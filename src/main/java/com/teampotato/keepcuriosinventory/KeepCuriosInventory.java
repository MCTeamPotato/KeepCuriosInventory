package com.teampotato.keepcuriosinventory;

import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

@Mod(KeepCuriosInventory.ID)
public class KeepCuriosInventory {
    public static final String ID = "keepcuriosinventory";
    public static final ForgeConfigSpec config;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> curiosBlacklist;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("KeepCuriosInventory");
        curiosBlacklist = builder.comment("Items here will always drop.").defineList("curiosBlacklist", Lists.newArrayList(), o -> o instanceof String);
        builder.pop();
        config = builder.build();
    }

    public KeepCuriosInventory() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, config);
    }
}