package myapp.controllers.Service;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prodotti") // Deve essere uguale al nome su pgAdmin
public class Prodotto {

    // Entità JPA usata dal repository; le tabelle create dinamicamente sono lette dal servizio SQL.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nome;
    private Double prezzo; // Assicurati che il tipo coincida (Double o Float)

    // Costruttore vuoto obbligatorio
    public Prodotto() {}

    public Prodotto(String nome, Double prezzo) {
        this.nome = nome;
        this.prezzo = prezzo;
    }

    // Getter e Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Double getPrezzo() { return prezzo; }
    public void setPrezzo(Double prezzo) { this.prezzo = prezzo; }
}
