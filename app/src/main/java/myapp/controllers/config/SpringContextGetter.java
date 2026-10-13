package myapp.controllers.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// Classe per ottenere il contesto Spring in modo statico
@Component
public class SpringContextGetter implements ApplicationContextAware {
    private static ApplicationContext context;

    // Spring chiama questo metodo durante l'avvio per rendere disponibile il contesto ai controller JavaFX.
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
        System.out.println("✅ Contesto Spring caricato correttamente nel Getter!");
    }

    // Metodo statico per ottenere un bean dal contesto
    public static <T> T getBean(Class<T> beanClass) {
            // Fallisce esplicitamente se richiesto prima che Spring abbia completato l'avvio.
        if (context == null) {
            throw new IllegalStateException("Il contesto Spring non è ancora pronto!");
        }
        return context.getBean(beanClass);
    }
}
