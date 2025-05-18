package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import java.io.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class Main extends Application {

    private static final String API_KEY = System.getenv("GEMINI_API_KEY");
    private ComboBox<String> contentTypeComboBox;
    private TextField subjectTextField;
    private Button generateButton;
    private Label promptLabel;
    private Label subjectLabel;
    private Label resultLabel;
    private BorderPane mainPane;
    private ScrollPane scrollPane;
    private TextFlow resultTextFlow;
    private ProgressIndicator progressIndicator;

    private static final String ACCENT_COLOR = "#4285F4";
    private static final String SECONDARY_COLOR = "#34A853"; // Google green
    private static final String BACKGROUND_COLOR = "#F8F9FA";
    private static final String CARD_COLOR = "#FFFFFF";

    @Override
    public void start(Stage primaryStage) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            showAlert("Configuration Manquante",
                    "La variable d'environnement GEMINI_API_KEY n'est pas définie.\n" +
                            "Veuillez définir cette variable avec votre clé API Gemini pour utiliser l'application.");
            Platform.exit();
            return;
        }

        primaryStage.setTitle("Java TextGen : Création Automatisée d’Articles et Blogs");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/static/images/right.png")));

        // Apply modern styling to components
        createComponents();
        layoutComponents();

        Scene scene = new Scene(mainPane, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        // Event handling
        generateButton.setOnAction(event -> {
            String contentType = contentTypeComboBox.getValue();
            String subject = subjectTextField.getText();
            if (subject.isEmpty()) {
                showAlert("Erreur", "Veuillez entrer un sujet.");
                return;
            }
            generateContent(contentType, subject);
        });
    }
    private Button downloadButton;
    private void createComponents() {
        // Styled input components
        contentTypeComboBox = new ComboBox<>();
        contentTypeComboBox.getItems().addAll("Article", "Contenu de Blog", "Rapport", "Résumé", "Étude de cas");
        contentTypeComboBox.setValue("Article");
        contentTypeComboBox.setPrefWidth(200);
        contentTypeComboBox.getStyleClass().add("styled-combo-box");

        subjectTextField = new TextField();
        subjectTextField.setPromptText("Entrez le sujet ici");
        subjectTextField.setPrefWidth(300);
        subjectTextField.getStyleClass().add("styled-text-field");

        // Load images safely or use null
        Image generateIcon = loadImageSafely("/static/images/generate.png");
        generateButton = new Button("Générer");
        generateButton.getStyleClass().add("primary-button");
        generateButton.setPrefWidth(150);
        if (generateIcon != null) {
            ImageView generateIconView = new ImageView(generateIcon);
            generateIconView.setFitHeight(24); // Increased from 16 to 24
            generateIconView.setFitWidth(24);  // Increased from 16 to 24
            generateIconView.setPreserveRatio(true);
            generateButton.setGraphic(generateIconView);
        }

        // Add download button
        Image downloadIcon = loadImageSafely("/static/images/download.png");
        downloadButton = new Button("Télécharger PDF");
        downloadButton.getStyleClass().add("secondary-button");
        if (downloadIcon != null) {
            ImageView downloadIconView = new ImageView(downloadIcon);
            downloadIconView.setFitHeight(24); // Increased from 16 to 24
            downloadIconView.setFitWidth(24);  // Increased from 16 to 24
            downloadIconView.setPreserveRatio(true);
            downloadButton.setGraphic(downloadIconView);
        }
        downloadButton.setDisable(true); // Disabled until content is generated

        // Labels with modern styling
        promptLabel = new Label("Type de Contenu");
        promptLabel.getStyleClass().add("label-header");

        subjectLabel = new Label("Sujet");
        subjectLabel.getStyleClass().add("label-header");

        resultLabel = new Label("Résultat");
        resultLabel.getStyleClass().add("section-header");

        // Result area
        resultTextFlow = new TextFlow();
        resultTextFlow.setPadding(new Insets(15));
        resultTextFlow.setLineSpacing(8);
        resultTextFlow.getStyleClass().add("result-text-flow");

        scrollPane = new ScrollPane(resultTextFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.getStyleClass().add("custom-scroll-pane");

        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(40, 40);
    }

    // Helper method to safely load images
    private Image loadImageSafely(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                System.out.println("Warning: Image not found at path: " + path);
                return null;
            }
            return new Image(is);
        } catch (Exception e) {
            System.out.println("Error loading image from " + path + ": " + e.getMessage());
            return null;
        }
    }

    private void layoutComponents() {
        // Header with logo
        HBox headerBox = createHeader();

        // Input form in a card
        VBox formCard = new VBox(15);
        formCard.setPadding(new Insets(20));
        formCard.getStyleClass().add("card");

        // Content type selection
        HBox contentTypeRow = new HBox(15);
        contentTypeRow.setAlignment(Pos.CENTER_LEFT);
        contentTypeRow.getChildren().addAll(promptLabel, contentTypeComboBox);

        // Subject input
        HBox subjectRow = new HBox(15);
        subjectRow.setAlignment(Pos.CENTER_LEFT);
        subjectRow.getChildren().addAll(subjectLabel, subjectTextField);

        // Generate button directly below subject row
        HBox buttonRow = new HBox();
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.setPadding(new Insets(5, 0, 0, 0));
        buttonRow.getChildren().add(generateButton);

        formCard.getChildren().addAll(contentTypeRow, subjectRow, buttonRow);

        // Result card
        VBox resultCard = new VBox(10);
        resultCard.setPadding(new Insets(20));
        resultCard.getStyleClass().add("card");

        // Result card with download button
        HBox resultHeaderBox = new HBox(10);
        resultHeaderBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        resultHeaderBox.getChildren().addAll(resultLabel, spacer, downloadButton);

        StackPane resultContainer = new StackPane();
        resultContainer.getChildren().addAll(scrollPane, progressIndicator);
        StackPane.setAlignment(progressIndicator, Pos.CENTER);

        resultCard.getChildren().addAll(resultHeaderBox, resultContainer);

        // Main content - removed the separate button box
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(20));
        contentBox.getChildren().addAll(formCard, resultCard);

        // Main layout
        mainPane = new BorderPane();
        mainPane.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");
        mainPane.setTop(headerBox);
        mainPane.setCenter(contentBox);
        downloadButton.setOnAction(event -> saveAsPdf());
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: " + CARD_COLOR + "; -fx-border-color: #EAEAEA; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        // Load images safely
        Image leftLogo = loadImageSafely("/static/images/left.png");
        Image rightLogo = loadImageSafely("/static/images/right.png");

        // Logo on the left with increased size
        if (leftLogo != null) {
            ImageView logoLeft = new ImageView(leftLogo);
            logoLeft.setFitHeight(60); // Increased from 40 to 60
            logoLeft.setPreserveRatio(true);
            header.getChildren().add(logoLeft);
        }

        // Title in the middle
        Label titleLabel = new Label("Java TextGen : Création Automatisée d'Articles et Blogs");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_COLOR + ";");

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        header.getChildren().addAll(spacer1, titleLabel, spacer2);

        // Gemini logo on the right with increased size
        if (rightLogo != null) {
            ImageView logoRight = new ImageView(rightLogo);
            logoRight.setFitHeight(175); // Increased from 40 to 60
            logoRight.setPreserveRatio(true);
            header.getChildren().add(logoRight);
        }

        return header;
    }

    private void generateContent(String contentType, String subject) {
        String prompt = "Génère pour moi un " + contentType.toLowerCase() + " sur le sujet suivant : " + subject;
        Platform.runLater(() -> {
            resultTextFlow.getChildren().clear();
            progressIndicator.setVisible(true);
        });
        generateButton.setDisable(true);
        downloadButton.setDisable(true);

        new Thread(() -> {
            try {
                String response = askGemini(prompt);
                Platform.runLater(() -> {
                    displayFormattedResult(response);
                    generateButton.setDisable(false);
                    downloadButton.setDisable(false); // Enable download after content is generated
                    progressIndicator.setVisible(false);
                });
            } catch (RuntimeException e) {
                Platform.runLater(() -> {
                    resultTextFlow.getChildren().clear();
                    Text errorText = new Text("Erreur : " + e.getMessage());
                    errorText.setFill(Color.RED);
                    resultTextFlow.getChildren().add(errorText);
                    generateButton.setDisable(false);
                    progressIndicator.setVisible(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void saveAsPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("document.pdf");

        File file = fileChooser.showSaveDialog(mainPane.getScene().getWindow());
        if (file != null) {
            try {
                // Show progress
                progressIndicator.setVisible(true);

                // Create a new background task for PDF creation
                new Thread(() -> {
                    try {
                        generatePdf(file);
                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            showSuccessAlert("PDF créé avec succès",
                                    "Le fichier a été enregistré sous: \n" + file.getAbsolutePath());
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            showAlert("Erreur", "Impossible de créer le PDF: " + e.getMessage());
                        });
                        e.printStackTrace();
                    }
                }).start();
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de créer le PDF: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void generatePdf(File file) throws Exception {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        float yPosition = 750; // Starting Y position
        float pageHeight = page.getMediaBox().getHeight();
        float margin = 50;

        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        try {
            // Basic configuration
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);

            // Get the text from the TextFlow
            StringBuilder textContent = new StringBuilder();
            for (Node node : resultTextFlow.getChildren()) {
                if (node instanceof Text) {
                    Text text = (Text) node;
                    textContent.append(text.getText());
                }
            }

            // Simple text rendering - just demonstrating the concept
            String[] lines = textContent.toString().split("\n");
            float leading = 15;

            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    yPosition -= leading;
                    continue;
                }

                // Check if we need a new page
                if (yPosition < margin + leading) {
                    // End current page
                    contentStream.endText();
                    contentStream.close();

                    // Create a new page
                    page = new PDPage();
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = 750;

                    // Reset text positioning
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                }

                // Format headings and regular text differently
                if (line.trim().startsWith("# ") || line.trim().startsWith("## ")) {
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    contentStream.showText(line.replaceAll("^#+\\s+", ""));
                    yPosition -= leading;
                } else {
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    // Handle lines that are too long
                    if (line.length() > 80) {
                        // Simple line wrapping
                        int chunkSize = 80;
                        for (int i = 0; i < line.length(); i += chunkSize) {
                            // Check if we need a new page for this chunk
                            if (yPosition < margin + leading) {
                                contentStream.endText();
                                contentStream.close();
                                page = new PDPage();
                                document.addPage(page);
                                contentStream = new PDPageContentStream(document, page);
                                yPosition = 750;
                                contentStream.beginText();
                                contentStream.newLineAtOffset(margin, yPosition);
                                contentStream.setFont(PDType1Font.HELVETICA, 12);
                            }

                            String chunk = line.substring(i, Math.min(i + chunkSize, line.length()));
                            contentStream.showText(chunk);
                            yPosition -= leading;
                            contentStream.newLineAtOffset(0, -leading);
                        }
                    } else {
                        contentStream.showText(line);
                        yPosition -= leading;
                    }
                }

                // Move to the next line
                contentStream.newLineAtOffset(0, -leading);
                yPosition -= leading;
            }

            contentStream.endText();
        } finally {
            // Always close the content stream
            if (contentStream != null) {
                contentStream.close();
            }
        }

        document.save(file);
        document.close();
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String askGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + API_KEY;

        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);

            JSONArray parts = new JSONArray();
            parts.put(textPart);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);

            try (OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream())) {
                writer.write(requestBody.toString());
                writer.flush();
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    return extractGeminiResponse(response.toString());
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    throw new RuntimeException("Erreur HTTP " + responseCode + ": " + errorResponse.toString());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Erreur réseau lors de l'appel à l'API Gemini : " + e.getMessage(), e);
        }
    }

    private String extractGeminiResponse(String response) {
        JSONObject json = new JSONObject(response);
        JSONArray candidates = json.getJSONArray("candidates");
        if (candidates.length() > 0) {
            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            if (parts.length() > 0) {
                return parts.getJSONObject(0).getString("text").trim();
            } else {
                return "Réponse Gemini vide (pas de partie de texte).";
            }
        } else {
            return "Réponse Gemini vide (pas de candidats).";
        }
    }

    private void displayFormattedResult(String result) {
        resultTextFlow.getChildren().clear();
        String[] lines = result.split("\n");

        for (String line : lines) {
            if (line.startsWith("# ")) {
                // Titre niveau 1 (h1)
                String content = line.substring(2);
                Text title = new Text(content + "\n");
                title.setFont(Font.font("System", FontWeight.BOLD, 24));
                title.setFill(Color.web(ACCENT_COLOR));
                resultTextFlow.getChildren().add(title);

            } else if (line.startsWith("## ")) {
                // Titre niveau 2 (h2)
                String content = line.substring(3);
                Text title = new Text(content + "\n");
                title.setFont(Font.font("System", FontWeight.BOLD, 20));
                title.setFill(Color.web(ACCENT_COLOR));
                resultTextFlow.getChildren().add(title);

            } else if (line.startsWith("### ")) {
                // Titre niveau 3 (h3)
                String content = line.substring(4);
                Text title = new Text(content + "\n");
                title.setFont(Font.font("System", FontWeight.BOLD, 18));
                title.setFill(Color.web(SECONDARY_COLOR));
                resultTextFlow.getChildren().add(title);

            } else if (line.startsWith("* ") || line.startsWith("· ") || line.startsWith(". ") || line.startsWith("- ")) {
                // Liste à puces
                String content = line.substring(2);
                Text bullet = new Text("• " + content + "\n");
                bullet.setFont(Font.font("System", 14));
                resultTextFlow.getChildren().add(bullet);

            } else if (line.trim().startsWith("1.") || line.trim().startsWith("2.") || line.trim().matches("^\\d+\\..*")) {
                // Liste numérotée
                Text numbered = new Text(line + "\n");
                numbered.setFont(Font.font("System", 14));
                resultTextFlow.getChildren().add(numbered);

            } else if (line.trim().isEmpty()) {
                // Ligne vide pour l'espacement
                resultTextFlow.getChildren().add(new Text("\n"));

            } else {
                // Texte normal avec formatage
                List<Text> parts = parseBoldItalic(line);
                parts.add(new Text("\n"));
                resultTextFlow.getChildren().addAll(parts);
            }
        }
    }

    private List<Text> parseBoldItalic(String line) {
        List<Text> texts = new ArrayList<>();
        int currentIndex = 0;

        while (currentIndex < line.length()) {
            int startBold = line.indexOf("**", currentIndex);
            int startItalic = line.indexOf("*", currentIndex);

            // Pas de formatage trouvé
            if (startBold == -1 && startItalic == -1) {
                String normalText = line.substring(currentIndex);
                if (!normalText.isEmpty()) {
                    Text text = new Text(normalText);
                    text.setFont(Font.font("System", FontWeight.NORMAL, FontPosture.REGULAR, 14));
                    texts.add(text);
                }
                break;
            }

            // Détermine quel formatage vient en premier
            int nextFormatPos;
            boolean isBold = false;

            if (startBold == -1) {
                nextFormatPos = startItalic;
            } else if (startItalic == -1) {
                nextFormatPos = startBold;
                isBold = true;
            } else {
                if (startBold < startItalic) {
                    nextFormatPos = startBold;
                    isBold = true;
                } else {
                    nextFormatPos = startItalic;
                }
            }

            // Ajouter le texte normal avant le formatage
            if (nextFormatPos > currentIndex) {
                String normalText = line.substring(currentIndex, nextFormatPos);
                Text text = new Text(normalText);
                text.setFont(Font.font("System", FontWeight.NORMAL, FontPosture.REGULAR, 14));
                texts.add(text);
            }

            if (isBold) {
                // Traiter le texte en gras
                int endBold = line.indexOf("**", startBold + 2);
                if (endBold == -1) {
                    // Pas de fermeture, traiter comme texte normal
                    String normalText = line.substring(startBold);
                    Text text = new Text(normalText);
                    text.setFont(Font.font("System", FontWeight.NORMAL, FontPosture.REGULAR, 14));
                    texts.add(text);
                    break;
                }

                String boldText = line.substring(startBold + 2, endBold);
                Text text = new Text(boldText);
                text.setFont(Font.font("System", FontWeight.BOLD, FontPosture.REGULAR, 14));
                texts.add(text);
                currentIndex = endBold + 2;
            } else {
                // Traiter le texte en italique
                int endItalic = line.indexOf("*", startItalic + 1);
                if (endItalic == -1) {
                    // Pas de fermeture, traiter comme texte normal
                    String normalText = line.substring(startItalic);
                    Text text = new Text(normalText);
                    text.setFont(Font.font("System", FontWeight.NORMAL, FontPosture.REGULAR, 14));
                    texts.add(text);
                    break;
                }

                String italicText = line.substring(startItalic + 1, endItalic);
                Text text = new Text(italicText);
                text.setFont(Font.font("System", FontWeight.NORMAL, FontPosture.ITALIC, 14));
                texts.add(text);
                currentIndex = endItalic + 1;
            }
        }

        return texts;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}