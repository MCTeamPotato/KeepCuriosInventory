package com.teampotato.keepcuriosinventory.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioDropsEvent;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.event.CuriosEventHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

@Mixin(value = CuriosEventHandler.class, remap = false)
public abstract class MixinCuriosEventHandler {
    @Shadow
    private static void handleDrops(LivingEntity livingEntity, List<Tuple<Predicate<ItemStack>, ICurio.DropRule>> dropRules, IDynamicStackHandler stacks, Collection<ItemEntity> drops, boolean keepInventory) {}

    /**
     * @author Kasualix
     * @reason avoid drop
     */
    @Overwrite
    @SubscribeEvent(
            priority = EventPriority.HIGHEST
    )
    public void playerDrops(LivingDropsEvent evt) {
        LivingEntity livingEntity = evt.getEntityLiving();
        if (!livingEntity.isSpectator()) {
            CuriosApi.getCuriosHelper().getCuriosHandler(livingEntity).ifPresent((handler) -> {
                Collection<ItemEntity> curioDrops = new ArrayList<>();
                DropRulesEvent dropRulesEvent = new DropRulesEvent(livingEntity, handler, evt.getSource(), evt.getLootingLevel(), evt.isRecentlyHit());
                MinecraftForge.EVENT_BUS.post(dropRulesEvent);
                List<Tuple<Predicate<ItemStack>, ICurio.DropRule>> dropRules = dropRulesEvent.getOverrides();
                boolean keepInventory = true;
                handler.getCurios().forEach((id, stacksHandler) -> {
                    handleDrops(livingEntity, dropRules, stacksHandler.getStacks(), curioDrops, keepInventory);
                    handleDrops(livingEntity, dropRules, stacksHandler.getCosmeticStacks(), curioDrops, keepInventory);
                });
                if (!MinecraftForge.EVENT_BUS.post(new CurioDropsEvent(
                        livingEntity,
                        handler,
                        evt.getSource(),
                        curioDrops,
                        evt.getLootingLevel(),
                        evt.isRecentlyHit()
                ))) {
                    evt.getDrops().addAll(curioDrops);
                }
            });
        }

    }
}
