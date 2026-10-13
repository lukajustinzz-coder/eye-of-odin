package com.eyeofodin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EyeLogic {
    // ---- Settings you can change ----
    public static final int MAX_STACKS = 30;          // stacks gained when equipped
    public static final int OVERHEAT_TICKS = 20 * 30; // overheat lasts 30 seconds (20 ticks = 1 second)
    public static final double DODGE_DISTANCE = 3.0;  // how far you teleport (blocks)
    public static final float CRIT_MULTIPLIER = 1.5f; // same as a vanilla critical hit
    // ---------------------------------

    private static final Map<UUID, EyeState> STATES = new HashMap<>();

    private EyeLogic() {}

    public static EyeState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), k -> new EyeState());
    }

    /** Called every server tick for every player. */
    public static void tick(ServerPlayer player, int now) {
        EyeState s = state(player);
        boolean equipped = TrinketsBridge.isEquipped(player);

        if (equipped && !s.wasEquipped) {
            // Just equipped: "When this unit enters the Encounter, gain 30 Eye of Precognition"
            s.stacks = MAX_STACKS;
            s.overheated = false;
        }
        s.wasEquipped = equipped;
        if (!equipped) return;

        if (s.overheated && now >= s.overheatUntilTick) {
            s.overheated = false;
            s.stacks = MAX_STACKS;
            player.sendSystemMessage(Component.literal("Eye of Precognition: " + s.stacks), true);
        }
    }

    /** Dodge logic. Return false to cancel the damage. */
    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) return true;
        if (!(player.level() instanceof ServerLevel level)) return true;

        // Only attacks from creatures, projectiles, explosions of creatures, etc.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (attacker == null && direct == null) return true; // fall, lava, drowning...

        if (player.invulnerableTime > 10) return true; // vanilla hit-cooldown, leave it alone

        EyeState s = state(player);
        if (s.overheated || !TrinketsBridge.isEquipped(player)) return true;

        int now = level.getServer().getTickCount();

        // Several hits in the same moment (sweep, multishot) count as one dodge
        if (now - s.lastDodgeTick < 5) return false;
        s.lastDodgeTick = now;

        Entity origin = direct != null ? direct : attacker;
        Vec3 from = origin.position();

        Vec3 oldPos = player.position();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, oldPos.x, oldPos.y + 1.0, oldPos.z, 25, 0.3, 0.6, 0.3, 0.1);
        boolean moved = teleportAway(player, level, from);
        Vec3 newPos = player.position();
        if (moved) {
            level.sendParticles(ParticleTypes.CLOUD, newPos.x, newPos.y + 1.0, newPos.z, 12, 0.3, 0.6, 0.3, 0.02);
        }
        play(level, newPos, EyeOfOdinMod.DODGE_SOUND);

        s.stacks--;
        if (s.stacks <= 0) {
            s.stacks = 0;
            s.overheated = true;
            s.overheatUntilTick = now + OVERHEAT_TICKS;
            play(level, newPos, EyeOfOdinMod.OVERHEATED_SOUND);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, newPos.x, newPos.y + 1.7, newPos.z, 20, 0.2, 0.2, 0.2, 0.02);
            player.sendSystemMessage(Component.literal("Eye of Precognition - Overheat"), true);
        } else {
            player.sendSystemMessage(Component.literal("Precognition  [" + s.stacks + "]"), true);
        }
        return false; // damage cancelled
    }

    /** Make the player's outgoing melee hits critical while the eye is not overheated. */
    public static float modifyOutgoingDamage(LivingEntity victim, ServerLevel level, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return amount;
        if (source.getDirectEntity() != attacker) return amount;       // melee only
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return amount;
        if (victim == attacker) return amount;

        EyeState s = state(attacker);
        if (s.overheated || !TrinketsBridge.isEquipped(attacker)) return amount;

        double y = victim.getY() + victim.getBbHeight() * 0.5;
        level.sendParticles(ParticleTypes.CRIT, victim.getX(), y, victim.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
        level.playSound((Player) null, victim.getX(), y, victim.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        return amount * CRIT_MULTIPLIER;
    }

    private static void play(ServerLevel level, Vec3 pos, SoundEvent sound) {
        level.playSound((Player) null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /** Try to teleport to the side / back, away from the attacker. Returns true if the player moved. */
    private static boolean teleportAway(ServerPlayer player, ServerLevel level, Vec3 from) {
        Vec3 pos = player.position();
        Vec3 away = new Vec3(pos.x - from.x, 0, pos.z - from.z);
        if (away.lengthSqr() < 1.0E-4) {
            Vec3 look = player.getLookAngle();
            away = new Vec3(-look.x, 0, -look.z);
        }
        if (away.lengthSqr() < 1.0E-4) away = new Vec3(0, 0, 1);
        away = away.normalize();
        Vec3 side = new Vec3(-away.z, 0, away.x);

        double d = DODGE_DISTANCE;
        double sign = level.getRandom().nextBoolean() ? 1 : -1;
        Vec3[] options = {
                side.scale(d * sign),
                side.scale(-d * sign),
                side.scale((d + 1) * sign).add(away),
                side.scale(-(d + 1) * sign).add(away),
                away.scale(d),
                side.scale(2 * sign),
                side.scale(-2 * sign)
        };

        for (Vec3 offset : options) {
            Vec3 dest = findStandable(player, level, pos.add(offset));
            if (dest != null) {
                player.teleportTo(level, dest.x, dest.y, dest.z, Set.of(), player.getYRot(), player.getXRot(), true);
                player.setDeltaMovement(Vec3.ZERO);
                return true;
            }
        }
        return false;
    }

    private static Vec3 findStandable(ServerPlayer player, ServerLevel level, Vec3 target) {
        AABB flat = player.getBoundingBox().move(target.x - player.getX(), 0, target.z - player.getZ());
        for (int dy : new int[]{0, 1, -1, -2}) {
            AABB box = flat.move(0, dy, 0);
            boolean free = level.noCollision(player, box);
            boolean hasFloor = !level.noCollision(player, box.move(0, -0.5, 0));
            if (free && hasFloor) {
                BlockPos feet = BlockPos.containing(target.x, player.getY() + dy, target.z);
                if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.below()).isEmpty()) continue;
                return new Vec3(target.x, player.getY() + dy, target.z);
            }
        }
        return null;
    }
}
