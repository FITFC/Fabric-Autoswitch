package autoswitch.compat.autoswitch_api.impl;

import autoswitch.AutoSwitch;
import autoswitch.config.AutoSwitchAttackActionConfig;
import autoswitch.config.AutoSwitchUseActionConfig;
import autoswitch.config.util.ConfigReflection;
import autoswitch.util.SwitchData;
import autoswitch.util.SwitchUtil;

import org.apache.commons.lang3.tuple.Pair;

import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;

import net.minecraft.block.Material;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;

public class ApiMapGenerator {
    public static void createApiMaps() {

        // Damage Systems
        //This is solely for testing if this system works. Do not use as it matches all items.
        //AutoSwitch.data.damageMap.put(Item.class, ItemStack::getDamage);

        // Tool Groups
        AutoSwitch.switchData.toolGroupings.put("pickaxe", Pair.of(FabricToolTags.PICKAXES, PickaxeItem.class));
        AutoSwitch.switchData.toolGroupings.put("shovel", Pair.of(FabricToolTags.SHOVELS, ShovelItem.class));
        AutoSwitch.switchData.toolGroupings.put("hoe", Pair.of(FabricToolTags.HOES, HoeItem.class));
        AutoSwitch.switchData.toolGroupings.put("shears", Pair.of(FabricToolTags.SHEARS, ShearsItem.class));
        AutoSwitch.switchData.toolGroupings.put("trident", Pair.of(null, TridentItem.class));
        AutoSwitch.switchData.toolGroupings.put("axe", Pair.of(FabricToolTags.AXES, AxeItem.class));
        AutoSwitch.switchData.toolGroupings.put("sword", Pair.of(FabricToolTags.SWORDS, SwordItem.class));

        // Targets
        genTargetMap();
        genConfigMaps();
    }

    // Populate Targets map with default values
    private static void genTargetMap() {
        AutoSwitch.switchData.targets.put("solid_organic", Material.SOLID_ORGANIC);

        AutoSwitch.switchData.targets.put("repair_station", Material.REPAIR_STATION);

        AutoSwitch.switchData.targets.put("bamboo", Material.BAMBOO);

        AutoSwitch.switchData.targets.put("bamboo_sapling", Material.BAMBOO_SAPLING);

        AutoSwitch.switchData.targets.put("cactus", Material.CACTUS);

        AutoSwitch.switchData.targets.put("cake", Material.CAKE);

        AutoSwitch.switchData.targets.put("carpet", Material.CARPET);

        AutoSwitch.switchData.targets.put("organic_product", Material.ORGANIC_PRODUCT);

        AutoSwitch.switchData.targets.put("cobweb", Material.COBWEB);

        AutoSwitch.switchData.targets.put("soil", Material.SOIL);

        AutoSwitch.switchData.targets.put("egg", Material.EGG);

        AutoSwitch.switchData.targets.put("glass", Material.GLASS);

        AutoSwitch.switchData.targets.put("ice", Material.ICE);

        AutoSwitch.switchData.targets.put("leaves", Material.LEAVES);

        AutoSwitch.switchData.targets.put("metal", Material.METAL);

        AutoSwitch.switchData.targets.put("dense_ice", Material.DENSE_ICE);

        AutoSwitch.switchData.targets.put("sub_block", Material.DECORATION);

        AutoSwitch.switchData.targets.put("supported", Material.DECORATION);

        AutoSwitch.switchData.targets.put("piston", Material.PISTON);

        AutoSwitch.switchData.targets.put("plant", Material.PLANT);

        AutoSwitch.switchData.targets.put("gourd", Material.GOURD);

        AutoSwitch.switchData.targets.put("redstone_lamp", Material.REDSTONE_LAMP);

        AutoSwitch.switchData.targets.put("replaceable_plant", Material.REPLACEABLE_PLANT);

        AutoSwitch.switchData.targets.put("aggregate", Material.AGGREGATE);

        AutoSwitch.switchData.targets.put("replaceable_underwater_plant", Material.REPLACEABLE_UNDERWATER_PLANT);

        AutoSwitch.switchData.targets.put("shulker_box", Material.SHULKER_BOX);

        AutoSwitch.switchData.targets.put("snow_layer", Material.SNOW_LAYER);

        AutoSwitch.switchData.targets.put("snow_block", Material.SNOW_BLOCK);

        AutoSwitch.switchData.targets.put("sponge", Material.SPONGE);

        AutoSwitch.switchData.targets.put("nether_wood", Material.NETHER_WOOD);

        AutoSwitch.switchData.targets.put("stone", Material.STONE);

        AutoSwitch.switchData.targets.put("tnt", Material.TNT);

        AutoSwitch.switchData.targets.put("nether_shoots", Material.NETHER_SHOOTS);

        AutoSwitch.switchData.targets.put("underwater_plant", Material.UNDERWATER_PLANT);

        AutoSwitch.switchData.targets.put("unused_plant", Material.UNUSED_PLANT);

        AutoSwitch.switchData.targets.put("wood", Material.WOOD);

        AutoSwitch.switchData.targets.put("wool", Material.WOOL);

        AutoSwitch.switchData.targets.put("water", Material.WATER);

        AutoSwitch.switchData.targets.put("fire", Material.FIRE);

        AutoSwitch.switchData.targets.put("lava", Material.LAVA);

        AutoSwitch.switchData.targets.put("barrier", Material.BARRIER);

        AutoSwitch.switchData.targets.put("bubble_column", Material.BUBBLE_COLUMN);

        AutoSwitch.switchData.targets.put("air", Material.AIR);

        AutoSwitch.switchData.targets.put("portal", Material.PORTAL);

        addTarget("structure_void", Material.STRUCTURE_VOID);

        if (isAcceptableVersion("1.17-alpha.20.49.a")) {
            addTarget("amethyst", Material.AMETHYST);

            addTarget("passable_snow_block", Material.POWDER_SNOW);

            addTarget("sculk", Material.SCULK);
        }

        // Entities
        AutoSwitch.switchData.targets.put("aquaticEntity", EntityGroup.AQUATIC);

        AutoSwitch.switchData.targets.put("arthropod", EntityGroup.ARTHROPOD);

        AutoSwitch.switchData.targets.put("defaultEntity", EntityGroup.DEFAULT);

        AutoSwitch.switchData.targets.put("illager", EntityGroup.ILLAGER);

        AutoSwitch.switchData.targets.put("undead", EntityGroup.UNDEAD);

        AutoSwitch.switchData.targets.put("boat", EntityType.BOAT);

        // Item Use
        AutoSwitch.switchData.targets.put("bow_action", SwitchData.itemTarget);
    }

    // Populate maps with default values to be sent to mods
    private static void genConfigMaps() {
        ConfigReflection.defaults(AutoSwitch.switchData.attackConfig, AutoSwitchAttackActionConfig.class);
        ConfigReflection.defaults(AutoSwitch.switchData.usableConfig, AutoSwitchUseActionConfig.class);

        // PoC for ensuring empty values don't get passed allowing mods to override
        /*for (String key : matCfg.propertyNames()) {
            if (!matCfg.getProperty(key, "").equals("")) {
                AutoSwitch.data.actionConfig.put(key, matCfg.getProperty(key));
            }
        }

        for (String key : usableCfg.propertyNames()) {
            if (!usableCfg.getProperty(key, "").equals("")) {
                AutoSwitch.data.usableConfig.put(key, usableCfg.getProperty(key));
            }
        }*/

    }

}
