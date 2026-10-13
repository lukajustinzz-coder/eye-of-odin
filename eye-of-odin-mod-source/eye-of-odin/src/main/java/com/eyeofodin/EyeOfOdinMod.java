package com.eyeofodin;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EyeOfOdinMod implements ModInitializer {
    public static final String MOD_ID = "eyeofodin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Item EYE_OF_ODIN;
    public static SoundEvent DODGE_SOUND;
    public static SoundEvent OVERHEATED_SOUND;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        // The item
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id("eye_of_odin"));
        EYE_OF_ODIN = Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new Item(new Item.Properties().setId(itemKey).stacksTo(1).rarity(Rarity.EPIC)));

        // Your sounds (defined in assets/eyeofodin/sounds.json)
        DODGE_SOUND = Registry.register(BuiltInRegistries.SOUND_EVENT, id("dodge"),
                SoundEvent.createVariableRangeEvent(id("dodge")));
        OVERHEATED_SOUND = Registry.register(BuiltInRegistries.SOUND_EVENT, id("overheated"),
                SoundEvent.createVariableRangeEvent(id("overheated")));

        // Dodge: runs before an entity takes damage. Returning false cancels the damage.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                (entity, source, amount) -> EyeLogic.allowDamage(entity, source, amount));

        // Equip tracking and overheat cooldown
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int now = server.getTickCount();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                EyeLogic.tick(player, now);
            }
        });

        LOGGER.info("Eye of Odin loaded.");
    }
}
