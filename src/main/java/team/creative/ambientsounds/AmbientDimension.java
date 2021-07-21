package team.creative.ambientsounds;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import team.creative.ambientsounds.AmbientEnviroment.TerrainHeight;
import team.creative.creativecore.common.config.api.CreativeConfig;

public class AmbientDimension {
    
    @CreativeConfig.DecimalRange(min = 0, max = 1)
    public transient double volumeSetting = 1;
    public transient HashMap<String, AmbientRegion> regions = new HashMap<>();
    
    public String name;
    
    public boolean mute = false;
    
    @SerializedName("biome-selector")
    public AmbientCondition biomeSelector;
    
    public Boolean night;
    public Boolean rain;
    public Boolean storm;
    
    @SerializedName(value = "dimension-names")
    public String[] dimensionNames;
    
    @SerializedName(value = "bad-dimension-names")
    public String[] badDimensionNames;
    
    @SerializedName(value = "average-height")
    public Integer averageHeight;
    
    public void load(Gson gson, JsonParser parser, IResourceManager manager) throws IOException {
        for (IResource resource : manager.getResources(new ResourceLocation(AmbientSounds.MODID, "dimensions/" + name + ".json"))) {
            AmbientRegion[] regions = gson.fromJson(parser.parse(IOUtils.toString(resource.getInputStream(), Charsets.UTF_8)).getAsJsonObject(), AmbientRegion[].class);
            for (int j = 0; j < regions.length; j++) {
                AmbientRegion region = regions[j];
                region.dimension = this;
                this.regions.put(region.name, region);
                region.load(gson, parser, manager);
            }
        }
    }
    
    public void init(AmbientEngine engine) {
        if (biomeSelector != null)
            biomeSelector.init(engine);
        
        if (badDimensionNames != null)
            for (int i = 0; i < badDimensionNames.length; i++)
                badDimensionNames[i] = ".*" + badDimensionNames[i].toLowerCase().replace("*", ".*").replace("?", "\\?") + ".*";
            
        if (dimensionNames != null)
            for (int i = 0; i < dimensionNames.length; i++)
                dimensionNames[i] = ".*" + dimensionNames[i].toLowerCase().replace("*", ".*").replace("?", "\\?") + ".*";
    }
    
    public boolean is(World world) {
        String dimensionTypeName = world.dimension().location().toString();
        
        if (badDimensionNames != null)
            for (int j = 0; j < badDimensionNames.length; j++)
                if (dimensionTypeName.matches(badDimensionNames[j]))
                    return false;
                
        if (dimensionNames != null)
            for (int j = 0; j < dimensionNames.length; j++)
                if (dimensionTypeName.matches(dimensionNames[j]))
                    return true;
                
        return dimensionNames == null;
    }
    
    public void manipulateEnviroment(AmbientEnviroment env) {
        env.soundsDisabled = mute;
        
        if (night != null)
            env.night = night;
        
        if (rain != null)
            env.raining = rain;
        
        if (storm != null)
            env.thundering = storm;
        
        if (biomeSelector != null) {
            AmbientSelection selection = biomeSelector.value(env);
            if (selection != null)
                env.biomeVolume = selection.getEntireVolume();
            else
                env.biomeVolume = 0;
        }
        
        if (averageHeight != null)
            env.setHeight(new TerrainHeight(averageHeight, averageHeight, averageHeight));
    }
    
    @Override
    public String toString() {
        return name;
    }
    
}
