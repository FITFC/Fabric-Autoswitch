package autoswitch.events;

import static autoswitch.AutoSwitch.featureCfg;
import static autoswitch.AutoSwitch.tickTime;
import static autoswitch.util.SwitchState.preventBlockAttack;

import java.util.Optional;

import autoswitch.AutoSwitch;
import autoswitch.config.AutoSwitchConfig;
import autoswitch.mixin.mixins.PlayerEntityAccessor;
import autoswitch.targetable.AbstractTargetable;
import autoswitch.util.EventUtil;
import autoswitch.util.SwitchState;
import autoswitch.util.SwitchUtil;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Saddleable;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Processing of switch events.
 */
public enum SwitchEvent {
    /**
     * Event for "attack" action of the player.
     */
    ATTACK {
        @Override
        public boolean handlePreSwitchTasks() {
            return true;
        }

        /**
         * Setup switchstate for switchback feature.
         *
         * @param hasSwitched whether a switch has occurred
         */
        private void handlePostSwitchTasks(boolean hasSwitched) {
            boolean doSwitchBack = featureCfg.switchbackAllowed() == AutoSwitchConfig.TargetType.BOTH;
            // Handles switchback
            if (protoTarget instanceof Entity) {
                if (hasSwitched &&
                    (doSwitchBack || featureCfg.switchbackAllowed() == AutoSwitchConfig.TargetType.MOBS)) {
                    AutoSwitch.switchState.setHasSwitched(true);
                    AutoSwitch.switchState.setAttackedEntity(true);
                }
            } else if (protoTarget instanceof BlockState) {
                if (hasSwitched &&
                    (doSwitchBack || featureCfg.switchbackAllowed() == AutoSwitchConfig.TargetType.BLOCKS)) {
                    AutoSwitch.switchState.setHasSwitched(true);
                }
            }
        }

        @Override
        public boolean invoke() {
            if (canNotSwitch()) return false; // Shortcircuit to make it easier to read

            if (!handlePreSwitchTasks()) return false; // Mowing Control
            handlePrevSlot();

            AbstractTargetable targetable = AbstractTargetable.attack(protoTarget, player);
            targetable.changeTool().ifPresent(this::handlePostSwitchTasks);

            return true;
        }
    },
    /**
     * Event for "interact" or "use" action of the player.
     */
    USE {
        private boolean doOffhand() {
            if (AutoSwitch.featureCfg.putUseActionToolInOffHand() == AutoSwitchConfig.OffhandType.SADDLE) {
                return protoTarget instanceof Saddleable;
            }
            return AutoSwitch.featureCfg.putUseActionToolInOffHand().ordinal() <= 1;
        }

        @Override
        public boolean invoke() {
            if (canNotSwitch()) return false; // Shortcircuit to make it easier to read

            handlePrevSlot();
            Optional<Boolean> temp = AbstractTargetable.use(protoTarget, player).changeTool();
            temp.ifPresent(b -> {
                doOffhandSwitch = doOffhand();
                AutoSwitch.switchState.setHasSwitched(b);
                if (b) AutoSwitch.scheduler.schedule(SwitchEvent.OFFHAND, 0.1, AutoSwitch.tickTime);
            });

            return true;

        }
    },
    /**
     * Event for returning to the previously selected slot once conditions that triggered a switch are over.
     */
    SWITCHBACK {
        @Override
        protected boolean canNotSwitch() {
            // Check if conditions are met for switchback
            if (AutoSwitch.switchState.getHasSwitched() && !player.handSwinging) {
                // Uses -20.0f to give player some leeway when fighting. Use 0 for perfect timing

                return ((doBlockSwitchback() || doMobSwitchback()) && doSwitchback())
                       || SwitchState.preventBlockAttack;
            }

            return true;
        }

        /**
         * @return if the attack cooldown has progressed enough.
         */
        private boolean doSwitchback() {
            return player.getAttackCooldownProgress(-20.0f) != 1.0f;
        }

        private boolean doMobSwitchback() {
            return AutoSwitch.switchState.hasAttackedEntity() &&
                   (featureCfg.switchbackWaits() == AutoSwitchConfig.TargetType.BOTH ||
                    featureCfg.switchbackWaits() == AutoSwitchConfig.TargetType.MOBS);
        }

        private boolean doBlockSwitchback() {
            return !AutoSwitch.switchState.hasAttackedEntity() &&
                   (featureCfg.switchbackWaits() == AutoSwitchConfig.TargetType.BOTH ||
                    featureCfg.switchbackWaits() == AutoSwitchConfig.TargetType.BLOCKS);
        }

        /**
         * Cleanup switchstate after switching back.
         *
         * @param hasSwitched whether a switch has occurred
         */
        private void handlePostSwitchTasks(boolean hasSwitched) {
            if (hasSwitched) {
                AutoSwitch.switchState.setHasSwitched(false);
                AutoSwitch.switchState.setAttackedEntity(false);
            }

        }

        @Override
        public boolean invoke() {
            if (canNotSwitch()) return false; // Shortcircuit to make it easier to read

            AbstractTargetable.switchback(AutoSwitch.switchState.getPrevSlot(), player).changeTool()
                              .ifPresent(this::handlePostSwitchTasks);

            return true;
        }
    },
    /**
     * Event for moving a USE action item to the offhand.
     */
    OFFHAND {
        @Override
        public boolean invoke() {
            SwitchUtil.handleUseSwitchConsumer().accept(doOffhandSwitch);
            doOffhandSwitch = false;
            AutoSwitch.scheduler.schedule(SwitchEvent.SWITCHBACK, 0.1, AutoSwitch.tickTime);

            return true;

        }
    },
    PREVENT_BLOCK_ATTACK {
        @Override
        public boolean invoke() {
            float delay = featureCfg.preventBlockSwitchAfterEntityAttack();
            if (delay == 0) return true;
            SwitchState.preventBlockAttack = AutoSwitch.scheduler.isEventScheduled(PREVENT_BLOCK_ATTACK);

            if (preventBlockAttack) {
                AutoSwitch.scheduler.schedule(SwitchEvent.REMOVE_PREVENTION, delay, AutoSwitch.tickTime);
            }
            return true;
        }
    },
    REMOVE_PREVENTION { // This is ugly. Todo make better
        @Override
        public boolean invoke() {
            AutoSwitch.scheduler.remove(PREVENT_BLOCK_ATTACK);
            SwitchState.preventBlockAttack = false;
            return true;
        }
    };

    public static PlayerEntity player;
    private static Object protoTarget;
    private static boolean doSwitchType;
    private static boolean doSwitch;
    private static boolean doOffhandSwitch;


    /**
     * Try to perform the switch.
     *
     * @return false if the switch could not be performed
     */
    public abstract boolean invoke();

    /**
     * @return whether switching should NOT occur based on current conditions.
     */
    boolean canNotSwitch() {
        // Client is checked to fix LAN worlds (Issue #18)
        return !doSwitch || !doSwitchType;
    }

    /**
     * Store the previously selected slot for use in the SWITCHBACK feature.
     */
    void handlePrevSlot() {
        if (!AutoSwitch.switchState.getHasSwitched()) {
            AutoSwitch.switchState.setPrevSlot(((PlayerEntityAccessor) player).getInventory().selectedSlot);
        }
    }


    /**
     * Run extra checks before performing a switch.
     *
     * @return whether evaluation before switching indicates that a switch should not occur.
     */
    // Formerly used to ensure mowing control did not trigger a switch
    public boolean handlePreSwitchTasks() {
        return true;
    }


    public SwitchEvent setProtoTarget(Object protoTarget) {
        SwitchEvent.protoTarget = protoTarget;
        return this;
    }

    public SwitchEvent setPlayer(PlayerEntity player) {
        SwitchEvent.player = player;
        return this;
    }

    public SwitchEvent setDoSwitchType(boolean doSwitchType) {
        SwitchEvent.doSwitchType = doSwitchType;
        return this;
    }

    public SwitchEvent setDoSwitch(boolean doAS) {
        doSwitch = doAS;
        return this;
    }
}
