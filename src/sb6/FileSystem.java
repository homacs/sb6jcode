package sb6;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileSystem {

	public static String readTextFile(File file) throws IOException {
		  String text = "",line;
		  BufferedReader reader = null;
		  try {
			  reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			  while ((line = reader.readLine()) != null) {
			      text += line + "\n";
			  }
		  } finally {
			  if (reader != null) reader.close();
		  }
		  return text;
	}
	

}
