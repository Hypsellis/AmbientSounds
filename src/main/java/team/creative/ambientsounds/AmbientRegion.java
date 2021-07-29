package team.creative.ambientsounds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class AmbientRegion extends AmbientCondition {
    
    public String name;
    public transient double volumeSetting = 1;
    protected transient boolean active;
    public transient LinkedHashMap<String, AmbientSound> sounds = new LinkedHashMap<>();
    
    transient List<AmbientSound> playing = new ArrayList<>();
    
    public transient AmbientDimension dimension;
    
    public AmbientRegion() {
        
    }
    
    public void load(Gson gson, JsonParser parser, ResourceManager manager) throws IOException {
        for (Resource resource : manager.getResources(new ResourceLocation(AmbientSounds.MODID, "regions/" + (dimension != null ? dimension.name + "." : "") + name + ".json"))) {
            AmbientSound[] sounds = gson.fromJson(parser.parse(IOUtils.toString(resource.getInputStream(), Charsets.UTF_8)).getAsJsonObject(), AmbientSound[].class);
            for (int j = 0; j < sounds.length; j++) {
                AmbientSound sound = sounds[j];
                this.sounds.put(sound.name, sound);
            }
        }
    }
    
    @Override
    public String regionName() {
        return name;
    }
    
    @Override
    public void init(AmbientEngine engine) {
        super.init(engine);
        
        if (sounds != null)
            for (AmbientSound sound : sounds.values())
                sound.init(engine);
    }
    
    @Override
    public AmbientSelection value(AmbientEnviroment env) {
        if (dimension != null && !dimension.is(env.level))
            return null;
        if (volumeSetting == 0)
            return null;
        AmbientSelection selection = super.value(env);
        if (selection != null)
            selection.volume *= volumeSetting;
        return selection;
    }
    
    public boolean fastTick(AmbientEnviroment env) {
        if (!playing.isEmpty()) {
            for (Iterator<AmbientSound> iterator = playing.iterator(); iterator.hasNext();) {
                AmbientSound sound = iterator.next();
                if (!sound.fastTick(env)) {
                    sound.deactivate();
                    iterator.remove();
                }
            }
        }
        
        return !playing.isEmpty();
    }
    
    public boolean tick(AmbientEnviroment env) {
        
        if (sounds == null)
            return false;
        
        AmbientSelection selection = value(env);
        for (AmbientSound sound : sounds.values()) {
            if (sound.tick(env, selection)) {
                if (!sound.isActive()) {
                    sound.activate();
                    playing.add(sound);
                }
            } else if (sound.isActive()) {
                sound.deactivate();
                playing.remove(sound);
            }
        }
        
        return !playing.isEmpty();
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void activate() {
        active = true;
    }
    
    public void deactivate() {
        active = false;
        
        if (!playing.isEmpty()) {
            for (AmbientSound sound : playing)
                sound.deactivate();
            playing.clear();
        }
    }
    
    @Override
    public String toString() {
        return name + ", playing: " + playing.size();
    }
    
}
