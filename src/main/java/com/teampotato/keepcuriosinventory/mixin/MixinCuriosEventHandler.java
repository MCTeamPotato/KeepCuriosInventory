package com.teampotato.keepcuriosinventory.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.event.CuriosEventHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(value = CuriosEventHandler.class, remap = false)
public abstract class MixinCuriosEventHandler {
    @Shadow
    private static void handleDrops(String identifier, LivingEntity livingEntity, List<Tuple<Predicate<ItemStack>, ICurio.DropRule>> dropRules, NonNullList<Boolean> renders, IDynamicStackHandler stacks, boolean cosmetic, Collection<ItemEntity> drops, boolean keepInventory, LivingDropsEvent evt) {}

    /**
     * @author Kasualix
     * @reason avoid drop
     */
    @Overwrite
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void playerDrops(LivingDropsEvent evt) {

        LivingEntity livingEntity = evt.getEntity();

        if (!livingEntity.isSpectator()) {

            CuriosApi.getCuriosHelper().getCuriosHandler(livingEntity).ifPresent(handler -> {
                Collection<ItemEntity> drops = evt.getDrops();
                Collection<ItemEntity> curioDrops = new ArrayList<>();
                Map<String, ICurioStacksHandler> curios = handler.getCurios();

                DropRulesEvent dropRulesEvent = new DropRulesEvent(livingEntity, handler, evt.getSource(),
                        evt.getLootingLevel(), evt.isRecentlyHit());
                MinecraftForge.EVENT_BUS.post(dropRulesEvent);
                List<Tuple<Predicate<ItemStack>, ICurio.DropRule>> dropRules = dropRulesEvent.getOverrides();

                boolean keepInventory = true;

                curios.forEach((id, stacksHandler) -> {
                    handleDrops(id, livingEntity, dropRules, stacksHandler.getRenders(),
                            stacksHandler.getStacks(), false, curioDrops, keepInventory, evt);
                    handleDrops(id, livingEntity, dropRules, stacksHandler.getRenders(),
                            stacksHandler.getCosmeticStacks(), true, curioDrops, keepInventory, evt);
                });

                if (!MinecraftForge.EVENT_BUS.post(
                        new CurioDropsEvent(livingEntity, handler, evt.getSource(), curioDrops,
                                evt.getLootingLevel(), evt.isRecentlyHit()))) {
                    drops.addAll(curioDrops);
                }
            });
        }
    }
}
