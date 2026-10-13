package myapp.controllers.config;

public final class DatabaseConfig {
    // Legge la connessione da variabili d'ambiente e usa i valori locali come fallback.
    public static final String URL = System.getenv().getOrDefault(
            "DB_URL", "jdbc:postgresql://localhost:5432/SpringDatabaseProdotti");
    public static final String USERNAME = System.getenv().getOrDefault("DB_USERNAME", "postgres");
    public static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "1234");

    private DatabaseConfig() {
    }
}
