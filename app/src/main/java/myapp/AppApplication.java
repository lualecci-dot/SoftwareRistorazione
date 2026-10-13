package myapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import javafx.application.Application;

@SpringBootApplication(scanBasePackages = "myapp")
public class AppApplication   {

	
	// Avvia JavaFX; il ciclo di vita JavaFX avvia e chiude a sua volta Spring.
	public static void main(String[] args) {
		//SpringApplication.run(AppApplication.class, args);
		Application.launch(App.class, args);
	}

}
