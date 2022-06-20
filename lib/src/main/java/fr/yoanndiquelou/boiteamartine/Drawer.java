package fr.yoanndiquelou.boiteamartine;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import java.util.UUID;

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
		String format = source.toString().substring(source.toString().lastIndexOf(".") + 1);
		Path target = Paths.get(source.getParent() + File.separator + UUID.randomUUID().toString() + format);

		BufferedImage originalImage = ImageIO.read(source.toFile());
		System.out.println("Read: " + (System.currentTimeMillis()-start));
		// jpg needs BufferedImage.TYPE_INT_RGB
		// png needs BufferedImage.TYPE_INT_ARGB
		int type = -1;
		if ("png".equalsIgnoreCase(format)) {
			type = BufferedImage.TYPE_INT_ARGB;
		} else if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
			type = BufferedImage.TYPE_INT_RGB;
		} else {
			throw new IOException("Extension de fichier incorrect: " + format);
		}

		// save an image
//		ImageIO.write(process(originalImage, type, nom, description), format, target.toFile());
		BufferedImage image =  process(originalImage, type, nom, description);
		System.out.println("Process: " + (System.currentTimeMillis()-start));
		return image;
	}

	private float getFontSizeForWidth(String text, BufferedImage img, Font font, double width) {
		float size = 10f;
		FontMetrics metrics = img.getGraphics().getFontMetrics(font.deriveFont(size));
		while (metrics.stringWidth(text) < width) {
			size += 10;
			metrics = img.getGraphics().getFontMetrics(font.deriveFont(size));
		}
		return size - 10;
	}

	private BufferedImage process(BufferedImage old, int type, String nom, String description)
			throws FontFormatException, IOException {

		Color color = getHexColor(old);

		old.getGraphics().fillRect(Double.valueOf(old.getWidth() * 0.05).intValue(), 0, old.getWidth(),
				Double.valueOf(old.getHeight() / 3.4).intValue());
		float sizeLine1 = getFontSizeForWidth(nom, old, mFont, old.getWidth() * 0.9);
		AttributedString attributedText = new AttributedString(nom);
		attributedText.addAttribute(TextAttribute.FONT, mFont.deriveFont(sizeLine1));
		attributedText.addAttribute(TextAttribute.FOREGROUND, color);

		FontMetrics metrics = old.getGraphics().getFontMetrics(mFont.deriveFont(sizeLine1));
		int positionX = (old.getWidth() - metrics.stringWidth(nom)) / 2;
//		int positionY = metrics.getAscent()-5;
		int positionY = Double.valueOf(metrics.getHeight() / 2. + 50).intValue();
		Graphics2D g2d = (Graphics2D) old.getGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
//		old.getGraphics().drawString(attributedText.getIterator(), positionX, positionY);
		g2d.drawString(attributedText.getIterator(), positionX, positionY);
		float sizeLine2 = getFontSizeForWidth(description, old, mFont, old.getWidth() / 2);
		attributedText = new AttributedString(description);
		attributedText.addAttribute(TextAttribute.FOREGROUND, color);
		attributedText.addAttribute(TextAttribute.FONT, mFont.deriveFont(sizeLine2));
		FontMetrics metrics1 = old.getGraphics().getFontMetrics(mFont.deriveFont(sizeLine2));
		int positionX1 = Double.valueOf(old.getWidth() - metrics1.stringWidth(description) - old.getWidth() / 8.)
				.intValue();
		int positionY1 = Double.valueOf(positionY + metrics1.getHeight() / 2. + metrics1.getAscent() / 2.).intValue();
		g2d.drawString(attributedText.getIterator(), positionX1, positionY1);
		return old;
	}

	public static Color getHexColor(BufferedImage image) {

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

		return getMostCommonColor(colorMap);
	}

	private static Color getMostCommonColor(Map<Integer, Integer> map) {
		List<Map.Entry<Integer, Integer>> list = new LinkedList<>(map.entrySet());

		Collections.sort(list, (Map.Entry<Integer, Integer> obj1,
				Map.Entry<Integer, Integer> obj2) -> ((Comparable) obj1.getValue()).compareTo(obj2.getValue()));

		Map.Entry<Integer, Integer> entry = list.get(list.size() - 1);
		int[] rgb = getRGBArr(entry.getKey());

		return new Color(rgb[0], rgb[1], rgb[2]);
//        return "#" + Integer.toHexString(rgb[0])
//                + Integer.toHexString(rgb[1])
//                + Integer.toHexString(rgb[2]);
	}

	private static int[] getRGBArr(int pixel) {
		int alpha = (pixel >> 24) & 0xff;
		int red = (pixel >> 16) & 0xff;
		int green = (pixel >> 8) & 0xff;
		int blue = (pixel) & 0xff;

		return new int[] { red, green, blue };
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
