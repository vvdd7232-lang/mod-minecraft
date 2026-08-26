package com.vvdd7232.elementalstaves.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A reusable fire charge with a deliberately short cooldown. */
public final class FireStaffItem extends BaseStaffItem {
    private static final int COOLDOWN_TICKS = 24;
    private static final int DURABILITY_COST = 1;

    public FireStaffItem() {
        super(384);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        // Projectile entities must only be created by the logical server.
        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        Vec3d direction = user.getRotationVec(1.0F).normalize();
        Vec3d origin = user.getEyePos().add(direction.multiply(0.75D));
        SmallFireballEntity fireball = new SmallFireballEntity(world, user, direction.x, direction.y, direction.z);
        fireball.setPosition(origin.x, origin.y, origin.z);
        fireball.setVelocity(direction.x, direction.y, direction.z, 1.15F, 0.35F);
        world.spawnEntity(fireball);

        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.9F, 0.9F + world.random.nextFloat() * 0.2F);
        ((ServerWorld) world).spawnParticles(ParticleTypes.FLAME, origin.x, origin.y, origin.z, 10, 0.08D, 0.08D, 0.08D, 0.02D);
        completeCast(user, stack, hand, COOLDOWN_TICKS, DURABILITY_COST);
        return TypedActionResult.success(stack, false);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        addDescription(tooltip, "tooltip.elementalstaves.fire_staff.1", "tooltip.elementalstaves.fire_staff.2");
    }
}
