package autoswitch.mixin_impl;

import autoswitch.AutoSwitch;
import autoswitch.events.SwitchEvent;
import autoswitch.util.EventUtil;
import autoswitch.util.SwitchData;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;


/**
 * Implementation of the logic for the MinecraftClient mixin
 */
public class SwitchEventTriggerImpl {

    /**
     * Logic for handling ATTACK type actions.
     *
     * @param attackCooldown  the attack cooldown
     * @param player          the player
     * @param world           the world
     * @param crosshairTarget the crosshair target
     */
    public static void attack(int attackCooldown, ClientPlayerEntity player, ClientWorld world, HitResult crosshairTarget) {
        // Duplicate conditions from MinecraftClient#doAttack that prevent reaching the switch for attack logic
        if (attackCooldown > 0 || player.isRiding() || crosshairTarget == null) return;

        triggerSwitch(DesiredType.ACTION, crosshairTarget, world, player);

    }

    /**
     * Logic for handling USE actions.
     *
     * @param interactionManager the interaction manager
     * @param player             the player
     * @param world              the world
     * @param crosshairTarget    the crosshair target
     */
    public static void interact(ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player, ClientWorld world, HitResult crosshairTarget) {
        // Duplicate conditions from MinecraftClient#doItemUse that prevent reaching the switch for use logic
        if (interactionManager.isBreakingBlock() || player.isRiding() || crosshairTarget == null) return;

        triggerSwitch(DesiredType.USE, crosshairTarget, world, player);

    }

    private static void triggerSwitch(DesiredType desiredType, HitResult crosshairTarget, ClientWorld world, ClientPlayerEntity player) {
        SwitchEvent event;
        boolean doSwitchType;

        // Set event and doSwitchType
        switch (desiredType) {
            case USE:
                event = SwitchEvent.USE;
                doSwitchType = AutoSwitch.featureCfg.switchUseActions();
                break;
            case ACTION:
                event = SwitchEvent.ATTACK;
                doSwitchType = crosshairTarget.getType() == HitResult.Type.ENTITY ?
                        AutoSwitch.featureCfg.switchForMobs() : AutoSwitch.featureCfg.switchForBlocks();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + desiredType);
        }

        // Trigger switch
        switch (crosshairTarget.getType()) {
            case MISS:
                if (desiredType != DesiredType.USE) break;
                if (AutoSwitch.useActionCfg.bow_action().length == 0) return; // guard to help prevent lag when rclicking into empty space
                EventUtil.scheduleEvent(event, AutoSwitch.doAS, world, player, doSwitchType, SwitchData.itemTarget);
                break;
            case ENTITY:
                EntityHitResult entityHitResult = (EntityHitResult) crosshairTarget;
                Entity entity = entityHitResult.getEntity();
                EventUtil.scheduleEvent(event, AutoSwitch.doAS, world, player, doSwitchType, entity);
                break;
            case BLOCK:
                BlockHitResult blockHitResult = ((BlockHitResult) crosshairTarget);
                BlockPos blockPos = blockHitResult.getBlockPos();
                BlockState blockState = world.getBlockState(blockPos);
                if (blockState.isAir()) break;
                EventUtil.scheduleEvent(event, AutoSwitch.doAS, world, player, doSwitchType, blockState);
                break;
        }

        // Run scheduler here as well as in the clock to ensure immediate-eval switches occur
        AutoSwitch.scheduler.execute(AutoSwitch.tickTime);

    }

    // Dummy type to allow unification of ATTACK and USE impl
    enum DesiredType{
        USE,
        ACTION
    }

}