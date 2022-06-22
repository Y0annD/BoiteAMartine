package fr.yoanndiquelou.boiteamartine;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.AttributedString;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

public class Drawer {

	private static Font mFont;

	static {
		try (InputStream is = ClassLoader.getSystemResourceAsStream("font/VAG Rounded Regular.ttf")) {
			mFont = Font.createFont(Font.TRUETYPE_FONT, is);
		} catch (FontFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws IOException, FontFormatException {
		System.out.println(args[0]);

		Path source = Paths.get(args[0]);
		Drawer drawer = new Drawer();
		drawer.processImage(source, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
	}

	public Drawer() {

	}

	public BufferedImage processImage(Path source, String nom, String description)
			throws IOException, FontFormatException {
		long start = System.currentTimeMillis();

		BufferedImage originalImage = ImageIO.read(source.toFile());

		System.out.println("Read: " + (System.currentTimeMillis() - start));

		// save an image
		BufferedImage image = process(originalImage, originalImage.getType(), nom, description);
		System.out.println("Process: " + (System.currentTimeMillis() - start));
		return image;
	}

	public void saveImage(BufferedImage image, Path target) throws IOException {
		ImageIO.write(image, "PNG", target.toFile());

	}

	/**
	 * Get font size according to font and write domain.
	 * 
	 * @param text     text to write
	 * @param graphics graphics object
	 * @param font     font to use
	 * @param width    domain width
	 * @param height   domain height
	 * @return best size for text
	 */
	private float getFontSizeForSize(String text, Graphics graphics, Font font, double width, double height) {
		float size = 1f;
		FontMetrics metrics = graphics.getFontMetrics(font.deriveFont(size));
		int stringHeight = metrics.getAscent() - metrics.getDescent();
		while (metrics.stringWidth(text) < width && stringHeight < height) {
			size += 1f;
			metrics = graphics.getFontMetrics(font.deriveFont(size));
			stringHeight = metrics.getAscent() - metrics.getDescent();
			if (metrics.stringWidth(text) >= width) {
				System.out.println("width");
			}
			if (stringHeight >= height) {
				System.out.println("height");
			}
		}
		return size - 1f;
	}

	private BufferedImage process(BufferedImage old, int type, String nom, String description)
			throws FontFormatException, IOException {
		double widthOffsetRatio = 0.02;
		double nameRatio = 0.5;
		double descriptionRatio = 1 - nameRatio - 0.1;
		int rectHeight = Double.valueOf(old.getHeight() * 0.30).intValue();

		float sizeLine1 = getFontSizeForSize(nom, old.getGraphics(), mFont, old.getWidth() * 0.9,
				rectHeight * nameRatio);
		float sizeLine2 = getFontSizeForSize(description, old.getGraphics(), mFont, old.getWidth() * 0.8,
				rectHeight * descriptionRatio);
		sizeLine2 = Math.min(sizeLine2, sizeLine1/2f);

		Color color = getColor(old);
		Graphics2D g2d = (Graphics2D) old.getGraphics();
		g2d.fillRect(Double.valueOf(old.getWidth() * widthOffsetRatio).intValue(), 0, old.getWidth(), rectHeight);
		AttributedString attributedText = new AttributedString(nom);
		attributedText.addAttribute(TextAttribute.FONT, mFont.deriveFont(sizeLine1));
		attributedText.addAttribute(TextAttribute.FOREGROUND, color);

		FontMetrics metrics = old.getGraphics().getFontMetrics(mFont.deriveFont(sizeLine1));
		int positionX = (old.getWidth() - metrics.stringWidth(nom)) / 2;
		int positionY = metrics.getAscent() - metrics.getDescent() + (int) (rectHeight * 0.20);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		g2d.drawString(attributedText.getIterator(), positionX, positionY);
		attributedText = new AttributedString(description);
		attributedText.addAttribute(TextAttribute.FOREGROUND, color);
		attributedText.addAttribute(TextAttribute.FONT, mFont.deriveFont(sizeLine2));
		FontMetrics metrics1 = old.getGraphics().getFontMetrics(mFont.deriveFont(sizeLine2));
		int positionX1 = Double.valueOf(old.getWidth() - metrics1.stringWidth(description) - (old.getWidth() * 0.05))
				.intValue();
		int positionY1 = positionY + metrics1.getAscent() - metrics1.getDescent() + (int) (rectHeight * .1);
		g2d.drawString(attributedText.getIterator(), positionX1, positionY1);
		return old;
	}

	/**
	 * Get main color of image.
	 * 
	 * @param image buffered image to process
	 * @return main color
	 */
	public static Color getColor(BufferedImage image) {

		Map<Integer, Integer> colorMap = new HashMap<>();
		int height = image.getHeight();
		int width = image.getWidth();

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int rgb = image.getRGB(i, j);
				if (!isGray(getRGBArr(rgb))) {
					Integer counter = colorMap.get(rgb);
					if (counter == null) {
						counter = 0;
					}

					colorMap.put(rgb, ++counter);
				}
			}
		}

		return getMostCommonColor(colorMap, false);
	}

	private static Color getMostCommonColor(Map<Integer, Integer> map, boolean useAlpha) {
		List<Map.Entry<Integer, Integer>> list = new LinkedList<>(map.entrySet());

		Collections.sort(list, (Map.Entry<Integer, Integer> obj1,
				Map.Entry<Integer, Integer> obj2) -> ((Comparable) obj1.getValue()).compareTo(obj2.getValue()));

		Map.Entry<Integer, Integer> entry = list.get(list.size() - 1);
		int[] rgb = getRGBArr(entry.getKey());
		if (useAlpha) {
			return new Color(rgb[0], rgb[1], rgb[2], rgb[3]);
		} else {
			return new Color(rgb[0], rgb[1], rgb[2], rgb[3]);
		}
	}

	/**
	 * Get Red Gren Blue Alphe from pixel value
	 * 
	 * @param pixel pixel value
	 * @return table { red, green, blue, alpha }
	 */
	private static int[] getRGBArr(int pixel) {
		int alpha = (pixel >> 24) & 0xff;
		int red = (pixel >> 16) & 0xff;
		int green = (pixel >> 8) & 0xff;
		int blue = (pixel) & 0xff;

		return new int[] { red, green, blue, alpha };
	}

	private static boolean isGray(int[] rgbArr) {
		int rgDiff = rgbArr[0] - rgbArr[1];
		int rbDiff = rgbArr[0] - rgbArr[2];
		// Filter out black, white and grays...... (tolerance within 10 pixels)
		int tolerance = 10;
		if (rgDiff > tolerance || rgDiff < -tolerance) {
			if (rbDiff > tolerance || rbDiff < -tolerance) {
				return false;
			}
		}
		return true;
	}
}
