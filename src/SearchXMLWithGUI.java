import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchXMLWithGUI {
    private JFrame frame;
    private JTextField searchField;
    private JTextField folderField;
    private JPanel resultPanel;
    private JCheckBox caseSensitiveCheckBox; // Checkbox untuk sensitivitas huruf besar/kecil

    public SearchXMLWithGUI() {
        // Setting up GUI
        frame = new JFrame("I-Finder V1.0");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(720, 600); // Lebar 720 piksel
        frame.setLayout(new BorderLayout());
        
        // Change window icon (replace "path/to/icon.png" with your icon path)
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("icon.png")));

         // Center window on the screen
        frame.setLocationRelativeTo(null);

        // Input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(7, 1)); // Using 7 rows for better layout

        inputPanel.add(new JLabel("Search Keyword:"));
        searchField = new JTextField();
        inputPanel.add(searchField);

        // Case sensitivity options
        caseSensitiveCheckBox = new JCheckBox("Case Sensitive");
        inputPanel.add(caseSensitiveCheckBox);

        inputPanel.add(new JLabel("Folder:"));
        folderField = new JTextField();
        inputPanel.add(folderField);

        // Choose Folder Button
        JButton folderButton = new JButton("Choose Folder");
        folderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseFolder();
            }
        });
        JPanel folderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        folderPanel.add(folderButton);
        inputPanel.add(folderPanel);

        // Search Button
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String keyword = searchField.getText();
                String folderPath = folderField.getText();
                searchFiles(keyword, folderPath);
            }
        });
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(searchButton);
        inputPanel.add(searchPanel);

        frame.add(inputPanel, BorderLayout.NORTH);

         // Result panel
        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS)); // Vertical layout for results
        JScrollPane scrollPane = new JScrollPane(resultPanel);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
    
        // Ensure the dialog is centered on the screen with reference to the frame
        int returnValue = chooser.showOpenDialog(frame); // frame is the main window that has been set to the center of the screen
    
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = chooser.getSelectedFile();
            folderField.setText(selectedFolder.getAbsolutePath());
        }
    }

    //Search Folder location
    private void searchFiles(String keyword, String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            resultPanel.removeAll();
            resultPanel.add(new JLabel("Folder not found."));
            resultPanel.revalidate();
            resultPanel.repaint();
            return;
        }

        resultPanel.removeAll();  // Clear previous results
        searchInFolder(keyword, folder);
        resultPanel.revalidate(); // Refresh result panel to show new results
        resultPanel.repaint();     // Ensure the panel is updated
    }

    
    private void searchInFolder(String keyword, File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchInFolder(keyword, file);  // Recursive search in subfolders
                } else if (file.getName().endsWith(".xml")) {
                    try {
                        // Read file content, taking into account BOM for UTF-8 and UTF-16
                        String content = readFileWithBOM(file);
                        // Regex to find String="..."
                        String regex = "String=\"(.*?)\"";
                        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
                        Matcher matcher = pattern.matcher(content);
                        boolean found = false;

                        while (matcher.find()) {
                            String foundString = matcher.group(1).trim(); // Mendapatkan seluruh string di dalam tanda kutip
                            
                             // Check case sensitivity
                            if (caseSensitiveCheckBox.isSelected()) {
                                if (foundString.equals(keyword)) {
                                    found = true; // Set flag to true if keyword is found
                                    addResultPanel(file, foundString);
                                }
                            } else {
                                if (foundString.toLowerCase().contains(keyword.toLowerCase())) {
                                    found = true; // Set flag to true if keyword is found
                                    addResultPanel(file, foundString);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        resultPanel.add(new JLabel("An error occurred while reading the file: " + file.getAbsolutePath()));
                    }
                }
            }
        }
    }

    //Read files
    private String readFileWithBOM(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        // Check BOM UTF-8
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8); // Mengabaikan BOM
        }
        // Check BOM UTF-16
        else if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE); // Mengabaikan BOM
        }
        // If no BOM, use UTF-8 as default
        return new String(bytes, StandardCharsets.UTF_8);
    }

    //Result
    private void addResultPanel(File file, String foundString) {
         // Create a panel for each result
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BorderLayout());
    
        JLabel fileLabel = new JLabel("Found in: " + file.getAbsolutePath());
        filePanel.add(fileLabel, BorderLayout.NORTH);
    
        JLabel sentenceLabel = new JLabel("Sentence: " + foundString);
        filePanel.add(sentenceLabel, BorderLayout.CENTER);
    
        // Create a panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Using FlowLayout for left alignment
    
        JButton openButton = new JButton("Open");
        openButton.setPreferredSize(new Dimension(80, 20)); // Set size for Open button
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile(file);
            }
        });
        buttonPanel.add(openButton); // Add button to button panel
    
        // Tombol Edit
        JButton editButton = new JButton("Edit");
        editButton.setPreferredSize(new Dimension(80, 20)); // Set size for Edit button
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openEditDialog(file, foundString); // Call edit dialog
            }
        });
        buttonPanel.add(editButton); // Add Edit button to button panel
    
        filePanel.add(buttonPanel, BorderLayout.SOUTH); // Add button panel to result panel
    
        resultPanel.add(filePanel); // Add file panel to result panel
    }
    
    
    private void openFile(File file) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file);  // Open file with default editor
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Cannot open file: " + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(frame, "File opening feature is not supported on this system.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openEditDialog(File file, String foundString) {
       // Create a new dialog for editing
        JDialog editDialog = new JDialog(frame, "Edit Text", true);
        editDialog.setSize(400, 250);
        editDialog.setLayout(new BorderLayout());
        
         // Position dialog in the center of the screen
        editDialog.setLocationRelativeTo(null);

        // Input form for editing text
        JTextField editField = new JTextField(foundString);
        editDialog.add(new JLabel("Edit text:"), BorderLayout.NORTH);
        editDialog.add(editField, BorderLayout.CENTER);
    
        // Dropdown for selecting translation language
        String[] languages = {"id", "en", "fr", "es", "de", "zh-CN", "ja"}; // Tambahkan kode bahasa lainnya jika diperlukan
        JComboBox<String> languageDropdown = new JComboBox<>(languages);
        JPanel dropdownPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dropdownPanel.add(new JLabel("Select Translation Language:"));
        dropdownPanel.add(languageDropdown);
        editDialog.add(dropdownPanel, BorderLayout.NORTH);
    
        // Panel for Translate, Save and Cancel buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
        // Translate button
        JButton translateButton = new JButton("Translate");
        translateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedLanguage = (String) languageDropdown.getSelectedItem();
                String textToTranslate = editField.getText();
                translateText(textToTranslate, selectedLanguage, editField);
            }
        });
        buttonPanel.add(translateButton);
    
        // Save button
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newText = editField.getText();
                if (!newText.isEmpty()) {
                    updateFileContent(file, foundString, newText); // Save changes to file
                    editDialog.dispose(); // Close the dialog after saving
                }
            }
        });
        buttonPanel.add(saveButton);
    
        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editDialog.dispose(); // Close the dialog without saving
            }
        });
        buttonPanel.add(cancelButton);
    
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
        editDialog.setVisible(true); // Show dialog
    }
    
    private void translateText(String text, String targetLanguage, JTextField editField) {
        try {
            // Create a URL for the Google Translate API
            String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + targetLanguage + "&dt=t&q=" + java.net.URLEncoder.encode(text, "UTF-8");
            
            // Use URI to resolve URL issues
            java.net.URI uri = new java.net.URI(urlStr);
            java.net.URL url = uri.toURL();  // Convert URI to URL
        
           // Opens an HTTP connection
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        
            // Read results from the API
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
        
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        
            // Parsing JSON response (results from Google Translate API)
            String jsonResponse = response.toString();
            // The translation results will be in the first array of JSON
            String translatedText = jsonResponse.split("\"")[1];
        
           // Update the input field with the translation results
            editField.setText(translatedText);
        
            JOptionPane.showMessageDialog(frame, "Translation successful.", "Information", JOptionPane.INFORMATION_MESSAGE);
        
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Failed to translate text.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }    

    private void updateFileContent(File file, String oldText, String newText) {
        try {
            // Read file contents in UTF-16 LE format while preserving the BOM
            byte[] fileBytes = Files.readAllBytes(file.toPath());
    
            // Check whether the file contains a UTF-16 LE BOM
            boolean hasBOM = (fileBytes.length >= 2 && fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xFE);
            String content;
            
            // If there is a BOM, read the file starting from the 2nd byte
            if (hasBOM) {
                content = new String(fileBytes, 2, fileBytes.length - 2, StandardCharsets.UTF_16LE);
            } else {
                content = new String(fileBytes, StandardCharsets.UTF_16LE);
            }
    
            // Replace old text with new text
            content = content.replace(oldText, newText);
    
            // Prepare data for rewriting (with BOM if previously there)
            byte[] newContentBytes;
            if (hasBOM) {
                // Re-add the BOM before writing updated content
                byte[] bom = {(byte) 0xFF, (byte) 0xFE};
                byte[] contentBytes = content.getBytes(StandardCharsets.UTF_16LE);
                newContentBytes = new byte[bom.length + contentBytes.length];
                System.arraycopy(bom, 0, newContentBytes, 0, bom.length);
                System.arraycopy(contentBytes, 0, newContentBytes, bom.length, contentBytes.length);
            } else {
                // If there is no BOM, just write the content in UTF-16 LE
                newContentBytes = content.getBytes(StandardCharsets.UTF_16LE);
            }
    
           // Rewrite the file with updated content
            Files.write(file.toPath(), newContentBytes);
    
            JOptionPane.showMessageDialog(frame, "Text saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Failed to save changes.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SearchXMLWithGUI();
            }
        });
    }
}
