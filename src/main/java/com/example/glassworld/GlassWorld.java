package com.example.glassworld;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(GlassWorld.MODID)
public class GlassWorld {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "glassworld";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // Create a Deferred Register to hold Items which will all be registered under the "glassworld" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    /**
     * Tool material used by the glass sword.
     * <p>
     * Record components, in order: (blocks it cannot mine correctly, durability, mining speed,
     * attack damage bonus, enchantment value, repair items).
     * <p>
     * Durability is 3 and the attack damage bonus is 50, everything else is kept identical to wood.
     */
    public static final ToolMaterial GLASS_TOOL_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_WOODEN_TOOL, // a glass sword is not a mining tool anyway
            3, // durability: 3 uses
            2.0F, // mining speed (unused for swords)
            50.0F, // attack damage bonus: +50 damage
            15, // enchantment value
            ItemTags.WOODEN_TOOL_MATERIALS // default repair tag, overridden below with glass
    );

    /**
     * The glass sword: same behaviour as any other sword, but with 3 durability and +50 attack damage.
     * {@code sword(material, attackDamageBaseline, attackSpeedBaseline)} sets up the tool component,
     * the attack damage/speed attribute modifiers, the weapon component and the durability in one call.
     */
    public static final DeferredItem<Item> GLASS_SWORD = ITEMS.registerItem("glass_sword",
            properties -> new Item(properties
                    .sword(GLASS_TOOL_MATERIAL, 0.0F, -2.4F) // 0 + 50 bonus = +50 attack damage, vanilla sword speed
                    .repairable(Items.GLASS))); // a cracked glass sword is repaired with glass

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public GlassWorld(IEventBus modEventBus, ModContainer modContainer) {
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);

        // Register the glass sword into the vanilla Combat creative tab, next to the other swords
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);
    }

    // Add the glass sword to the Combat tab, where the vanilla swords live
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(GLASS_SWORD);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Glass World loaded");
    }
}
