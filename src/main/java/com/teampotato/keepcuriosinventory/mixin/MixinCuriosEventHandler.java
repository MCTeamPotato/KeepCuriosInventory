package com.teampotato.keepcuriosinventory.mixin;

import com.teampotato.keepcuriosinventory.KeepCuriosInventory;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioDropsEvent;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.event.CuriosEventHandler;

import java.util.*;
import java.util.function.Predicate;

@Mixin(value = CuriosEventHandler.class, remap = false)
public abstract class MixinCuriosEventHandler {
    @Shadow
    private static ItemEntity getDroppedItem(ItemStack droppedItem, LivingEntity livingEntity) {
        throw new RuntimeException();
    }

    /**
     * @author Kasualix
     * @reason impl blacklist
     */
    @Overwrite
    private static void handleDrops(String identifier, LivingEntity livingEntity,
                                    List<Tuple<Predicate<ItemStack>, ICurio.DropRule>> dropRules,
                                    NonNullList<Boolean> renders, IDynamicStackHandler stacks,
                                    boolean cosmetic, Collection<ItemEntity> drops,
                                    boolean keepInventory, LivingDropsEvent evt) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            SlotContext slotContext = new SlotContext(identifier, livingEntity, i, cosmetic,
                    renders.size() > i && renders.get(i));

            if (!stack.isEmpty()) {
                ICurio.DropRule dropRuleOverride = null;

                for (Tuple<Predicate<ItemStack>, ICurio.DropRule> override : dropRules) {

                    if (override.getA().test(stack)) {
                        dropRuleOverride = override.getB();
                    }
                }
                ICurio.DropRule dropRule = dropRuleOverride != null ? dropRuleOverride :
                        CuriosApi.getCuriosHelper().getCurio(stack).map(curio -> curio
                                .getDropRule(slotContext, evt.getSource(), evt.getLootingLevel(),
                                        evt.isRecentlyHit())).orElse(ICurio.DropRule.DEFAULT);

                if ((dropRule == ICurio.DropRule.DEFAULT && keepInventory) || dropRule == ICurio.DropRule.ALWAYS_KEEP) {
                    if (!KeepCuriosInventory.curiosBlacklist.get().contains(
                            Objects.requireNonNull(stack.getItem().getRegistryName()).toString())){
                        continue;
                    }
                }

                if (!EnchantmentHelper.hasVanishingCurse(stack) && dropRule != ICurio.DropRule.DESTROY) {
                    drops.add(getDroppedItem(stack, livingEntity));
                }
                stacks.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }
    /**
     * @author Kasualix
     * @reason avoid drop
     */
    @Overwrite
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void playerDrops(LivingDropsEvent evt) {

        LivingEntity livingEntity = evt.getEntityLiving();

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
