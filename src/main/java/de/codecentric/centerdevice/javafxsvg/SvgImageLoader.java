package de.codecentric.centerdevice.javafxsvg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.iio.common.ImageLoaderImpl;
import com.sun.javafx.stage.ScreenHelper;
import com.sun.javafx.tk.quantum.QuantumToolkit;

import javafx.stage.Screen;

public class SvgImageLoader extends ImageLoaderImpl {

	private static final int DEFAULT_SIZE = 400;

	private static final int BYTES_PER_PIXEL = 4; // RGBA

	private final InputStream input;

	private float maxPixelScale = 0;

	protected SvgImageLoader(InputStream input) {
		super(SvgDescriptor.getInstance());

		if (input == null) {
			throw new IllegalArgumentException("input == null!");
		}

		this.input = input;
	}

	@Override
	public ImageFrame load(int imageIndex, int width, int height, boolean preserveAspectRatio, boolean smooth)
			throws IOException {
		if (0 != imageIndex) {
			return null;
		}

		int imageWidth = width > 0 ? width : DEFAULT_SIZE;
		int imageHeight = height > 0 ? height : DEFAULT_SIZE;

		try {
			return createImageFrame(imageWidth, imageHeight, getPixelScale());
		} catch (TranscoderException ex) {
			throw new IOException(ex);
		}
	}

	public float getPixelScale() {
		if (maxPixelScale == 0) {
			maxPixelScale = calculateMaxRenderScale();
		}
		return maxPixelScale;
	}

  public float calculateMaxRenderScale() {
    try{
      return ((QuantumToolkit) QuantumToolkit.getToolkit()).getMaxRenderScale();
    } catch(LinkageError e){
      //continue
    }
	  try{
  		float maxRenderScale = 0;
  		for (Screen screen : Screen.getScreens()) {
  			maxRenderScale = Math.max(maxRenderScale, ScreenHelper.getScreenAccessor ().getRenderScale (screen));
  		}
  		return maxRenderScale;
	  }	catch(LinkageError e){
	    //we can't rely on com.sun.javafx.stage.ScreenHelper so we have to manage a failover
	    return 1.0f;
  	}
	}

	private ImageFrame createImageFrame(int width, int height, float pixelScale) throws TranscoderException {
		BufferedImage bufferedImage = getTranscodedImage(width * pixelScale, height * pixelScale);
		ByteBuffer imageData = getImageData(bufferedImage);

		return new FixedPixelDensityImageFrame(ImageStorage.ImageType.RGBA, imageData, bufferedImage.getWidth(),
				bufferedImage.getHeight(), getStride(bufferedImage), null, pixelScale, null);
	}

	private BufferedImage getTranscodedImage(float width, float height) throws TranscoderException {
		BufferedImageTranscoder trans = new BufferedImageTranscoder(BufferedImage.TYPE_INT_ARGB);
		trans.setImageSize(width, height);
		trans.transcode(new TranscoderInput(input), null);

		return trans.getBufferedImage();
	}

	private int getStride(BufferedImage bufferedImage) {
		return bufferedImage.getWidth() * BYTES_PER_PIXEL;
	}

	private ByteBuffer getImageData(BufferedImage bufferedImage) {
		int[] rgb = bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null, 0,
				bufferedImage.getWidth());

		byte[] imageData = new byte[getStride(bufferedImage) * bufferedImage.getHeight()];

		copyColorToBytes(rgb, imageData);
		return ByteBuffer.wrap(imageData);
	}

	private void copyColorToBytes(int[] rgb, byte[] imageData) {
		if (rgb.length * BYTES_PER_PIXEL != imageData.length) {
			throw new ArrayIndexOutOfBoundsException();
		}

		ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);

		for (int i = 0; i < rgb.length; i++) {
			byte[] bytes = byteBuffer.putInt(rgb[i]).array();

			int dataOffset = BYTES_PER_PIXEL * i;
			imageData[dataOffset] = bytes[1];
			imageData[dataOffset + 1] = bytes[2];
			imageData[dataOffset + 2] = bytes[3];
			imageData[dataOffset + 3] = bytes[0];

			byteBuffer.clear();
		}
	}

	@Override
	public void dispose() {
		// Nothing to do
	}
}
