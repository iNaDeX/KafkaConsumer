import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

public class Context {
    private static final Logger LOG = LoggerFactory.getLogger(Context.class);

    private Properties properties;

    Context(String configFileLocation) throws Exception {
        properties = new Properties();
        try {
            InputStream is = new FileInputStream(configFileLocation);
            properties.load(is);
        } catch (FileNotFoundException e) {
            LOG.error("Error : unable to find configuration file: "
                    + e.getMessage());
            throw e;
        } catch (SecurityException e) {
            LOG.error("Error: unauthorized to access configuration file: "
                    + e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Error: unable to access configuration file: "
                    + e.getMessage());
            throw e;
        }
    }

    public String getString(String key) {
        return properties.getProperty(key);
    }
}