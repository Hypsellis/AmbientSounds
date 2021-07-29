package team.creative.ambientsounds;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Strings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.fmlclient.gui.GuiUtils;
import team.creative.ambientsounds.AmbientEnviroment.BiomeArea;
import team.creative.creativecore.CreativeCore;
import team.creative.creativecore.common.config.holder.ConfigHolderDynamic;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.config.sync.ConfigSynchronization;
import team.creative.creativecore.common.util.type.Pair;

public class AmbientTickHandler {
    
    private static Minecraft mc = Minecraft.getInstance();
    
    public AmbientSoundEngine soundEngine;
    public AmbientEnviroment enviroment = null;
    public AmbientEngine engine;
    public int timer = 0;
    
    public boolean showDebugInfo = false;
    
    public void setEngine(AmbientEngine engine) {
        this.engine = engine;
        initConfiguration();
    }
    
    public void initConfiguration() {
        CreativeConfigRegistry.ROOT.removeField(AmbientSounds.MODID);
        ConfigHolderDynamic holder = CreativeConfigRegistry.ROOT.registerFolder(AmbientSounds.MODID, ConfigSynchronization.CLIENT);
        ConfigHolderDynamic sounds = holder.registerFolder("sounds");
        Field soundField = ObfuscationReflectionHelper.findField(AmbientSound.class, "volumeSetting");
        for (Entry<String, AmbientRegion> pair : engine.allRegions.entrySet())
            if (pair.getValue().sounds != null)
                for (AmbientSound sound : pair.getValue().sounds.values())
                    sounds.registerField(pair.getKey() + "." + sound.name, soundField, sound);
                
        ConfigHolderDynamic dimensions = holder.registerFolder("dimensions");
        Field dimensionField = ObfuscationReflectionHelper.findField(AmbientDimension.class, "volumeSetting");
        for (AmbientDimension dimension : engine.dimensions.values())
            dimensions.registerField(dimension.name, dimensionField, dimension);
        
        holder.registerField("silent-dimensions", ObfuscationReflectionHelper.findField(AmbientEngine.class, "silentDimensions"), engine);
        
        CreativeCore.CONFIG_HANDLER.load(AmbientSounds.MODID, Dist.CLIENT);
    }
    
    private static DecimalFormat df = new DecimalFormat("0.##");
    
    private String format(Object value) {
        if (value instanceof Double || value instanceof Float)
            return df.format(value);
        return value.toString();
    }
    
    private String format(List<Pair<String, Object>> details) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Pair<String, Object> pair : details) {
            if (!first)
                builder.append(",");
            else
                first = false;
            builder.append(ChatFormatting.YELLOW + pair.key + ChatFormatting.RESET + ":" + format(pair.value));
        }
        return builder.toString();
    }
    
    @SubscribeEvent
    public void onRender(RenderTickEvent event) {
        if (showDebugInfo && event.phase == Phase.END && engine != null && !mc.isPaused() && enviroment != null && mc.level != null) {
            List<String> list = new ArrayList<>();
            
            AmbientDimension dimension = engine.getDimension(mc.level);
            List<Pair<String, Object>> details = new ArrayList<>();
            details.add(new Pair<>("night", enviroment.night));
            details.add(new Pair<>("rain", enviroment.raining));
            details.add(new Pair<>("worldRain", enviroment.overallRaining));
            details.add(new Pair<>("snow", enviroment.snowing));
            details.add(new Pair<>("storm", enviroment.thundering));
            details.add(new Pair<>("b-volume", enviroment.biomeVolume));
            details.add(new Pair<>("underwater", enviroment.underwater));
            details.add(new Pair<>("dim-name", mc.level.dimension().location()));
            
            list.add(format(details));
            
            details.clear();
            
            for (Entry<BiomeArea, Float> pair : enviroment.biomes.entrySet())
                details.add(new Pair<>(pair.getKey().biome.getBiomeCategory().getName(), pair.getValue()));
            
            list.add(format(details));
            
            details.clear();
            
            details.add(new Pair<>("dimension", dimension));
            details.add(new Pair<>("playing", engine.soundEngine.playingCount()));
            details.add(new Pair<>("light", enviroment.blocks.averageLight));
            details.add(new Pair<>("outside", enviroment.blocks.outsideVolume));
            details.add(new Pair<>("height", df.format(enviroment.relativeHeight) + "," + df.format(enviroment.averageHeight) + "," + df
                    .format(enviroment.player.getEyeY() - enviroment.minHeight) + "," + df.format(enviroment.player.getEyeY() - enviroment.maxHeight)));
            
            list.add(format(details));
            
            details.clear();
            
            for (AmbientRegion region : engine.activeRegions) {
                
                details.add(new Pair<>("region", ChatFormatting.DARK_GREEN + region.name + ChatFormatting.RESET));
                details.add(new Pair<>("playing", region.playing.size()));
                
                list.add(format(details));
                
                details.clear();
                for (AmbientSound sound : region.playing) {
                    
                    if (!sound.isPlaying())
                        continue;
                    
                    String text = "";
                    if (sound.stream1 != null) {
                        details.add(new Pair<>("n", sound.stream1.location));
                        details.add(new Pair<>("v", sound.stream1.volume));
                        details.add(new Pair<>("i", sound.stream1.index));
                        details.add(new Pair<>("p", sound.stream1.pitch));
                        details.add(new Pair<>("t", sound.stream1.ticksPlayed));
                        details.add(new Pair<>("d", sound.stream1.duration));
                        
                        text = "[" + format(details) + "]";
                        
                        details.clear();
                    }
                    
                    if (sound.stream2 != null) {
                        details.add(new Pair<>("n", sound.stream2.location));
                        details.add(new Pair<>("v", sound.stream2.volume));
                        details.add(new Pair<>("i", sound.stream2.index));
                        details.add(new Pair<>("p", sound.stream2.pitch));
                        details.add(new Pair<>("t", sound.stream2.ticksPlayed));
                        details.add(new Pair<>("d", sound.stream2.duration));
                        
                        text += "[" + format(details) + "]";
                        
                        details.clear();
                    }
                    
                    list.add(text);
                }
            }
            
            for (int i = 0; i < list.size(); ++i) {
                String s = list.get(i);
                
                if (!Strings.isNullOrEmpty(s)) {
                    int j = mc.font.lineHeight;
                    int k = mc.font.width(s);
                    int i1 = 2 + j * i;
                    PoseStack mat = new PoseStack();
                    GuiUtils.drawGradientRect(mat.last().pose(), 0, 1, i1 - 1, 2 + k + 1, i1 + j - 1, -1873784752, -1873784752);
                    mc.font.drawShadow(mat, s, 2, i1, 14737632);
                }
            }
        }
    }
    
    @SubscribeEvent
    public void onTick(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            
            if (soundEngine == null) {
                soundEngine = new AmbientSoundEngine(mc.getSoundManager(), mc.options);
                if (engine != null)
                    engine.soundEngine = soundEngine;
            }
            
            if (engine == null)
                setEngine(AmbientEngine.loadAmbientEngine(soundEngine));
            
            if (engine == null)
                return;
            
            Level level = mc.level;
            Player player = mc.player;
            
            if (level != null && player != null && !mc.isPaused() && mc.options.getSoundSourceVolume(SoundSource.AMBIENT) > 0) {
                
                if (enviroment == null)
                    enviroment = new AmbientEnviroment(player);
                
                AmbientDimension newDimension = engine.getDimension(level);
                if (enviroment.dimension != newDimension) {
                    engine.changeDimension(enviroment, newDimension);
                    enviroment.dimension = newDimension;
                }
                
                if (timer % engine.enviromentTickTime == 0) {
                    enviroment.level = level;
                    enviroment.player = player;
                    enviroment.biomeVolume = 1;
                    enviroment.setHeight(engine.calculateAverageHeight(level, player));
                    
                    enviroment.biomeVolume = 1.0F;
                    
                    if (enviroment.dimension != null)
                        enviroment.dimension.manipulateEnviroment(enviroment);
                    
                    if (enviroment.biomeVolume > 0)
                        enviroment.biomes = engine.calculateBiomes(level, player, enviroment.biomeVolume);
                    else if (enviroment.biomes != null)
                        enviroment.biomes.clear();
                    else
                        enviroment.biomes = new LinkedHashMap<>();
                    
                    enviroment.blocks.updateAllDirections(engine);
                }
                
                if (timer % engine.soundTickTime == 0) {
                    enviroment.setSunAngle((float) Math.toDegrees(level.getSunAngle(mc.getDeltaFrameTime())));
                    enviroment.updateLevel();
                    int depth = 0;
                    if (player.isEyeInFluid(FluidTags.WATER)) {
                        BlockPos blockpos = new BlockPos(player.blockPosition()).above();
                        while (level.getBlockState(blockpos).getMaterial() == Material.WATER) {
                            depth++;
                            blockpos = blockpos.above();
                        }
                        depth--;
                    }
                    enviroment.setUnderwater(depth);
                    
                    engine.tick(enviroment);
                }
                
                engine.fastTick(enviroment);
                
                timer++;
            } else if (!engine.activeRegions.isEmpty())
                engine.stopEngine();
        }
    }
}
