import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class GraphicsDisplay extends JPanel {
    private ArrayList<Double[]> graphicsData;
    private ArrayList<Double[]> originalData;
    private int selectedMarker = -1;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double[][] viewport = new double[2][2];
    private ArrayList<double[][]> undoHistory = new ArrayList(5);
    private double scaleX;
    private double scaleY;
    private BasicStroke axisStroke;
    private BasicStroke gridStroke;
    private BasicStroke markerStroke;
    private BasicStroke selectionStroke;
    private Font axisFont;
    private Font labelsFont;
    private static DecimalFormat formatter = (DecimalFormat)NumberFormat.getInstance();
    private boolean scaleMode = false;
    private boolean changeMode = false;
    private double[] originalPoint = new double[2];
    private Rectangle2D.Double selectionRect = new Rectangle2D.Double();

    public GraphicsDisplay() {
        this.setBackground(Color.WHITE);
        this.axisStroke = new BasicStroke(2.0F, 0, 0, 10.0F, (float[])null, 0.0F);
        this.gridStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[]{4.0F, 4.0F}, 0.0F);
        this.markerStroke = new BasicStroke(1.0F, 0, 0, 10.0F, (float[])null, 0.0F);
        this.selectionStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[]{10.0F, 10.0F}, 0.0F);
        this.axisFont = new Font("Serif", 1, 36);
        this.labelsFont = new Font("Serif", 0, 10);
        formatter.setMaximumFractionDigits(5);
        this.addMouseListener(new MouseHandler());
        this.addMouseMotionListener(new MouseMotionHandler());
    }

    public void displayGraphics(ArrayList<Double[]> graphicsData) {
        this.graphicsData = graphicsData;
        this.originalData = new ArrayList(graphicsData.size());

        for(Double[] point : graphicsData) {
            Double[] newPoint = new Double[]{new Double(point[0]), new Double(point[1])};
            this.originalData.add(newPoint);
        }

        this.minX = ((Double[])graphicsData.get(0))[0];
        this.maxX = ((Double[])graphicsData.get(graphicsData.size() - 1))[0];
        this.minY = ((Double[])graphicsData.get(0))[1];
        this.maxY = this.minY;

        for(int i = 1; i < graphicsData.size(); ++i) {
            if (((Double[])graphicsData.get(i))[1] < this.minY) {
                this.minY = ((Double[])graphicsData.get(i))[1];
            }

            if (((Double[])graphicsData.get(i))[1] > this.maxY) {
                this.maxY = ((Double[])graphicsData.get(i))[1];
            }
        }

        this.zoomToRegion(this.minX, this.maxY, this.maxX, this.minY);
    }

    public void zoomToRegion(double x1, double y1, double x2, double y2) {
        this.viewport[0][0] = x1;
        this.viewport[0][1] = y1;
        this.viewport[1][0] = x2;
        this.viewport[1][1] = y2;
        this.repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.scaleX = this.getSize().getWidth() / (this.viewport[1][0] - this.viewport[0][0]);
        this.scaleY = this.getSize().getHeight() / (this.viewport[0][1] - this.viewport[1][1]);
        if (this.graphicsData != null && this.graphicsData.size() != 0) {
            Graphics2D canvas = (Graphics2D)g;
            this.paintGrid(canvas);
            this.paintAxis(canvas);
            this.paintGraphics(canvas);
            this.paintMarkers(canvas);
            this.paintLabels(canvas);
            this.paintSelection(canvas);
        }
    }

    private void paintSelection(Graphics2D canvas) {
        if (this.scaleMode) {
            canvas.setStroke(this.selectionStroke);
            canvas.setColor(Color.BLACK);
            canvas.draw(this.selectionRect);
        }
    }

    private void paintGraphics(Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.RED);
        Double currentX = null;
        Double currentY = null;

        for(Double[] point : this.graphicsData) {
            if (!(point[0] < this.viewport[0][0]) && !(point[1] > this.viewport[0][1]) && !(point[0] > this.viewport[1][0]) && !(point[1] < this.viewport[1][1])) {
                if (currentX != null && currentY != null) {
                    canvas.draw(new Line2D.Double(this.translateXYtoPoint(currentX, currentY), this.translateXYtoPoint(point[0], point[1])));
                }

                currentX = point[0];
                currentY = point[1];
            }
        }

    }

    private void paintMarkers(Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.RED);
        canvas.setPaint(Color.RED);
        Ellipse2D.Double lastMarker = null;
        int i = -1;

        for(Double[] point : this.graphicsData) {
            ++i;
            if (!(point[0] < this.viewport[0][0]) && !(point[1] > this.viewport[0][1]) && !(point[0] > this.viewport[1][0]) && !(point[1] < this.viewport[1][1])) {
                int radius;
                if (i == this.selectedMarker) {
                    radius = 6;
                } else {
                    radius = 3;
                }

                Ellipse2D.Double marker = new Ellipse2D.Double();
                Point2D center = this.translateXYtoPoint(point[0], point[1]);
                Point2D corner = new Point2D.Double(center.getX() + (double)radius, center.getY() + (double)radius);
                marker.setFrameFromCenter(center, corner);
                if (i == this.selectedMarker) {
                    lastMarker = marker;
                } else {
                    canvas.draw(marker);
                    canvas.fill(marker);
                }
            }
        }

        if (lastMarker != null) {
            canvas.setColor(Color.BLUE);
            canvas.setPaint(Color.BLUE);
            canvas.draw(lastMarker);
            canvas.fill(lastMarker);
        }

    }

    private void paintLabels(Graphics2D canvas) {
        canvas.setColor(Color.BLACK);
        canvas.setFont(this.labelsFont);
        FontRenderContext context = canvas.getFontRenderContext();
        double labelYPos;
        if (this.viewport[1][1] < (double)0.0F && this.viewport[0][1] > (double)0.0F) {
            labelYPos = (double)0.0F;
        } else {
            labelYPos = this.viewport[1][1];
        }

        double labelXPos;
        if (this.viewport[0][0] < (double)0.0F && this.viewport[1][0] > (double)0.0F) {
            labelXPos = (double)0.0F;
        } else {
            labelXPos = this.viewport[0][0];
        }

        double pos = this.viewport[0][0];

        for(double step = (this.viewport[1][0] - this.viewport[0][0]) / (double)10.0F; pos < this.viewport[1][0]; pos += step) {
            Point2D.Double point = this.translateXYtoPoint(pos, labelYPos);
            String label = formatter.format(pos);
            Rectangle2D bounds = this.labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + (double)5.0F), (float)(point.getY() - bounds.getHeight()));
        }

        pos = this.viewport[1][1];

        for(double var15 = (this.viewport[0][1] - this.viewport[1][1]) / (double)10.0F; pos < this.viewport[0][1]; pos += var15) {
            Point2D.Double point = this.translateXYtoPoint(labelXPos, pos);
            String label = formatter.format(pos);
            Rectangle2D bounds = this.labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + (double)5.0F), (float)(point.getY() - bounds.getHeight()));
        }

        if (this.selectedMarker >= 0) {
            Point2D.Double point = this.translateXYtoPoint(((Double[])this.graphicsData.get(this.selectedMarker))[0], ((Double[])this.graphicsData.get(this.selectedMarker))[1]);
            String label = "X=" + formatter.format(((Double[])this.graphicsData.get(this.selectedMarker))[0]) + ", Y=" + formatter.format(((Double[])this.graphicsData.get(this.selectedMarker))[1]);
            Rectangle2D bounds = this.labelsFont.getStringBounds(label, context);
            canvas.setColor(Color.BLUE);
            canvas.drawString(label, (float)(point.getX() + (double)5.0F), (float)(point.getY() - bounds.getHeight()));
        }

    }

    private void paintGrid(Graphics2D canvas) {
        canvas.setStroke(this.gridStroke);
        canvas.setColor(Color.GRAY);
        double pos = this.viewport[0][0];

        for(double step = (this.viewport[1][0] - this.viewport[0][0]) / (double)10.0F; pos < this.viewport[1][0]; pos += step) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(pos, this.viewport[0][1]), this.translateXYtoPoint(pos, this.viewport[1][1])));
        }

        canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[1][0], this.viewport[0][1]), this.translateXYtoPoint(this.viewport[1][0], this.viewport[1][1])));
        pos = this.viewport[1][1];

        for(double var7 = (this.viewport[0][1] - this.viewport[1][1]) / (double)10.0F; pos < this.viewport[0][1]; pos += var7) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], pos), this.translateXYtoPoint(this.viewport[1][0], pos)));
        }

        canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], this.viewport[0][1]), this.translateXYtoPoint(this.viewport[1][0], this.viewport[0][1])));
    }

    private void paintAxis(Graphics2D canvas) {
        canvas.setStroke(this.axisStroke);
        canvas.setColor(Color.BLACK);
        canvas.setFont(this.axisFont);
        FontRenderContext context = canvas.getFontRenderContext();
        if (this.viewport[0][0] <= (double)0.0F && this.viewport[1][0] >= (double)0.0F) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint((double)0.0F, this.viewport[0][1]), this.translateXYtoPoint((double)0.0F, this.viewport[1][1])));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(-(this.viewport[1][0] - this.viewport[0][0]) * 0.0025, this.viewport[0][1] - (this.viewport[0][1] - this.viewport[1][1]) * 0.015), this.translateXYtoPoint((double)0.0F, this.viewport[0][1])));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint((this.viewport[1][0] - this.viewport[0][0]) * 0.0025, this.viewport[0][1] - (this.viewport[0][1] - this.viewport[1][1]) * 0.015), this.translateXYtoPoint((double)0.0F, this.viewport[0][1])));
            Rectangle2D bounds = this.axisFont.getStringBounds("y", context);
            Point2D.Double labelPos = this.translateXYtoPoint((double)0.0F, this.viewport[0][1]);
            canvas.drawString("y", (float)labelPos.x + 10.0F, (float)(labelPos.y + bounds.getHeight() / (double)2.0F));
        }

        if (this.viewport[1][1] <= (double)0.0F && this.viewport[0][1] >= (double)0.0F) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], (double)0.0F), this.translateXYtoPoint(this.viewport[1][0], (double)0.0F)));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[1][0] - (this.viewport[1][0] - this.viewport[0][0]) * 0.01, (this.viewport[0][1] - this.viewport[1][1]) * 0.005), this.translateXYtoPoint(this.viewport[1][0], (double)0.0F)));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[1][0] - (this.viewport[1][0] - this.viewport[0][0]) * 0.01, -(this.viewport[0][1] - this.viewport[1][1]) * 0.005), this.translateXYtoPoint(this.viewport[1][0], (double)0.0F)));
            Rectangle2D bounds = this.axisFont.getStringBounds("x", context);
            Point2D.Double labelPos = this.translateXYtoPoint(this.viewport[1][0], (double)0.0F);
            canvas.drawString("x", (float)(labelPos.x - bounds.getWidth() - (double)10.0F), (float)(labelPos.y - bounds.getHeight() / (double)2.0F));
        }

    }

    protected Point2D.Double translateXYtoPoint(double x, double y) {
        double deltaX = x - this.viewport[0][0];
        double deltaY = this.viewport[0][1] - y;
        return new Point2D.Double(deltaX * this.scaleX, deltaY * this.scaleY);
    }

    protected double[] translatePointToXY(int x, int y) {
        return new double[]{this.viewport[0][0] + (double)x / this.scaleX, this.viewport[0][1] - (double)y / this.scaleY};
    }

    protected int findSelectedPoint(int x, int y) {
        if (this.graphicsData == null) {
            return -1;
        } else {
            int pos = 0;

            for(Double[] point : this.graphicsData) {
                Point2D.Double screenPoint = this.translateXYtoPoint(point[0], point[1]);
                double distance = (screenPoint.getX() - (double)x) * (screenPoint.getX() - (double)x) + (screenPoint.getY() - (double)y) * (screenPoint.getY() - (double)y);
                if (distance < (double)100.0F) {
                    return pos;
                }

                ++pos;
            }

            return -1;
        }
    }

    public void reset() {
        this.displayGraphics(this.originalData);
    }

    public class MouseHandler extends MouseAdapter {
        public MouseHandler() {
        }

        public void mouseClicked(MouseEvent ev) {
            if (ev.getButton() == 3) {
                if (GraphicsDisplay.this.undoHistory.size() > 0) {
                    GraphicsDisplay.this.viewport = (double[][])GraphicsDisplay.this.undoHistory.get(GraphicsDisplay.this.undoHistory.size() - 1);
                    GraphicsDisplay.this.undoHistory.remove(GraphicsDisplay.this.undoHistory.size() - 1);
                } else {
                    GraphicsDisplay.this.zoomToRegion(GraphicsDisplay.this.minX, GraphicsDisplay.this.maxY, GraphicsDisplay.this.maxX, GraphicsDisplay.this.minY);
                }

                GraphicsDisplay.this.repaint();
            }

        }

        public void mousePressed(MouseEvent ev) {
            if (ev.getButton() == 1) {
                GraphicsDisplay.this.selectedMarker = GraphicsDisplay.this.findSelectedPoint(ev.getX(), ev.getY());
                GraphicsDisplay.this.originalPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());
                if (GraphicsDisplay.this.selectedMarker >= 0) {
                    GraphicsDisplay.this.changeMode = true;
                    GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(8));
                } else {
                    GraphicsDisplay.this.scaleMode = true;
                    GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(5));
                    GraphicsDisplay.this.selectionRect.setFrame((double)ev.getX(), (double)ev.getY(), (double)1.0F, (double)1.0F);
                }

            }
        }

        public void mouseReleased(MouseEvent ev) {
            if (ev.getButton() == 1) {
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(0));
                if (GraphicsDisplay.this.changeMode) {
                    GraphicsDisplay.this.changeMode = false;
                } else {
                    GraphicsDisplay.this.scaleMode = false;
                    double[] finalPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());
                    GraphicsDisplay.this.undoHistory.add(GraphicsDisplay.this.viewport);
                    GraphicsDisplay.this.viewport = new double[2][2];
                    GraphicsDisplay.this.zoomToRegion(GraphicsDisplay.this.originalPoint[0], GraphicsDisplay.this.originalPoint[1], finalPoint[0], finalPoint[1]);
                    GraphicsDisplay.this.repaint();
                }

            }
        }
    }

    public class MouseMotionHandler implements MouseMotionListener {
        public MouseMotionHandler() {
        }

        public void mouseMoved(MouseEvent ev) {
            GraphicsDisplay.this.selectedMarker = GraphicsDisplay.this.findSelectedPoint(ev.getX(), ev.getY());
            if (GraphicsDisplay.this.selectedMarker >= 0) {
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(8));
            } else {
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(0));
            }

            GraphicsDisplay.this.repaint();
        }

        public void mouseDragged(MouseEvent ev) {
            if (GraphicsDisplay.this.changeMode) {
                double[] currentPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());
                double newY = ((Double[])GraphicsDisplay.this.graphicsData.get(GraphicsDisplay.this.selectedMarker))[1] + (currentPoint[1] - ((Double[])GraphicsDisplay.this.graphicsData.get(GraphicsDisplay.this.selectedMarker))[1]);
                if (newY > GraphicsDisplay.this.viewport[0][1]) {
                    newY = GraphicsDisplay.this.viewport[0][1];
                }

                if (newY < GraphicsDisplay.this.viewport[1][1]) {
                    newY = GraphicsDisplay.this.viewport[1][1];
                }

                ((Double[])GraphicsDisplay.this.graphicsData.get(GraphicsDisplay.this.selectedMarker))[1] = newY;
                GraphicsDisplay.this.repaint();
            } else {
                double width = (double)ev.getX() - GraphicsDisplay.this.selectionRect.getX();
                if (width < (double)5.0F) {
                    width = (double)5.0F;
                }

                double height = (double)ev.getY() - GraphicsDisplay.this.selectionRect.getY();
                if (height < (double)5.0F) {
                    height = (double)5.0F;
                }

                GraphicsDisplay.this.selectionRect.setFrame(GraphicsDisplay.this.selectionRect.getX(), GraphicsDisplay.this.selectionRect.getY(), width, height);
                GraphicsDisplay.this.repaint();
            }

        }
    }
}
