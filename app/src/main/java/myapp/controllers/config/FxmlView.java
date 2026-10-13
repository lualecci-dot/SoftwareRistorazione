package myapp.controllers.config;

public enum FxmlView {

    // Associa ogni vista navigabile al relativo file FXML nel classpath.
    LOGIN {
        @Override
        public String getFxmlName() {
            return "/myapp/Login.fxml";
        }
    },
    DATABASEPAGE{
        @Override
        public String getFxmlName() {
            return "/myapp/Database.fxml";
        }
    },
    HOMEPAGE{
        @Override
        public String getFxmlName() {
            return "/myapp/Home.fxml";
        }
    },
     CREADATABASE{
        @Override
        public String getFxmlName() {
            return "/myapp/CreaTabella.fxml";
        }
    },
    CAMERIERI {
        @Override
        public String getFxmlName() {
            return "/myapp/Camerieri.fxml";
        }
    };

       
         public abstract String getFxmlName();

            public String getTitle() {
                return this.name(); }}