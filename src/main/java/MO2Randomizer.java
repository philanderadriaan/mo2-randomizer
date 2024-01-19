import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.apache.commons.lang3.StringUtils;

public class MO2Randomizer {
  private static final String PROPERTIES_FILE_NAME = "mo2-randomizer.properties";
  private static final String PROPERTIES_HISTORY_NAME = "history";
  private static final String RANDOM_PREFIX = "RANDOM ";

  private static final Properties PROPERTIES = new Properties();

  private static String selectedVariant = null;
  private static String destinationModName;

  public static void main(String[] args) throws Exception {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      PROPERTIES.load(new FileInputStream(PROPERTIES_FILE_NAME));

      List<String> variantHistoryList = new ArrayList<String>(Arrays.asList(PROPERTIES.getProperty(PROPERTIES_HISTORY_NAME).split(",")));

      for (File sourceGroup : new File(PROPERTIES.getProperty("source.directory")).listFiles()) {
        File sourceMod = rename(shuffle(sourceGroup).get(0), null);

        for (File sourcePart : sourceMod.listFiles()) {
          List<File> sourceArchiveList = shuffle(sourcePart);
          destinationModName = RANDOM_PREFIX + sourceGroup.getName() + sourcePart.getName();
          File destinationMod = new File(PROPERTIES.getProperty("destination.directory") + "\\" + destinationModName);
          if (sourceArchiveList.size() == 1) {
            File sourceArchive = sourceArchiveList.get(0);
            extract(sourceArchive, destinationMod);
          } else {
            if (sourceArchiveList.size() <= variantHistoryList.size()) {
              variantHistoryList.clear();
            }
            for (File sourceArchive : sourceArchiveList) {
              // String sourceArchiveBaseName =
              // FilenameUtils.getBaseName(sourceArchive.getName());
              String[] substring = PROPERTIES.getProperty("substring").split(",");
              String currentVariant = StringUtils.substringBetween(sourceArchive.getName(), substring[0], substring[1]);

              if (selectedVariant == null && !variantHistoryList.contains(currentVariant)) {
                selectedVariant = currentVariant;
                variantHistoryList.add(selectedVariant);
              }

              if (currentVariant.equals(selectedVariant)) {
                extract(sourceArchive, destinationMod);
              }
            }
          }
        }
      }

      PROPERTIES.put(PROPERTIES_HISTORY_NAME, String.join(",", variantHistoryList));
      PROPERTIES.store(new FileOutputStream(PROPERTIES_FILE_NAME), null);

      log(selectedVariant);
      log(variantHistoryList.toString());

    } catch (Exception exception) {
      JOptionPane.showMessageDialog(null, exception.getMessage(), exception.getClass().toString(), JOptionPane.ERROR_MESSAGE);
      throw exception;
    }
  }

  private static void extract(File sourceArchive, File destinationDirectory) throws IOException {
    log("Delete");
    for (File destinationFile : destinationDirectory.listFiles()) {
      if (!destinationFile.getName().equalsIgnoreCase("meta.ini")) {
        log(destinationFile.getAbsolutePath());
        if (destinationFile.isDirectory()) {
          FileUtils.deleteDirectory(destinationFile);
        } else {
          destinationFile.delete();
        }
      }
    }

    log("Extract", sourceArchive.getAbsolutePath(), destinationDirectory.getAbsolutePath());
    SevenZFile zFile = new SevenZFile(sourceArchive);
    SevenZArchiveEntry zEntry;
    while ((zEntry = zFile.getNextEntry()) != null) {
      if (!zEntry.isDirectory()) {
        log(zEntry.getName());

        File extractedFile = new File(destinationDirectory.getAbsolutePath(), zEntry.getName());
        File parentDirectory = extractedFile.getParentFile();
        if (!parentDirectory.exists()) {
          parentDirectory.mkdirs();
        }

        FileOutputStream stream = new FileOutputStream(extractedFile);
        byte[] bytes = new byte[(int) zEntry.getSize()];
        zFile.read(bytes, 0, bytes.length);
        stream.write(bytes);
        stream.close();

        if (FilenameUtils.getExtension(extractedFile.getName()).equals("esp")) {
          extractedFile = rename(extractedFile, destinationModName);
        }
      }
    }
    zFile.close();
  }

  private static File rename(File file, String newName) {
    String extension = FilenameUtils.getExtension(file.getName());
    File newFile = new File(file.getParent() + '\\' + (newName == null ? System.currentTimeMillis() : newName) + (file.isFile() ? "." + extension : ""));
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
