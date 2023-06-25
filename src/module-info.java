module project_server {
	requires javafx.controls;
	requires javafx.fxml;
	requires java.sql;
	
	opens server to javafx.graphics, javafx.fxml;
}
