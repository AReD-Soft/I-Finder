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
        // Menyiapkan GUI
        frame = new JFrame("I-Finder V1.0");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(720, 600); // Lebar 720 piksel
        frame.setLayout(new BorderLayout());
        
        // Mengubah ikon jendela (ganti "path/to/icon.png" dengan path ikon Anda)
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("icon.png")));


        // Panel input
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(7, 1)); // Menggunakan 7 baris untuk tata letak yang lebih baik

        inputPanel.add(new JLabel("Kata yang dicari:"));
        searchField = new JTextField();
        inputPanel.add(searchField);

        // Opsi sensitivitas huruf besar/kecil
        caseSensitiveCheckBox = new JCheckBox("Case Sensitive");
        inputPanel.add(caseSensitiveCheckBox);

        inputPanel.add(new JLabel("Folder:"));
        folderField = new JTextField();
        inputPanel.add(folderField);

        // Tombol Pilih Folder
        JButton folderButton = new JButton("Pilih Folder");
        folderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseFolder();
            }
        });
        JPanel folderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        folderPanel.add(folderButton);
        inputPanel.add(folderPanel);

        // Tombol Cari
        JButton searchButton = new JButton("Cari");
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

        // Panel hasil
        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS)); // Tata letak vertikal untuk hasil
        JScrollPane scrollPane = new JScrollPane(resultPanel);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        
        int returnValue = chooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = chooser.getSelectedFile();
            folderField.setText(selectedFolder.getAbsolutePath());
        }
    }

    private void searchFiles(String keyword, String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            resultPanel.removeAll();
            resultPanel.add(new JLabel("Folder tidak ditemukan."));
            resultPanel.revalidate();
            resultPanel.repaint();
            return;
        }

        resultPanel.removeAll();  // Menghapus hasil sebelumnya
        searchInFolder(keyword, folder);
        resultPanel.revalidate(); // Memperbarui panel hasil untuk menunjukkan hasil baru
        resultPanel.repaint();     // Memastikan panel diperbarui
    }

    private void searchInFolder(String keyword, File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchInFolder(keyword, file);  // Pencarian rekursif di subfolder
                } else if (file.getName().endsWith(".xml")) {
                    try {
                        // Membaca konten file, memperhatikan BOM untuk UTF-8 dan UTF-16
                        String content = readFileWithBOM(file);
                        // Regex untuk menemukan String="..."
                        String regex = "String=\"(.*?)\"";
                        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
                        Matcher matcher = pattern.matcher(content);
                        boolean found = false;

                        while (matcher.find()) {
                            String foundString = matcher.group(1).trim(); // Mendapatkan seluruh string di dalam tanda kutip
                            
                            // Memeriksa sensitivitas huruf besar/kecil
                            if (caseSensitiveCheckBox.isSelected()) {
                                if (foundString.equals(keyword)) {
                                    found = true; // Menetapkan bendera ke true jika kata kunci ditemukan
                                    addResultPanel(file, foundString);
                                }
                            } else {
                                if (foundString.toLowerCase().contains(keyword.toLowerCase())) {
                                    found = true; // Menetapkan bendera ke true jika kata kunci ditemukan
                                    addResultPanel(file, foundString);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        resultPanel.add(new JLabel("Terjadi kesalahan saat membaca file: " + file.getAbsolutePath()));
                    }
                }
            }
        }
    }

    private String readFileWithBOM(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        // Periksa BOM UTF-8
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8); // Mengabaikan BOM
        }
        // Periksa BOM UTF-16
        else if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE); // Mengabaikan BOM
        }
        // Jika tidak ada BOM, gunakan UTF-8 sebagai default
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void addResultPanel(File file, String foundString) {
        // Buat panel untuk setiap hasil
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BorderLayout());
    
        JLabel fileLabel = new JLabel("Ditemukan di: " + file.getAbsolutePath());
        filePanel.add(fileLabel, BorderLayout.NORTH);
    
        JLabel sentenceLabel = new JLabel("Kalimat: " + foundString);
        filePanel.add(sentenceLabel, BorderLayout.CENTER);
    
        // Membuat panel untuk tombol
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Menggunakan FlowLayout untuk rata kiri
        
        JButton openButton = new JButton("Buka");
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile(file);
            }
        });
    
        // Mengatur ukuran tombol lebih besar
        openButton.setPreferredSize(new Dimension(80, 20)); // Set ukuran tombol Buka
        buttonPanel.add(openButton); // Tambahkan tombol ke panel tombol
        
        filePanel.add(buttonPanel, BorderLayout.SOUTH); // Tambahkan panel tombol ke panel hasil
    
        resultPanel.add(filePanel); // Tambahkan panel file ke panel hasil
    }
    
    
    private void openFile(File file) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file);  // Membuka file dengan editor default
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Tidak dapat membuka file: " + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Fitur membuka file tidak didukung di sistem ini.", "Error", JOptionPane.ERROR_MESSAGE);
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
