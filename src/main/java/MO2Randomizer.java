import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class MO2Randomizer {
  private static final String PROPERTIES_FILE_NAME = "mo2-randomizer.properties";
  private static final String PROPERTIES_HISTORY_NAME = "history";

  private static final Properties PROPERTIES = new Properties();

  private static String selectedVariant = null;

  public static void main(String[] args) throws Exception {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      PROPERTIES.load(new FileInputStream(PROPERTIES_FILE_NAME));

      List<String> variantHistoryList = new ArrayList<String>(Arrays.asList(PROPERTIES.getProperty(PROPERTIES_HISTORY_NAME).split(",")));

      for (File sourceGroup : new File(PROPERTIES.getProperty("source.directory")).listFiles()) {
        File sourceMod = rename(shuffle(sourceGroup).get(0));

        for (File sourcePart : sourceMod.listFiles()) {
          List<File> sourceArchiveList = shuffle(sourcePart);
          if (sourceArchiveList.size() <= variantHistoryList.size()) {
            variantHistoryList.clear();
          }
          for (File sourceArchive : sourceArchiveList) {
            String sourceArchiveBaseName = FilenameUtils.getBaseName(sourceArchive.getName());
            selectedVariant = selectedVariant == null && !variantHistoryList.contains(sourceArchiveBaseName) ? sourceArchiveBaseName : selectedVariant;

            if (sourceArchiveBaseName.equals(selectedVariant)) {
              File destinationMod = new File(PROPERTIES.getProperty("destination.directory") + "\\RANDOM " + sourceGroup.getName() + sourcePart.getName());
              log("Delete");
              for (File destinationFile : destinationMod.listFiles()) {
                if (!destinationFile.getName().equalsIgnoreCase("meta.ini")) {
                  log(destinationFile.getAbsolutePath());
                  FileUtils.deleteDirectory(destinationFile);
                }
              }

              log("Extract", sourceArchive.getAbsolutePath());
              SevenZFile zFile = new SevenZFile(sourceArchive);
              SevenZArchiveEntry zEntry = zFile.getNextEntry();
              while ((zEntry = zFile.getNextEntry()) != null) {
                if (!zEntry.isDirectory()) {
                  log(zEntry.getName());

                  File newFile = new File(destinationMod.getAbsolutePath(), zEntry.getName());
                  File parentDirectory = newFile.getParentFile();
                  if (!parentDirectory.exists()) {
                    parentDirectory.mkdirs();
                  }

                  FileOutputStream stream = new FileOutputStream(newFile);
                  byte[] bytes = new byte[(int) zEntry.getSize()];
                  zFile.read(bytes, 0, bytes.length);
                  stream.write(bytes);
                  stream.close();
                }
              }
              zFile.close();
              variantHistoryList.add(selectedVariant);
            }
          }
        }
      }

      log(selectedVariant);

      PROPERTIES.put(PROPERTIES_HISTORY_NAME, String.join(",", variantHistoryList));
      PROPERTIES.store(new FileOutputStream(PROPERTIES_FILE_NAME), null);

    } catch (Exception exception) {
      JOptionPane.showMessageDialog(null, exception.getMessage(), exception.getClass().toString(), JOptionPane.ERROR_MESSAGE);
      throw exception;
    }
  }

  private static File rename(File file) {
    String extension = FilenameUtils.getExtension(file.getName());
    File newFile = new File(file.getParent() + '\\' + System.currentTimeMillis() + (file.isFile() ? "." + extension : ""));
    log("Rename", file.getAbsolutePath(), newFile.getAbsolutePath());
    file.renameTo(newFile);
    return newFile;
  }

  private static List<File> shuffle(File parent) {
    List<File> fileList = new ArrayList<File>();
    for (File file : parent.listFiles()) {
      String fileName = file.getName();
      fileName = file.isFile() ? fileName.substring(0, fileName.indexOf('.')) : fileName;
      try {
        Long.parseLong(fileName);
        if (fileName.length() != String.valueOf(System.currentTimeMillis()).length()) {

          fileList.add(file);
        }
      } catch (NumberFormatException exception) {
        fileList.add(file);
      }
    }
    fileList = fileList.isEmpty() ? Arrays.asList(parent.listFiles()) : fileList;
    Collections.shuffle(fileList);
    return fileList;
  }

  private static void log(String... logs) {
    for (String log : logs) {
      System.out.println(new Date() + " " + log);
    }
  }

}
