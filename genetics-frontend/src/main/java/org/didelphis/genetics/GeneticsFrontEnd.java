package org.didelphis.genetics;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Class {@code GeneticsFrontEnd}
 *
 * @author Samantha Fiona McCabe
 * @since 0.1.0 Date: 2017-07-08
 */
public class GeneticsFrontEnd extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		WebView webView = new WebView();
		WebEngine webEngine = webView.getEngine();
		URL url = GeneticsFrontEnd.class.getClassLoader()
				.getResource("GeneticsFrontEnd.html");
		webEngine.load(url == null ? "" : url.toString());
		primaryStage.setScene(new Scene(webView));
		primaryStage.show();
	}
}
