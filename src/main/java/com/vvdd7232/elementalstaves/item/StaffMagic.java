package com.vvdd7232.elementalstaves.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Instant server-authoritative spells. No terrain edits, projectiles or delayed tasks. */
final class StaffMagic {
    enum Spell {
        FIRE(12, 2), NOVA(50, 6), CHAIN(24, 3), STORM(70, 8),
        QUAKE(30, 3), STONE_SKIN(100, 5), FROST(35, 3),
        GUST(20, 2), DASH(40, 3), HEAL(30, 4), SANCTUARY(80, 8);
        final int cooldown, cost;
        Spell(int cooldown, int cost) { this.cooldown = cooldown; this.cost = cost; }
    }

    private StaffMagic() {}

    // Players, pets (including other players' pets), villagers and teammates are not combat targets.
    private static boolean enemy(ServerPlayer p, LivingEntity e) {
        return e instanceof Mob && e.isAlive() && !e.isInvulnerable() && !e.isAlliedTo(p)
                && !(e instanceof net.minecraft.world.entity.npc.AbstractVillager)
                && !(e instanceof TamableAnimal pet && pet.isTame())
                && !(e instanceof AbstractHorse horse && horse.isTamed());
    }

    private static List<LivingEntity> targets(ServerLevel level, ServerPlayer p, Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> enemy(p, e) && e.distanceToSqr(center) <= radius * radius && p.hasLineOfSight(e))
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).limit(24).toList();
    }

    private static void burst(ServerLevel level, Vec3 at, SimpleParticleType particle) {
        level.sendParticles(particle, at.x, at.y + 0.8, at.z, 18, 0.4, 0.6, 0.4, 0.03);
    }

    private static void beam(ServerLevel level, Vec3 from, Vec3 to, SimpleParticleType particle) {
        int steps = Math.min(64, Math.max(1, (int)(from.distanceTo(to) * 3)));
        for (int i = 0; i <= steps; i++) {
            Vec3 v = from.lerp(to, (double)i / steps);
            level.sendParticles(particle, v.x, v.y, v.z, 1, 0, 0, 0, 0);
        }
    }

    private static LivingEntity aimed(ServerLevel level, ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition();
        Vec3 end = p.pick(range, 1, false).getLocation(); // walls cap entity targeting
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1), e -> enemy(p, e))
                .stream().filter(e -> e.getBoundingBox().inflate(0.35).clip(start, end).isPresent())
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
    }

    private static boolean hit(ServerPlayer p, LivingEntity e, float damage) {
        return e.hurt(p.damageSources().playerAttack(p), damage);
    }

    private static void push(LivingEntity e, Vec3 center, double force) {
        Vec3 delta = e.position().subtract(center);
        // LivingEntity.knockback subtracts its direction vector, hence the minus signs.
        e.knockback(force, -delta.x, -delta.z);
        e.push(0, 0.35, 0);
        e.hurtMarked = true;
    }

    static boolean heal(ServerLevel level, LivingEntity target, float amount) {
        if (!target.isAlive() || target.isSpectator()) return false;
        boolean changed = target.getHealth() < target.getMaxHealth();
        if (changed) target.heal(amount);
        for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                changed |= target.removeEffect(effect.getEffect());
            }
        }
        if (target.isOnFire()) { target.clearFire(); changed = true; }
        if (changed) {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            burst(level, target.position(), ParticleTypes.HEART);
        }
        return changed;
    }

    static boolean cast(ServerLevel level, ServerPlayer p, Spell spell) {
        Vec3 origin = p.position();
        boolean changed = false;
        switch (spell) {
            case FIRE -> {
                // Three instant flame rays. Each creature can be hit once per salvo.
                Set<LivingEntity> struck = new HashSet<>();
                for (float angle : new float[]{-0.12F, 0, 0.12F}) {
                    Vec3 start = p.getEyePosition();
                    Vec3 end = start.add(p.getLookAngle().yRot(angle).scale(22));
                    end = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE, p)).getLocation();
                    beam(level, start, end, ParticleTypes.FLAME);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                            new AABB(start, end).inflate(0.5), e -> enemy(p, e))) {
                        if (e.getBoundingBox().inflate(0.4).clip(start, end).isPresent() && struck.add(e) && hit(p, e, 10))
                            e.igniteForSeconds(5);
                    }
                }
                changed = true; // firing into empty space still spends charge
            }
            case CHAIN, STORM -> {
                LivingEntity first = aimed(level, p, 32);
                if (first == null) return false;
                if (spell == Spell.STORM) {
                    for (LivingEntity e : targets(level, p, first.position(), 7)) {
                        if (hit(p, e, 16)) {
                            burst(level, e.position(), ParticleTypes.ELECTRIC_SPARK);
                            beam(level, e.position().add(0, 8, 0), e.position(), ParticleTypes.ELECTRIC_SPARK);
                            changed = true;
                        }
                    }
                } else {
                    Set<LivingEntity> struck = new HashSet<>();
                    LivingEntity current = first;
                    Vec3 previous = p.getEyePosition();
                    for (int i = 0; i < 5 && current != null; i++) {
                        struck.add(current);
                        beam(level, previous, current.getEyePosition(), ParticleTypes.ELECTRIC_SPARK);
                        changed |= hit(p, current, 12 - i);
                        LivingEntity last = current;
                        previous = current.getEyePosition();
                        current = targets(level, p, current.position(), 6).stream()
                                .filter(e -> !struck.contains(e) && last.hasLineOfSight(e)).findFirst().orElse(null);
                    }
                }
            }
            case NOVA, QUAKE, FROST, GUST -> {
                double radius = spell == Spell.FROST ? 10 : 7;
                for (LivingEntity e : targets(level, p, origin, radius)) {
                    Vec3 direction = e.getEyePosition().subtract(p.getEyePosition()).normalize();
                    if ((spell == Spell.FROST || spell == Spell.GUST) && direction.dot(p.getLookAngle()) < 0.45) continue;
                    float damage = spell == Spell.NOVA ? 14 : spell == Spell.QUAKE ? 12 : spell == Spell.FROST ? 8 : 6;
                    if (!hit(p, e, damage)) continue;
                    changed = true;
                    if (spell == Spell.NOVA) e.igniteForSeconds(6);
                    if (spell == Spell.FROST) {
                        e.clearFire();
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                    }
                    if (spell == Spell.QUAKE) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    if (spell == Spell.GUST) e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160));
                    push(e, origin, spell == Spell.GUST ? 2.4 : 0.8);
                    burst(level, e.position(), spell == Spell.NOVA ? ParticleTypes.FLAME
                            : spell == Spell.FROST ? ParticleTypes.SNOWFLAKE : ParticleTypes.CLOUD);
                }
            }
            case DASH -> {
                if (p.isPassenger() || p.isFallFlying()) return false;
                Vec3 v = p.getLookAngle().scale(1.8);
                p.setDeltaMovement(v.x, Math.max(0.25, Math.min(0.9, v.y)), v.z);
                p.fallDistance = 0;
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200));
                p.connection.send(new ClientboundSetEntityMotionPacket(p));
                burst(level, origin, ParticleTypes.CLOUD);
                changed = true;
            }
            case STONE_SKIN -> {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1));
                burst(level, origin, ParticleTypes.CRIT);
                changed = true;
            }
            case HEAL -> changed = heal(level, p, 12);
            case SANCTUARY -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8),
                        e -> e.distanceToSqr(p) <= 64 && p.hasLineOfSight(e)
                                && (e instanceof Player || e instanceof TamableAnimal pet && pet.isOwnedBy(p)))) {
                    changed |= heal(level, e, 12);
                }
            }
        }
        if (changed) level.playSound(null, p.blockPosition(),
                spell == Spell.CHAIN || spell == Spell.STORM ? SoundEvents.TRIDENT_THUNDER
                : spell == Spell.HEAL || spell == Spell.SANCTUARY ? SoundEvents.AMETHYST_BLOCK_CHIME
                : SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 1.15F);
        return changed;
    }
}
