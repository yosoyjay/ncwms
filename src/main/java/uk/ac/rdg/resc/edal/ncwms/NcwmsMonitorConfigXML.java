package uk.ac.rdg.resc.edal.ncwms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig;

import javax.xml.bind.JAXBException;

/**
 * Created by Jesse Lopez on 4/13/17.
 */
public class NcwmsMonitorConfigXML extends TimerTask {
    private long timeStamp;
    private File file;
    private NcwmsCatalogue currentCatalogue;
    private NcwmsConfig newConfig;
    private static final Logger log = LoggerFactory.getLogger(NcwmsMonitorConfigXML.class);


    public NcwmsMonitorConfigXML(File file, NcwmsCatalogue catalogue) {
        this.file = file;
        this.timeStamp = file.lastModified();
        this.currentCatalogue = catalogue;
    }

    public final void run() {
        long timeStamp = file.lastModified();

        if (this.timeStamp != timeStamp) {
            this.timeStamp = timeStamp;
            updateConfig();
        }
    }

    /**
     * Updates datasets derived from NcwmsAdminServlet.java
     */
    private void updateConfig() {
        // Read the new newConfig.xml file
        try {
            newConfig = NcwmsConfig.readFromFile(file);
        } catch (JAXBException e) {
            log.error("Config file is invalid - using the old newConfig", e);
            newConfig = currentCatalogue.getConfig();
            return;
        } catch (FileNotFoundException e) {
            log.error( "Cannot find newConfig file - using old newConfig ", e);
            newConfig = currentCatalogue.getConfig();
            return;
        } catch (IOException e) {
            log.error("Problem writing new newConfig file - using old newConfig", e);
            newConfig = currentCatalogue.getConfig();
            return;
        }

        // Compare current and new catalogues and make appropriate adjustments based on datasetIds
        ArrayList<String> newDatasets = new ArrayList<>();
        for (DatasetConfig ds : newConfig.getDatasets()) {
            newDatasets.add(ds.getId());
        }
        ArrayList<String> currentDatasets = new ArrayList<>();
        for (DatasetConfig ds : currentCatalogue.getConfig().getDatasets()){
            currentDatasets.add(ds.getId());
        }

        for (DatasetConfig ds : newConfig.getDatasets()) {
            if (!currentDatasets.contains(ds.getId())) {
                currentCatalogue.getConfig().addDataset(ds);
            }
        }
        for (DatasetConfig ds : currentCatalogue.getConfig().getDatasets()) {
            if (!newDatasets.contains(ds.getId())) {
                currentCatalogue.removeDataset(ds.getId());
            }
        }
        log.info(new Date() + " config.xml updated");
    }
}
