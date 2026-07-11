package com.hlysine.create_connected.mixin.compat;

import com.hlysine.create_connected.compat.CopycatsManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Create: Copycats+ has no Fabric build for MC 1.21.11 at all (verified via Modrinth - see
// PORTING_NOTES.md), so its own com.copycatsplus.copycats.* classes don't exist on this platform
// and can't be referenced as .class literals (would fail to compile whenever Copycats+ isn't on the
// classpath, which today is unconditionally). @Pseudo + string-targets is Sponge Mixin's own
// mechanism for exactly this case (an optional cross-mod compat mixin targeting a class that may
// not exist at all) - it resolves the target by name at mixin-application time instead of at
// compile time, and silently no-ops if the class isn't present, matching this mod's existing
// "Mods.COPYCATS.isLoaded() is currently always false" reality elsewhere in the Copycats compat
// layer (see compat/CopycatsManager.java). Re-add real .class-literal targets (reverting this) if/
// when Copycats+ ships 1.21.11 Fabric support.
@Pseudo
@Mixin(targets = {
        "com.copycatsplus.copycats.content.copycat.slab.CopycatSlabBlock",
        "com.copycatsplus.copycats.content.copycat.board.CopycatBoardBlock"
})
public class CopycatBlockMixin {
    @Redirect(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),
            method = "canBeReplaced(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/context/BlockPlaceContext;)Z",
            require = 0
    )
    private boolean convertItem(ItemStack instance, Item pItem) {
        if (CopycatsManager.convertIfEnabled(instance.getItem()).equals(pItem))
            return true;
        return instance.is(pItem);
    }
}
