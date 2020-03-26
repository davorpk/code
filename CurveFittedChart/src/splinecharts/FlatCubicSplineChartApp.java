package splinecharts;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.SceneBuilder;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * @author Jasper Potts
 */
public class FlatCubicSplineChartApp extends Application
{
    double x = 0d;
    
    public static void main(String[] args)
    {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage)
    {
        // create chart and set as center content
        FlatCubicSplineChart chart = new FlatCubicSplineChart(
                new NumberAxis(0, 10000, 1000), new NumberAxis(-1000, 1000, 100));
        chart.setLegendVisible(false);
        chart.setHorizontalGridLinesVisible(false);
        chart.setVerticalGridLinesVisible(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setAlternativeRowFillVisible(false);

        Random random = new Random();
        final XYChart.Series<Number, Number> series1 = new XYChart.Series<>();
        final XYChart.Series<Number, Number> series2 = new XYChart.Series<>();
        int max = 20;
        int width = 10000 / max;
        for (int i = 0; i < max + 1; i++)
        {
            series1.getData().add(new XYChart.Data<>(i * width, random.nextDouble() * 1000));
            series2.getData().add(new XYChart.Data<>(i * width, Math.sin(x) * 1000));
            x += Math.PI / 4;
        }

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), (e -> {

            series1.getData()
                    .add(new XYChart.Data<>((max + 2) * width, random.nextDouble() * 1000));
            series2.getData()
                    .add(new XYChart.Data<>((max + 2) * width, Math.sin(x) * 1000));
            x += Math.PI / 4;
            for (int i = 0; i < max + 2; i++)
            {
                // series1.getData().get(i).setYValue(random.nextDouble() *
                // 1000);
                // series2.getData().get(i).setYValue(random.nextDouble() *
                // 1000);
                series1.getData().get(i).setXValue((i - 1) * width);
                series2.getData().get(i).setXValue((i - 1) * width);
            }
            series1.getData().remove(0);
            series2.getData().remove(0);
        })));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        /*
        series1.getData().addAll(
            new XYChart.Data<>(0       ,950),
            new XYChart.Data<>(2000    ,100),
            new XYChart.Data<>(5000    ,200),
            new XYChart.Data<>(7500    ,180),
            new XYChart.Data<>(10000   ,100)
        );
        
        series2.getData().addAll(
            new XYChart.Data<>(0       ,300),
            new XYChart.Data<>(2000    ,500),
            new XYChart.Data<>(5000    ,850),
            new XYChart.Data<>(7500    ,50),
            new XYChart.Data<>(10000   ,200)
        );
        */
        chart.getData().add(series1);
        chart.getData().add(series2);

        Scene scene = new Scene(chart, 500, 400);
        // scene.getStylesheets().add(SplineChartApp.class.getResource("CurveFittedChart.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
