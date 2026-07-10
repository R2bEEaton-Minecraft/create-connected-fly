package com.hlysine.create_connected.registries;

import com.hlysine.create_connected.CreateConnected;
import com.hlysine.create_connected.config.FeatureCategory;
import com.hlysine.create_connected.config.FeatureToggle;
import com.hlysine.create_connected.content.brassgearbox.VerticalBrassGearboxItem;
import com.hlysine.create_connected.content.copycat.board.CopycatBoxItem;
import com.hlysine.create_connected.content.copycat.board.CopycatCatwalkItem;
import com.hlysine.create_connected.content.kineticbattery.ChargedKineticBatteryItem;
import com.hlysine.create_connected.content.linkedtransmitter.LinkedTransmitterItem;
import com.hlysine.create_connected.content.parallelgearbox.VerticalParallelGearboxItem;
import com.hlysine.create_connected.content.redstonelinkwildcard.RedstoneLinkWildcardItem;
import com.hlysine.create_connected.content.sixwaygearbox.VerticalSixWayGearboxItem;
import com.zurrtum.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

// Original was Registrate's REGISTRATE.item(id, factory).transform(...).register(). Registrate is
// gone (see PORTING_NOTES.md); converted to direct registration via CCRegistrate. Music disc tags
// (Tags.Items.MUSIC_DISCS/CREEPER_DROP_MUSIC_DISCS) were pure datagen and are already static JSON.
public class CCItems {
    public static final Item CONTROL_CHIP = CCRegistrate.item("control_chip", Item::new);

    public static final SequencedAssemblyItem INCOMPLETE_CONTROL_CHIP = CCRegistrate.item("incomplete_control_chip", SequencedAssemblyItem::new);

    public static final RedstoneLinkWildcardItem REDSTONE_LINK_WILDCARD = CCRegistrate.item("redstone_link_wildcard", RedstoneLinkWildcardItem::new);
    static {
        FeatureToggle.register(id("redstone_link_wildcard"), FeatureCategory.REDSTONE);
    }

    public static final VerticalParallelGearboxItem VERTICAL_PARALLEL_GEARBOX =
            CCRegistrate.item("vertical_parallel_gearbox", VerticalParallelGearboxItem::new);
    static {
        FeatureToggle.registerDependent(id("vertical_parallel_gearbox"), id("parallel_gearbox"));
    }

    public static final VerticalSixWayGearboxItem VERTICAL_SIX_WAY_GEARBOX =
            CCRegistrate.item("vertical_six_way_gearbox", VerticalSixWayGearboxItem::new);
    static {
        FeatureToggle.registerDependent(id("vertical_six_way_gearbox"), id("six_way_gearbox"));
    }

    public static final VerticalBrassGearboxItem VERTICAL_BRASS_GEARBOX =
            CCRegistrate.item("vertical_brass_gearbox", VerticalBrassGearboxItem::new);
    static {
        FeatureToggle.registerDependent(id("vertical_brass_gearbox"), id("brass_gearbox"));
    }

    @SuppressWarnings("removal")
    @Deprecated(forRemoval = true, since = "1.3.0")
    public static final ChargedKineticBatteryItem CHARGED_KINETIC_BATTERY =
            CCRegistrate.item("charged_kinetic_battery", ChargedKineticBatteryItem::new);
    static {
        FeatureToggle.registerDependent(id("charged_kinetic_battery"), id("kinetic_battery"));
    }

    public static final LinkedTransmitterItem LINKED_TRANSMITTER = CCRegistrate.item("linked_transmitter", LinkedTransmitterItem::new);
    static {
        FeatureToggle.register(id("linked_transmitter"), FeatureCategory.REDSTONE);
    }

    public static final CopycatBoxItem COPYCAT_BOX = CCRegistrate.item("copycat_box", CopycatBoxItem::new);
    static {
        FeatureToggle.registerDependent(id("copycat_box"), id("copycat_board"));
    }

    public static final CopycatCatwalkItem COPYCAT_CATWALK = CCRegistrate.item("copycat_catwalk", CopycatCatwalkItem::new);
    static {
        FeatureToggle.registerDependent(id("copycat_catwalk"), id("copycat_board"));
    }

    public static final Item MUSIC_DISC_ELEVATOR = CCRegistrate.item("music_disc_elevator", Item::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(CCJukeboxSongs.ELEVATOR));

    public static final Item MUSIC_DISC_INTERLUDE = CCRegistrate.item("music_disc_interlude", Item::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(CCJukeboxSongs.INTERLUDE));

    public static void register() {
    }

    private static Identifier id(String path) {
        return CreateConnected.asResource(path);
    }
}
