package autoswitch.selectors.futures;

import java.util.Collection;
import java.util.function.Predicate;

import autoswitch.util.RegistryHelper;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public record IdentifiedTag<T>(TagKey<T> tagKey, Class<T> clazz, RegistryType type,
                               ObjectOpenCustomHashSet<FutureRegistryEntry> fallbackEntries,
                               Predicate<Object> defaultIsIn) {
    private static final ObjectOpenHashSet<IdentifiedTag<?>> IDENTIFIED_TAGS = new ObjectOpenHashSet<>();

    public IdentifiedTag(TagKey<T> tagKey, Class<T> clazz, RegistryType type, Predicate<Object> defaultIsIn) {
        this(tagKey, clazz, type, new ObjectOpenCustomHashSet<>(new FutureRegistryEntry.TargetHashingStrategy()),
             defaultIsIn);

    }

    public IdentifiedTag {
        IDENTIFIED_TAGS.add(this);
    }

    public static <U> IdentifiedTag<?> getOrCreate(TagKey<U> tagKey, Class<U> clazz, RegistryType type,
                                                   Predicate<Object> defaultIsIn) {
        return IDENTIFIED_TAGS.addOrGet(new IdentifiedTag<>(tagKey, clazz, type, defaultIsIn));
    }

    public static <U> Predicate<U> makeItemPredicate(TagKey<Item> tagKey) {
        return t -> getOrCreate(tagKey, Item.class, RegistryType.ITEM, o -> {
            if (o instanceof Item i) {
                return i.getRegistryEntry().isIn(tagKey);
            }
            return false;
        }).contains(t);
    }

    public static <U> Predicate<U> makeBlockPredicate(TagKey<Block> tagKey) {
        return t -> IdentifiedTag.getOrCreate(tagKey, Block.class, RegistryType.BLOCK, o -> {
            if (o instanceof BlockState state) {
                return state.isIn(tagKey);
            }
            return false;
        }).contains(t);
    }

    public static <U> Predicate<U> makeEntityPredicate(TagKey<EntityType<?>> tagKey) {
        return t -> IdentifiedTag.getOrCreate(tagKey,
                                              (Class<EntityType<?>>)(Class<?>)EntityType.class,
                                              RegistryType.ENTITY, o -> {
                    if (o instanceof Entity e) {
                        return e.getType().isIn(tagKey);
                    }
                    return false;
                }).contains(t);
    }

    public static <U> Predicate<U> makeEnchantmentPredicate(TagKey<Enchantment> tagKey) {
        return t -> getOrCreate(tagKey, Enchantment.class, RegistryType.ENCHANTMENT, o -> {
            if (o instanceof Enchantment e) {
                return RegistryHelper.isInTag(Registry.ENCHANTMENT, tagKey, e);
            }
            return false;
        }).contains(t);
    }

    // To be called only after FREs are refreshed
    //todo test
    public static void refreshIdentifiers() {
        IDENTIFIED_TAGS.trim();
    }

    public void addEntries(Collection<Identifier> ids) {
        for (Identifier id : ids) {
            fallbackEntries.add(FutureRegistryEntry.getOrCreate(type, id));
        }
    }
    //todo need way to remove just polymer entries - another set?

    public boolean contains(Object o) {
        if (clazz.isInstance(o)) {
            if (defaultIsIn.test(o)) {
                return true;
            }
        }

        return fallbackEntries.contains(o);
    }

}
