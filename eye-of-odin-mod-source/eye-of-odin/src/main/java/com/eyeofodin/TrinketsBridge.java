package com.eyeofodin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Asks "Trinkets Updated" whether the Eye of Odin is in the player's eye slot.
 * Uses reflection so this mod still builds and loads even if Trinkets is missing.
 * If Trinkets is not installed, holding the Eye in the offhand works instead.
 */
public final class TrinketsBridge {
    private static boolean resolved = false;
    private static Method getAttachment = null;
    private static final Map<Class<?>, Optional<Method>> IS_EQUIPPED = new ConcurrentHashMap<>();

    private TrinketsBridge() {}

    public static boolean isEquipped(LivingEntity entity) {
        if (!resolved) resolve();
        if (getAttachment == null) {
            // Trinkets not found: fallback to the offhand
            return entity.getOffhandItem().is(EyeOfOdinMod.EYE_OF_ODIN);
        }
        try {
            Object attachment = getAttachment.invoke(null, entity);
            if (attachment instanceof Optional<?> opt) attachment = opt.orElse(null);
            if (attachment == null) return false;

            Method m = IS_EQUIPPED.computeIfAbsent(attachment.getClass(), TrinketsBridge::findIsEquipped).orElse(null);
            if (m == null) return false;

            Object arg;
            if (m.getParameterTypes()[0] == Item.class) {
                arg = EyeOfOdinMod.EYE_OF_ODIN;
            } else {
                Predicate<ItemStack> pred = stack -> stack.is(EyeOfOdinMod.EYE_OF_ODIN);
                arg = pred;
            }
            Object result = m.invoke(attachment, arg);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> api = Class.forName("eu.pb4.trinkets.api.TrinketsApi");
            for (Method m : api.getMethods()) {
                if (m.getName().equals("getAttachment") && m.getParameterCount() == 1
                        && Modifier.isStatic(m.getModifiers())
                        && m.getParameterTypes()[0].isAssignableFrom(LivingEntity.class)) {
                    getAttachment = m;
                    break;
                }
            }
            if (getAttachment == null) {
                EyeOfOdinMod.LOGGER.warn("Trinkets found but getAttachment was not; using offhand fallback.");
            } else {
                EyeOfOdinMod.LOGGER.info("Trinkets found: the Eye of Odin works from the face slot.");
            }
        } catch (Throwable t) {
            EyeOfOdinMod.LOGGER.warn("Trinkets Updated not found; the Eye of Odin works from the OFFHAND instead.");
        }
    }

    private static Optional<Method> findIsEquipped(Class<?> cls) {
        Method predicateVersion = null;
        for (Method m : cls.getMethods()) {
            String n = m.getName();
            if ((n.equals("isEquipped") || n.equals("isEquiped")) && m.getParameterCount() == 1) {
                if (m.getParameterTypes()[0] == Item.class) return Optional.of(m);
                if (Predicate.class.isAssignableFrom(m.getParameterTypes()[0])) predicateVersion = m;
            }
        }
        return Optional.ofNullable(predicateVersion);
    }
}
