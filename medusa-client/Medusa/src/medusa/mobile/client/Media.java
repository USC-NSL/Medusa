package medusa.mobile.client;

import java.io.File;

public class Media 
{
		File file;
		private String type;
		
		public Media(File file, String type) 
		{
			this.file = file;
			this.type = type;
		}
		
		
		public String getType() 
		{
			return type;
		}
		
		
		public File getFile() 
		{
			return file;
		}
}
