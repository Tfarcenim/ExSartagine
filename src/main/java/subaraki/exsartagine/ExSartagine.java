package subaraki.exsartagine;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import subaraki.exsartagine.block.ExSartagineBlock;
import subaraki.exsartagine.gui.GuiHandler;
import subaraki.exsartagine.item.ExSartagineItems;
import subaraki.exsartagine.recipe.Recipes;
import subaraki.exsartagine.tileentity.*;
import subaraki.exsartagine.tileentity.render.TileEntityRenderFood;
import subaraki.exsartagine.util.ConfigHandler;

import java.util.Arrays;

import static subaraki.exsartagine.util.Reference.*;

@Mod.EventBusSubscriber
@Mod(name = NAME, modid = MODID, version = VERSION)
public class ExSartagine {

    public static ExSartagine instance;

    public ExSartagine() {
        instance = this;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModMetadata modMeta = event.getModMetadata();
        modMeta.authorList = Arrays.asList("Subaraki", "Isaac");
        modMeta.autogenerated = false;
        modMeta.credits = "";
        modMeta.description = "Thermo Efficient Pots and Pans";
        modMeta.url = "https://github.com/ArtixAllMighty/ExSartagine/wiki";
        instance = this;

        ConfigHandler.instance.loadConfig(event.getSuggestedConfigurationFile());

        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());

        new Recipes();

    }


    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        ExSartagineBlock.load(e.getRegistry());

        GameRegistry.registerTileEntity(TileEntityPan.class, "tileentityexsartagine");
        GameRegistry.registerTileEntity(TileEntitySmelter.class, "tileentityexsartaginesmelter");
        GameRegistry.registerTileEntity(TileEntityPot.class, "tileentityexsartaginepot");
        GameRegistry.registerTileEntity(TileEntityRange.class, "tileentityexsartaginerange");
        GameRegistry.registerTileEntity(TileEntityRangeExtension.class, "tileentityexsartaginerangeextension");
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        ExSartagineItems.load(e.getRegistry());
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        ExSartagineItems.registerRenders();

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPan.class, new TileEntityRenderFood());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySmelter.class, new TileEntityRenderFood());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPot.class, new TileEntityRenderFood());
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        Recipes.init();
    }

}
