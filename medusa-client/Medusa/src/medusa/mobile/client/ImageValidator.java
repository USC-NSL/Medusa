package medusa.mobile.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageValidator {
	
	private static Pattern pattern;
	private static Matcher matcher;
	
	private static final String IMAGE_PATTERN = "([^\\s]+(\\.(?i)(jpg|png|gif|bmp|jpeg))$)";
	
	public ImageValidator()
	{
		pattern = Pattern.compile(IMAGE_PATTERN);
	}
	
	
	public static boolean validate(String image)
	{
		matcher = pattern.matcher(image);
		return matcher.matches();
	}

}
