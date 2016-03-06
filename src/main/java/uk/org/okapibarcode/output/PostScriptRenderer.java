/*
 * Copyright 2015 Robin Stuart, Daniel Gredler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.okapibarcode.output;

import static uk.org.okapibarcode.util.Doubles.roughlyEqual;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;

import uk.org.okapibarcode.backend.Hexagon;
import uk.org.okapibarcode.backend.Symbol;
import uk.org.okapibarcode.backend.TextBox;

/**
 * Renders symbologies to EPS (Encapsulated PostScript).
 *
 * @author <a href="mailto:rstuart114@gmail.com">Robin Stuart</a>
 * @author Daniel Gredler
 */
public class PostScriptRenderer implements SymbolRenderer {

    /** The output stream to render to. */
    private final OutputStream out;

    /** The paper (background) color. */
    private final Color paper;

    /** The ink (foreground) color. */
    private final Color ink;

    /**
     * Creates a new PostScript renderer.
     *
     * @param out the output stream to render to
     * @param paper the paper (background) color
     * @param ink the ink (foreground) color
     */
    public PostScriptRenderer(OutputStream out, Color paper, Color ink) {
        this.out = out;
        this.paper = paper;
        this.ink = ink;
    }

    /** {@inheritDoc} */
    @Override
    public void render(Symbol symbol) throws IOException {

        // All y dimensions are reversed because EPS origin (0,0) is at the bottom left, not top left

        double magnification = symbol.getModuleWidth();
        int margin = symbol.getBorderWidth() * (int)magnification;
        
        String content = symbol.getContent();
        int width = (int) (symbol.getWidth() * magnification) + (2 * margin);
        int height = (int) (symbol.getHeight() * magnification) + (2 * margin);

        String title;
        if (content == null || content.isEmpty()) {
            title = "OkapiBarcode Generated Symbol";
        } else {
            title = content;
        }

        try (ExtendedOutputStreamWriter writer = new ExtendedOutputStreamWriter(out, "%.2f")) {

            // Header
            writer.append("%!PS-Adobe-3.0 EPSF-3.0\n");
            writer.append("%%Creator: OkapiBarcode\n");
            writer.append("%%Title: ").append(title).append('\n');
            writer.append("%%Pages: 0\n");
            writer.append("%%BoundingBox: 0 0 ").appendInt(width).append(" ").appendInt(height).append("\n");
            writer.append("%%EndComments\n");

            // Definitions
            writer.append("/TL { setlinewidth moveto lineto stroke } bind def\n");
            writer.append("/TC { moveto 0 360 arc 360 0 arcn fill } bind def\n");
            writer.append("/TH { 0 setlinewidth moveto lineto lineto lineto lineto lineto closepath fill } bind def\n");
            writer.append("/TB { 2 copy } bind def\n");
            writer.append("/TR { newpath 4 1 roll exch moveto 1 index 0 rlineto 0 exch rlineto neg 0 rlineto closepath fill } bind def\n");
            writer.append("/TE { pop pop } bind def\n");

            // Background
            writer.append("newpath\n");
            writer.append(ink.getRed() / 255.0).append(" ")
                  .append(ink.getGreen() / 255.0).append(" ")
                  .append(ink.getBlue() / 255.0).append(" setrgbcolor\n");
            writer.append(paper.getRed() / 255.0).append(" ")
                  .append(paper.getGreen() / 255.0).append(" ")
                  .append(paper.getBlue() / 255.0).append(" setrgbcolor\n");
            writer.append(height).append(" 0.00 TB 0.00 ").append(width).append(" TR\n");

            // Rectangles
            for (int i = 0; i < symbol.rectangles.size(); i++) {
                Rectangle2D.Double rect = symbol.rectangles.get(i);
                if (i == 0) {
                    writer.append("TE\n");
                    writer.append(ink.getRed() / 255.0).append(" ")
                          .append(ink.getGreen() / 255.0).append(" ")
                          .append(ink.getBlue() / 255.0).append(" setrgbcolor\n");
                    writer.append(rect.height * magnification).append(" ")
                          .append(height - ((rect.y + rect.height) * magnification) - margin).append(" TB ")
                          .append((rect.x * magnification) + margin).append(" ")
                          .append(rect.width * magnification).append(" TR\n");
                } else {
                    Rectangle2D.Double prev = symbol.rectangles.get(i - 1);
                    if (!roughlyEqual(rect.height, prev.height) || !roughlyEqual(rect.y, prev.y)) {
                        writer.append("TE\n");
                        writer.append(ink.getRed() / 255.0).append(" ")
                              .append(ink.getGreen() / 255.0).append(" ")
                              .append(ink.getBlue() / 255.0).append(" setrgbcolor\n");
                        writer.append(rect.height * magnification).append(" ")
                              .append(height - ((rect.y + rect.height) * magnification) - margin).append(" ");
                    }
                    writer.append("TB ").append((rect.x * magnification) + margin).append(" ").append(rect.width * magnification).append(" TR\n");
                }
            }

            // Text
            for (int i = 0; i < symbol.texts.size(); i++) {
                TextBox text = symbol.texts.get(i);
                if (i == 0) {
                    writer.append("TE\n");;
                    writer.append(ink.getRed() / 255.0).append(" ")
                          .append(ink.getGreen() / 255.0).append(" ")
                          .append(ink.getBlue() / 255.0).append(" setrgbcolor\n");
                }
                writer.append("matrix currentmatrix\n");
                writer.append("/").append(symbol.getFontName()).append(" findfont\n");
                writer.append(symbol.getFontSize() * magnification).append(" scalefont setfont\n");
                writer.append(" 0 0 moveto ").append((text.x * magnification) + margin).append(" ")
                      .append(height - (text.y * magnification) - margin).append(" translate 0.00 rotate 0 0 moveto\n");
                writer.append(" (").append(text.text).append(") stringwidth\n");
                writer.append("pop\n");
                writer.append("-2 div 0 rmoveto\n");
                writer.append(" (").append(text.text).append(") show\n");
                writer.append("setmatrix\n");
            }

            // Circles
            // Because MaxiCode size is fixed, this ignores magnification
            for (int i = 0; i < symbol.target.size(); i += 2) {
                Ellipse2D.Double ellipse1 = symbol.target.get(i);
                Ellipse2D.Double ellipse2 = symbol.target.get(i + 1);
                if (i == 0) {
                    writer.append("TE\n");
                    writer.append(ink.getRed() / 255.0).append(" ")
                          .append(ink.getGreen() / 255.0).append(" ")
                          .append(ink.getBlue() / 255.0).append(" setrgbcolor\n");
                    writer.append(ink.getRed() / 255.0).append(" ")
                          .append(ink.getGreen() / 255.0).append(" ")
                          .append(ink.getBlue() / 255.0).append(" setrgbcolor\n");
                }
                double x1 = ellipse1.x + (ellipse1.width / 2);
                double x2 = ellipse2.x + (ellipse2.width / 2);
                double y1 = height - ellipse1.y - (ellipse1.width / 2);
                double y2 = height - ellipse2.y - (ellipse2.width / 2);
                double r1 = ellipse1.width / 2;
                double r2 = ellipse2.width / 2;
                writer.append(x1 + margin)
                      .append(" ").append(y1 - margin)
                      .append(" ").append(r1)
                      .append(" ").append(x2 + margin)
                      .append(" ").append(y2 - margin)
                      .append(" ").append(r2)
                      .append(" ").append(x2 + r2 + margin)
                      .append(" ").append(y2 - margin)
                      .append(" TC\n");
            }

            // Hexagons
            // Because MaxiCode size is fixed, this ignores magnification
            for (int i = 0; i < symbol.hexagons.size(); i++) {
                Hexagon hexagon = symbol.hexagons.get(i);
                for (int j = 0; j < 6; j++) {
                    writer.append(hexagon.pointX[j] + margin).append(" ").append((height - hexagon.pointY[j]) - margin).append(" ");
                }
                writer.append(" TH\n");
            }

            // Footer
            writer.append("\nshowpage\n");
        }
    }
}
