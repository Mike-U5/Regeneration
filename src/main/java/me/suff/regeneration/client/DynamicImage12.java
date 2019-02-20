package me.suff.regeneration.client;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.resources.IResourceManager;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class DynamicImage12 extends AbstractTexture {
	private final int[] dynamicTextureData;
	/**
	 * width of this icon in pixels
	 */
	private final int width;
	/**
	 * height of this icon in pixels
	 */
	private final int height;
	
	public DynamicImage12(BufferedImage bufferedImage) {
		this(bufferedImage.getWidth(), bufferedImage.getHeight());
		bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), this.dynamicTextureData, 0, bufferedImage.getWidth());
		this.updateDynamicTexture();
	}
	
	public DynamicImage12(int textureWidth, int textureHeight) {
		this.width = textureWidth;
		this.height = textureHeight;
		this.dynamicTextureData = new int[textureWidth * textureHeight];
		TextureUtil.allocateTexture(this.getGlTextureId(), textureWidth, textureHeight);
	}
	
	public void loadTexture(IResourceManager resourceManager) throws IOException {
	}
	
	public void updateDynamicTexture() {
		TextureUtil.allocateTexture(this.getGlTextureId(), this.width, this.height);
	}
	
	public int[] getTextureData() {
		return this.dynamicTextureData;
	}
}