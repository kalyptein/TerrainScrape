package ui;

import java.util.*;

import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.stage.*;

public class FXDialogs
{
    public static Dialog<ButtonType> createTextAreaDialog(String title, Window owner)
    {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setResizable(true);
        dialog.getDialogPane().setContent(new TextArea());
        dialog.initModality(Modality.NONE);
        dialog.getDialogPane().getScene().getWindow().setOnCloseRequest(event -> dialog.close());
        return dialog;
    }

    public static TextInputDialog createTextInputDialog(String title, String headerText, String contentText, Window owner, String initial)
    {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);
        dialog.initModality(Modality.NONE);

        dialog.getDialogPane().getScene().getWindow().setOnCloseRequest(event -> dialog.close());
        
        return dialog;
    }

    public static <E> ChoiceDialog<E> createChoiceDialog(String title, String headerText, String contentText, Window owner, List<E> choices, E initial)
    {
        ChoiceDialog<E> dialog = new ChoiceDialog<>(initial, choices);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);
        dialog.initModality(Modality.NONE);

        dialog.getDialogPane().getScene().getWindow().setOnCloseRequest(event -> dialog.close());
        
        return dialog;
    }

    public static Alert createAlert(AlertType type, Window owner, String title, String header, String content, ButtonType... types)
    {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        if (header != null)
        	alert.setHeaderText(header);
        alert.setContentText(content);
        alert.setOnCloseRequest(event -> alert.close());
        
        if (types != null)
            alert.getButtonTypes().setAll(types);

        return alert;
    }
}
