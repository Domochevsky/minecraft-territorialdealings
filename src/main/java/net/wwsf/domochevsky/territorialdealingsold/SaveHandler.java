package net.wwsf.domochevsky.territorialdealingsold;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class SaveHandler 
{
	public static void saveFactionsToFile()
	{
		// Saving to file. File name is world name (dimension IDs are already sorted out)

		String path = Main.getConfigDirPath() + "/territorialdealings_" + Main.getWorldFolder() + ".dat";
		File configFile = new File(path);

		try
		{
			FileOutputStream saveFile = new FileOutputStream(configFile);			// Saving to the current profile
			ObjectOutputStream saveHandle = new ObjectOutputStream(saveFile);		// Creating the file handle

			saveHandle.writeObject(TerritoryHandler.getFactionsForSaving());	// Saving the whole shebang

			saveHandle.writeInt(EventListener.dayTick);
			saveHandle.writeInt(EventListener.saveTick);
			saveHandle.writeInt(EventListener.upkeepTick);

			saveHandle.close();	// Done saving all factions
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		finally { Main.console("[TERRITORIAL DEALINGS] Saved all factions and territories for world " + Main.getWorldFolder() + " to disk."); }
	}


	public static void loadFactionsFromFile()
	{
		ObjectInputStream loadHandle = null;

		try
		{
			String path = Main.getConfigDirPath() + "/territorialdealings_" + Main.getWorldFolder() + ".dat";

			if(new File(path).isFile())	// Exists
			{
				FileInputStream loadFile = new FileInputStream(path); 	// Loading the passed in profile
				loadHandle = new ObjectInputStream(loadFile);			// Creating the file handle

				Object loadedObj = loadHandle.readObject();

				if (loadedObj instanceof _Territory[])	// Legacy load
				{
					TerritoryHandler.setFactionsFromLoadingOld((_Territory[]) loadHandle.readObject());
				}
				else if (loadedObj instanceof ArrayList<?>)	// Regular
				{
					TerritoryHandler.setFactionsFromLoading((ArrayList<_Territory>) loadedObj);
				}

				EventListener.dayTick = loadHandle.readInt();
				EventListener.saveTick = loadHandle.readInt();
				EventListener.upkeepTick = loadHandle.readInt();

				loadHandle.close();
			}
			else
			{
				// Doesn't exist. Now what? Should be fine to load no factions on first start
			}
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (EOFException e) {  }	// That's fine. Doing nothing.
		catch (IOException e) { e.printStackTrace(); }
		catch (ClassNotFoundException e) { e.printStackTrace(); }
		finally
		{
			Main.console("[TERRITORIAL DEALINGS] Loaded all factions and territories for world " + Main.getWorldFolder() + " from disk.");
		}
	}
}
