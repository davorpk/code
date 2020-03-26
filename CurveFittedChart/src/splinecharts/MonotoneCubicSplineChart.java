package splinecharts;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

/**
 * Extended version of AreaChart that smooths the data points to a curve
 * 
 * @author Jasper Potts
 */
public class MonotoneCubicSplineChart extends LineChart<Number, Number>
{
    private int precision;

    public MonotoneCubicSplineChart(NumberAxis xAxis, NumberAxis yAxis)
    {
        this(xAxis, yAxis, 5);
    }


    public MonotoneCubicSplineChart(NumberAxis xAxis, NumberAxis yAxis, int precision)
    {
        super(xAxis, yAxis);
        if (precision <= 0)
        {
            throw new IllegalArgumentException("Requires precision > 0.");
        }
        this.precision = precision;
    }


    public int getPrecision()
    {
        return this.precision;
    }


    public void setPrecision(int p)
    {
        if (p <= 0)
        {
            throw new IllegalArgumentException("Requires p > 0.");
        }
        this.precision = p;
        // fireChangeEvent();
    }


    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (!(obj instanceof MonotoneCubicSplineChart))
            return false;
        MonotoneCubicSplineChart that = (MonotoneCubicSplineChart) obj;
        if (this.precision != that.precision)
            return false;
        return super.equals(obj);
    }


    /** @inheritDoc */
    @Override
    protected void layoutPlotChildren()
    {
        super.layoutPlotChildren();
        for (int seriesIndex = 0; seriesIndex < getDataSize(); seriesIndex++)
        {
            final XYChart.Series<Number, Number> series = getData().get(seriesIndex);
            final Path seriesLine = (Path) series.getNode();
            smooth(seriesLine.getElements(), this.precision);
        }
    }


    private int getDataSize()
    {
        final ObservableList<XYChart.Series<Number, Number>> data = getData();
        return (data != null) ? data.size() : 0;
    }


    private static void smooth(ObservableList<PathElement> strokeElements, int precision)
    {
        // as we do not have direct access to the data, first recreate the list
        // of all the data points we have
        final List<Point2D> dataPoints = new ArrayList<>();
        for (int i = 0; i < strokeElements.size(); i++)
        {
            final PathElement element = strokeElements.get(i);
            /* if (element instanceof MoveTo)
            {
                final MoveTo move = (MoveTo) element;
                dataPoints[i] = new Point2D(move.getX(), move.getY());
            }
            else*/ if (element instanceof LineTo)
            {
                final LineTo line = (LineTo) element;
                final double x = line.getX(), y = line.getY();
                dataPoints.add(new Point2D(x, y));
            }
        }

        // now clear and rebuild elements
        List<PathElement> cps = createPathElements(
                dataPoints.toArray(new Point2D[dataPoints.size()]), precision);
        strokeElements.clear();
        strokeElements.addAll(cps);
    }


    /**
     * Calculate open-ended Bezier Spline Control Points.
     * @param strokeElements 
     * 
     * @param points
     *                   Input data Bezier spline points.
     * @return 
     */
    public static List<PathElement> createPathElements(Point2D[] points, int precision)
    {
        List<PathElement> strokes = new ArrayList<>();
        int n = points.length - 1;
        if (n == 0)
        {
            return strokes;
        }
        strokes.add(new MoveTo(points[0].getX(), points[0].getY()));
        if (n == 1)
        {
            // Special case: Bezier curve should be a straight line.
            strokes.add(new LineTo(points[1].getX(), points[1].getY()));
            return strokes;
        }

        int np = points.length;
        double[] d = new double[np]; // Newton form coefficients
        double[] x = new double[np]; // x-coordinates of nodes

        for (int i = 0; i < np; i++)
        {
            x[i] = points[i].getX();
            d[i] = points[i].getY();
        }

        double[] delta = new double[np - 1];
        for (int i = 0; i < np - 1; i++)
        {
            delta[i] = (d[i + 1] - d[i]) / (x[i + 1] - x[i]);
        }

        double[] fix = new double[np];
        double[] m = new double[np];
        for (int i = 1; i < np - 1; i++)
        {
            m[i] = (delta[i - 1] + delta[i]) / 2;
            fix[i] = 0;
        }
        m[0] = delta[0];
        m[np - 1] = delta[np - 2];

        for (int i = 0; i < np - 1; i++)
        {
            if (delta[i] == 0)
            {
                fix[i] = 1.0;
                m[i] = 0.0;
                m[i + 1] = 0.0;
            }
        }

        double[] alpha = new double[np];
        double[] beta = new double[np];
        double[] dist = new double[np];
        double[] tau = new double[np];
        for (int i = 0; i <= np - 2; i++)
        {
            if (fix[i] == 0.0f)
            {
                alpha[i] = m[i] / delta[i];
                beta[i] = m[i + 1] / delta[i];
                dist[i] = alpha[i] * alpha[i] + beta[i] * beta[i];
                tau[i] = 3 / sqrt(dist[i]);
            }
        }
        for (int i = 0; i < np; i++)
        {
            if (dist[i] > 9)
            {
                m[i] = tau[i] * alpha[i] * delta[i];
                m[i + 1] = tau[i] * beta[i] * delta[i];
            }
        }

        double oldt = x[0], t1, t2;
        double oldy = d[0], t, y;
        double h00, h01, h10, h11;
        strokes.add(new MoveTo(oldt, oldy));
        for (int i = 0; i < np - 1; i++)
        {
            // loop over intervals between nodes
            for (int j = 1; j <= precision; j++)
            {
                double h = x[i + 1] - x[i];
                t1 = (h * j) / precision;
                t2 = x[i] + t1;
                t = j / (double) (precision);
                h00 = 2 * t * t * t - 3 * t * t + 1;
                h10 = t * t * t - 2 * t * t + t;
                h01 = -2 * t * t * t + 3 * t * t;
                h11 = t * t * t - t * t;
                y = h00 * d[i] + h10 * h * m[i] + h01 * d[i + 1] + h11 * h * m[i + 1];
                strokes.add(new LineTo(t2, y));
            }
        }
        return strokes;
    }
}