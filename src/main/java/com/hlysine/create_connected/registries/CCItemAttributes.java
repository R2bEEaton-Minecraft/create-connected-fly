package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.content.attributefilter.ItemDamageAttribute;
import com.hlysine.create_connected.content.attributefilter.ItemIdAttribute;
import com.hlysine.create_connected.content.attributefilter.ItemStackCountAttribute;
import com.zurrtum.create.api.registry.CreateRegistries;
import com.zurrtum.create.api.registry.CreateRegistryKeys;
import com.zurrtum.create.content.logistics.item.filter.attribute.ItemAttributeType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class CCItemAttributes {

    public static final ItemAttributeType MAX_DAMAGE = register("max_damage", new ItemDamageAttribute.Type());
    public static final ItemAttributeType ID_CONTAINS = register("id_contains", new ItemIdAttribute.Type());
    public static final ItemAttributeType STACK_SIZE = register("stack_size", new ItemStackCountAttribute.Type());

    public static void register() {
    }

    private static ItemAttributeType register(String id, ItemAttributeType type) {
        return Registry.register(CreateRegistries.ITEM_ATTRIBUTE_TYPE,
                ResourceKey.create(CreateRegistryKeys.ITEM_ATTRIBUTE_TYPE, CreateConnected.asResource(id)), type);
    }
}
