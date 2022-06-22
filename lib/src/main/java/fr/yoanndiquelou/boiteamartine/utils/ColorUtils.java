package fr.yoanndiquelou.boiteamartine.utils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class ColorUtils {

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

	public static Color getMostCommonColor(Map<Integer, Integer> map, boolean useAlpha) {
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
	public static int[] getRGBArr(int pixel) {
		int alpha = (pixel >> 24) & 0xff;
		int red = (pixel >> 16) & 0xff;
		int green = (pixel >> 8) & 0xff;
		int blue = (pixel) & 0xff;

		return new int[] { red, green, blue, alpha };
	}

	public static boolean isGray(int[] rgbArr) {
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
