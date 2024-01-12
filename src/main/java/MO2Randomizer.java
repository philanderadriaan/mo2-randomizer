import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.swing.UIManager;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.io.FileUtils;

public class MO2Randomizer
{

  private static final Properties PROPERTIES = new Properties();

  private static final String RANDOM = "RANDOM";

  private static String selectedVariant = null;

  public static void main(String[] args) throws Exception
  {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    PROPERTIES.load(new FileInputStream("mo2-randomizer.properties"));

    List<File> sourceModList = new ArrayList<File>();
    List<File> destinationModList = new ArrayList<File>();

    for (File sourceMod : new File(PROPERTIES.getProperty("source.directory")).listFiles())
    {
      sourceModList.add(sourceMod);
    }

    log("Delete");
    for (File destinationMod : new File(PROPERTIES.getProperty("destination.directory")).listFiles())
    {
      if (Arrays.asList(splitTrim(destinationMod.getName())).contains(RANDOM))
      {
        destinationModList.add(destinationMod);
        for (File destinationFile : destinationMod.listFiles())
        {
          if (!destinationFile.getName().equalsIgnoreCase("meta.ini"))
          {
            log(destinationFile.getAbsolutePath());
            FileUtils.deleteDirectory(destinationFile);
          }
        }
      }
    }

    Collections.shuffle(sourceModList);
    Collections.shuffle(destinationModList);

    for (File destinationMod : destinationModList)
    {
      String destinationModName = destinationMod.getName();
      int randomIndex = destinationModName.indexOf(RANDOM);
      File selectedMod = null;

      for (File sourceMod : sourceModList)
      {
        String sourceModName = sourceMod.getName();
        if (sourceModName.contains(destinationModName.substring(0, randomIndex)))
        {
          String variant = splitTrim(sourceModName.substring(randomIndex))[0];
          selectedVariant = selectedVariant == null ? variant : selectedVariant;
          if (variant == selectedVariant)
          {
            selectedMod = sourceMod;
            break;
          }
        }
      }

      if (selectedMod != null)
      {
        log("Extract", selectedMod.getAbsolutePath());
        SevenZFile zFile = new SevenZFile(selectedMod);
        SevenZArchiveEntry zEntry = zFile.getNextEntry();
        while ((zEntry = zFile.getNextEntry()) != null)
        {
          if (!zEntry.isDirectory())
          {
            log(zEntry.getName());

            File newFile = new File(destinationMod.getAbsolutePath(), zEntry.getName());
            File parentDirectory = newFile.getParentFile();
            if (!parentDirectory.exists())
            {
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
      }
    }
    log(selectedVariant);
  }

  private static String[] splitTrim(String s)
  {
    return Arrays.stream(s.split("-")).map(String::trim).toArray(String[]::new);
  }

  private static void log(String... logs)
  {
    for (String log : logs)
    {
      System.out.println(new Date() + " " + log);
    }
  }

}
