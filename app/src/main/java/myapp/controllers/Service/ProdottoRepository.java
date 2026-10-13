package myapp.controllers.Service;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProdottoRepository extends JpaRepository<Prodotto, Integer> {

    // Spring Data genera l'implementazione della cancellazione in base al nome del metodo.
  @Transactional
    void deleteByNome(String nome);

    
  
    
    // Non devi scrivere nulla qui. 
    // JpaRepository fornisce già: save(), deleteById(), findAll(), findById().
}