/*
 * Copyright 2021 Marco Cipriani (@marcocipriani01)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.marcocipriani01.telescopetouch.data;

import android.graphics.Color;

/**
 * Converts celestial magnitudes to brightness on a scale of 0 to 1.
 */
public class StarAttributeCalculator {

    // Much above this and the app crashes - best guess we run out of some openGL resource.
    // TODO(jontayler): find out why.
    public static final float MAX_MAGNITUDE = 5.6f;
    private static final int MAX_SIZE = 5;

    private StarAttributeCalculator() {
    }

    public static int getConstellationColor(float magnitude) {
        return getColor(magnitude, Color.CYAN);
    }

    private static int getChannelValue(int baseColor, Channel c, float shade) {
        int value = (baseColor >> c.getOffset()) & 0xFF;
        int newValue = (int) (shade * value);
        return newValue << c.getOffset();
    }

    public static int getColor(float magnitude, int baseColor) {
        if (magnitude > MAX_MAGNITUDE) return Color.BLACK;
        if (magnitude <= 0.0) return baseColor;

        float shade = 1.0f - magnitude / (MAX_MAGNITUDE + 3.0f);

        int result = 0xFF000000;
        for (Channel c : Channel.values()) {
            result += getChannelValue(baseColor, c, shade);
        }
        return result;
    }

    /**
     * Print out the byte associated with the R,G, and B color components in the given Color int.
     */
/*  private static void printBytes(int color) {
    System.out.println(colorToString(color));
  }
/*
  private static String colorToString(final int color) {
    return String.format("a=%03d, r=%03d, g=%03d, b=%03d",
        Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
  }

  public static void main(String[] args) {
    for (float m = 0.0f; m < 6.0f; m += 0.4f) {
      System.out.println("Magnitude: "+m);
      printBytes(getColor(m, Color.WHITE));
      printBytes(getConstellationColor(m));
      System.out.println();
    }
  }
*/
    public static int getSize(float magnitude) {
        return (int) Math.max(MAX_SIZE - magnitude, 1);
    }

    private enum Channel {
        A(24), R(16), G(8), B(0);

        private final int offset;

        Channel(int offset) {
            this.offset = offset;
        }

        public int getOffset() {
            return offset;
        }
    }
}