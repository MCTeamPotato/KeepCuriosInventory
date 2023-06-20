package com.teampotato.keepcuriosinventory;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.event.CurioDropsEvent;

@Mod(KeepCuriosInventory.ID)
@Mod.EventBusSubscriber()
public class KeepCuriosInventory {
    public static final String ID = "keepcuriosinventory";

    @SubscribeEvent
    public static void onCuriosDrop(CurioDropsEvent event) {
        if (event.getEntity() instanceof Player) event.setCanceled(true);
    }
}