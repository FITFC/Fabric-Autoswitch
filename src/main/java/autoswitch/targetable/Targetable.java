package autoswitch.targetable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

import autoswitch.AutoSwitch;
import autoswitch.mixin.mixins.PlayerEntityAccessor;
import autoswitch.util.SwitchData;
import autoswitch.util.TargetableUtil;

import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.apache.commons.lang3.tuple.Pair;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Parent class for Targetable type. Used to establish shared functions and parameters that are used for manipulating
 * the player's selected slot.
 */
@Environment(EnvType.CLIENT)
public abstract class Targetable {
    /**
     * Maps a hotbar slot to the rating for tool effectiveness - ie. speed for blocks and/or enchantment level based on
     * user config
     */
    private final Int2DoubleArrayMap slot2ToolRating = new Int2DoubleArrayMap();

    /**
     * The initial Target brought in from the world, eg. a block or entity. This differs from the Target in that a
     * {@link net.minecraft.block.Material} or {@link net.minecraft.entity.EntityGroup} may be targeted in the user
     * config
     * <p>
     * Equals the actual Target iff it is a block or entity override
     */
    Object protoTarget = null;

    PlayerEntity player;

    /**
     * Base constructor for Targetable, initializes the class parameters and fetches the target map and initial tool map
     * based on configs passed to it
     *
     * @param player player this will effect
     */
    protected Targetable(PlayerEntity player) {
        this.player = player;
    }

    /**
     * Switch logic for 'use' action
     *
     * @return returns the correct Targetable subclass to handle the operation
     */
    public static Targetable use(Object protoTarget, PlayerEntity player) {
        return new TargetableUsable(player, protoTarget);
    }

    /**
     * Switch logic for 'switchback' action
     *
     * @return returns the correct Targetable subclass to handle the operation
     */
    public static Targetable switchback(int prevSlot, PlayerEntity player) {
        return new TargetableNone(prevSlot, player);
    }

    /**
     * Switch logic for 'attack' action
     *
     * @return returns the correct Targetable subclass to handle the operation
     */
    public static Targetable attack(Object protoTarget, PlayerEntity player) {
        return new TargetableAttack(protoTarget, player);
    }


    /**
     * Pulls the list of ItemStacks from the player's hotbar and send the stack and slot number to populate the tool
     * map. Sends an air item if the slot is empty
     */
    void populateToolLists() {
        List<ItemStack> hotbar =
                ((PlayerEntityAccessor) this.player).getInventory().main.subList(0, PlayerInventory.getHotbarSize());
        for (int slot = 0; slot < PlayerInventory.getHotbarSize(); slot++) {
            if (TargetableUtil.skipSlot(hotbar.get(slot))) {
                continue;
            }
            populateToolSelection(hotbar.get(slot), slot);

        }

    }

    /**
     * Populate the tool map with the right tools for that type based on subclass
     *
     * @param stack ItemStack to be checked if it is valid
     * @param slot  slot of stack, to be inserted into map if it is valid
     */
    abstract void populateToolSelection(ItemStack stack, int slot);

    /**
     * Change the players selected slot based on the results of {@link Targetable#findSlot}. Checks if there is
     * a slot to change to first.
     *
     * @return If no slot to change to, returns empty. Otherwise returns true if the slot changed, false if it didn't.
     */
    public Optional<Boolean> changeTool() {
        return findSlot().map(slot -> {
            int currentSlot = ((PlayerEntityAccessor) this.player).getInventory().selectedSlot;
            if (slot == currentSlot) {
                // No need to change slot!
                return false;
            }

            ((PlayerEntityAccessor) this.player).getInventory().selectedSlot = slot;
            return true;
        });
    }

    /**
     * Find the optimal tool slot. Return empty if there isn't one.
     *
     * @return Returns empty if autoswitch is not allowed or there is no slot to change to
     */
    Optional<Integer> findSlot() {
        if (!switchAllowed() || this.slot2ToolRating.isEmpty()) {
            return Optional.empty();
        }

        AutoSwitch.logger.debug(String.valueOf(slot2ToolRating));
        if (!this.slot2ToolRating.isEmpty()) {
            int slot = Collections
                    .max(this.slot2ToolRating.int2DoubleEntrySet(), Comparator.comparingDouble(Map.Entry::getValue))
                    .getIntKey();
            if (AutoSwitch.featureCfg.cacheSwitchResults()) {
                TargetableUtil.getTargetableCache(AutoSwitch.switchState, isUse()).put(this.protoTarget, slot);
            }
            return Optional.of(slot);
        }


        return Optional.empty();
    }

    /**
     * @return returns true if the config allows autoswitch to happen; false otherwise. Does not take into account
     * toggle {@link AutoSwitch#doAS}
     */
    private Boolean switchAllowed() {
        return ((!this.player.isCreative() || AutoSwitch.featureCfg.switchInCreative()) &&
                (switchTypeAllowed() && (MinecraftClient.getInstance().isInSingleplayer() ||
                                         AutoSwitch.featureCfg.switchInMP())));
    }

    boolean isUse() {
        return false;
    }

    /**
     * Determine config value for switching for mobs/blocks
     *
     * @return true if that type of switch is allowed in the config
     */
    abstract Boolean switchTypeAllowed();

    /**
     * Add tools to map that can handle this target
     *
     * @param stack           item in hotbar slot to check for usage
     * @param slot            hotbar slot number
     * @param targetGetter    lookup protoTarget in the correct map
     * @param toolSelector    check ToolType for correct case
     * @param toolSelectorMap ToolSelectors relevant to the case
     */
    void processToolSelectors(ItemStack stack, int slot, Object2ObjectOpenHashMap<Object, IntArrayList> toolSelectorMap,
                              TargetGetter targetGetter, ToolSelector toolSelector) {
        if (!switchAllowed()) return; // Short-circuit to not evaluate tools when cannot switch

        // Check cache
        OptionalInt cachedSlot = TargetableUtil.getCachedSlot(protoTarget, AutoSwitch.switchState, isUse());
        if (cachedSlot.isPresent()) {
            this.slot2ToolRating.put(cachedSlot.getAsInt(), 100D);
            return;
        }

        Item item = stack.getItem();

        // Establish base value to add to the tool rating,
        // promoting higher priority tools from the config in the selection
        AtomicReference<Float> counter = new AtomicReference<>((float) PlayerInventory.getHotbarSize() * 10);

        Object target = targetGetter.getTarget(protoTarget);

        if (target == null || checkSpecialCase(target)) return;

        toolSelectorMap.getOrDefault(target, SwitchData.blank).forEach((IntConsumer) id -> {
            if (id == 0) return; // Check if no ID was assigned to the toolSelector.

            counter.updateAndGet(v -> (float) (v - 0.75)); // Tools later in the config list are not preferred
            String tool;
            ReferenceArrayList<Enchantment> enchants;

            if (id != SwitchData.blank.getInt(0)) {
                Pair<String, ReferenceArrayList<Enchantment>> pair = AutoSwitch.switchData.toolSelectors.get(id);
                tool = pair.getLeft();
                enchants = pair.getRight();
            } else { // Handle case of no target but user desires fallback to items
                tool = "blank";
                enchants = null;
            }

            if (toolSelector.correctType(tool, item) && (isUse() || TargetableUtil.isRightTool(stack, protoTarget))) {
                updateToolListsAndRatings(stack, tool, enchants, slot, counter);
            }

        });

    }

    /**
     * Generate the tool rating and add it to the tool rating map.
     */
    // Moves some core logic out of the main processing method to increase clarity
    private void updateToolListsAndRatings(ItemStack stack, String tool, ReferenceArrayList<Enchantment> enchants,
                                           int slot, AtomicReference<Float> counter) {
        double rating = 0;
        boolean stackEnchants = true;

        // Evaluate enchantment
        if (enchants == null) {
            rating += 1; // Promote tool in ranking as it is the correct one
            stackEnchants = false; // Items without the enchant shouldn't stack with ones that do
        } else {
            double enchantRating = 0;
            for (Enchantment enchant : enchants) {
                // Increase enchantRating if enchantment level > 0, otherwise stop processing this tool
                if ((enchantRating += 1.1 * EnchantmentHelper.getLevel(enchant, stack)) <= 0) return;
            }
            rating += enchantRating;
            AutoSwitch.logger.debug("Slot: {}; EnchantRating: {}", slot, enchantRating);
        }

        if (!isUse()) {
            if (AutoSwitch.featureCfg.preferMinimumViableTool() && rating != 0D) {
                rating += -1 * Math.log10(rating); // Reverse and clamp tool
            }
            rating += TargetableUtil.getTargetRating(protoTarget, stack) + counter.get();

            if (!tool.equals("blank") && ((stack.getItem().getMaxDamage() == 0))) { // Fix ignore overrides
                rating = 0.1;
            }
        }

        // Prefer current slot. Has outcome of making undamageable item fallback not switch if it can help it
        if (((PlayerEntityAccessor) this.player).getInventory().selectedSlot == slot) {
            rating += 0.1;
        }
        double finalRating = rating;
        boolean finalStackEnchants = stackEnchants;
        AutoSwitch.logger.debug("Rating: {}; Slot: {}", rating, slot);

        this.slot2ToolRating.computeIfPresent(slot, (iSlot, oldRating) -> TargetableUtil
                .toolRatingChange(oldRating, finalRating, stack, finalStackEnchants));
        this.slot2ToolRating.putIfAbsent(slot, rating);

    }

    boolean checkSpecialCase(Object target) {
        return false;
    }

    @FunctionalInterface
    interface TargetGetter {
        Object getTarget(Object protoTarget);

    }


    @FunctionalInterface
    interface ToolSelector {
        boolean correctType(String tool, Item item);

    }

}

