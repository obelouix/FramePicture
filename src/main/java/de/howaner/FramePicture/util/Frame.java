package de.howaner.FramePicture.util;

import java.awt.Image;

import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import de.howaner.FramePicture.FramePicturePlugin;
import de.howaner.FramePicture.event.ChangeFrameIdEvent;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

public class Frame {
	
	private String path;
	private Short mapId;
	
	public Frame(String path, Short mapId) {
		this.path = path;
		this.mapId = mapId;
	}
	
	public Short getMapId() {
		return this.mapId;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public void setPath(String path) {
		this.path = path;
		this.update();
	}
	
	public Image getPicture() {
		try {
			Image image = Utils.getPicture(this.getPath());
			if (image == null) return null;
			if (Config.CHANGE_SIZE_ENABLED)
				image = image.getScaledInstance(Config.SIZE_WIDTH, Config.SIZE_HEIGHT, Image.SCALE_DEFAULT);
			return image;
		} catch (Exception e) {
			return null;
		}
	}
	
	public void update() {
		MapView view = Bukkit.getMap(this.mapId);
		if (view == null) {
			short newId = Utils.createMapId();
			
			ChangeFrameIdEvent event = new ChangeFrameIdEvent(this, this.mapId, newId);
			Bukkit.getPluginManager().callEvent(event);
			if (event.getNewId() != newId) newId = event.getNewId();
			
			for (ItemFrame frame : Utils.getFrameEntitys(this.mapId)) {
				ItemStack item = frame.getItem();
				item.setDurability(newId);
				frame.setItem(item);
			}
			FramePicturePlugin.log.info("Frame #" + this.mapId + " has a new Id: #" + newId);
			this.mapId = newId;
			view = Bukkit.getMap(newId);
		}
		
		//Renderer
		for (MapRenderer render : view.getRenderers())
			view.removeRenderer(render);
		
		final MapView view2 = view;
		new Thread() {
			@Override
			public void run() {
				Image image = Frame.this.getPicture();
				if (image == null) {
					FramePicturePlugin.log.warning("The Url \"" + Frame.this.getPath() + "\" from Frame #" + Frame.this.getMapId().toString() + " does not exists!");
					view2.addRenderer(new TextRenderer("Can't open Image!", Frame.this.getMapId()));
					return;
				}
				
				MapRenderer renderer = new Renderer(image);
				view2.addRenderer(renderer);
				
				FramePicturePlugin.getManager().sendMap(Frame.this);
			}
		}.start();
	}

}
