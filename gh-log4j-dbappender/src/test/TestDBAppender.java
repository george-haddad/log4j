package test;

import java.io.File;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * 
 * @author Georges El-Haddad - george.dma@gmail.com
 */
public class TestDBAppender {

	private static Logger logger = null;
	
	public TestDBAppender() {
		
		for(int i=0; i < 1; i++) {
			logger.debug("Foo");
			logger.debug("Bar");
			
			try {
				Thread.sleep(2000);
			}
			catch(InterruptedException exception) {
				exception.printStackTrace();
			}
		}
		
		logger.removeAllAppenders();
	}
	
	public static void main(String ... args) {
	        File configfile = new File(System.getProperty("user.dir") + File.separator + "src" + File.separator + "test" + File.separator + "log4j.properties");
		PropertyConfigurator.configure(configfile.getAbsolutePath());
		logger = Logger.getLogger("test");
		
		new TestDBAppender();
	}
}
