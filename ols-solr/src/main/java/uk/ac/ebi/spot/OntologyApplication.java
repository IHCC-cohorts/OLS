package uk.ac.ebi.spot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.ac.ebi.spot.indexer.SolrIndexer;
import uk.ac.ebi.spot.config.PropertyBasedLoadingService;
import uk.ac.ebi.spot.loader.OntologyLoader;
import uk.ac.ebi.spot.model.TermDocument;

/**
 * @author Simon Jupp
 * @date 30/01/2015
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@SpringBootApplication
public class OntologyApplication implements CommandLineRunner {

    @Autowired
    SolrIndexer solrIndexingService;

    @Autowired
    PropertyBasedLoadingService propertyBasedLoadingService;

    @Override
   	public void run(String... args) throws Exception {

        System.setProperty("entityExpansionLimit", "10000000");
//        DocumentLoadingService loadingService = new PropertyBasedLoadingService();
        OntologyLoader loader = propertyBasedLoadingService.getLoader();
        for (TermDocument td :solrIndexingService.getOntologySolrRepository().findByOntologyName("EFO")) {
            System.out.println(td.getUri());

        }
//        if (args != null) {
//            if (args[0].equals("dropall")) {
//                solrIndexingService.dropIndex(loader);
//            }
//        }
//        solrIndexingService.createIndex(loader);
   	}

   	public static void main(String[] args) throws Exception {
        SpringApplication.run(OntologyApplication.class, args);
   	}

}
