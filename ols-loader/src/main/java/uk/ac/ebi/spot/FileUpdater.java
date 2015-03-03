package uk.ac.ebi.spot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

/**
 * @author Simon Jupp
 * @date 11/02/2015
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public class FileUpdater {


    private Logger log = LoggerFactory.getLogger(getClass());

    public Logger getLog() {
        return log;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Value("${ols.loader.filedir}")
    private String path;

    public FileUpdater() {
    }

    public FileStatus getFile(String name, URI file) throws FileUpdateServiceException{

        getLog().info("Downloading " + name + " from " + file.toString());
        File pathFile = new File(path);
        if (!pathFile.exists()) {
                if (pathFile.mkdir()) {

                }
                else {
                    throw new FileUpdateServiceException("Can't create tmp directory at " + path);
                }
        }

        String checkName = name + ".chk";
        File latestFileChecksum  = new File(pathFile, checkName);

        String downloadFileName = name + ".download";

        File latestFile  = new File(pathFile, name);

        // get file as input stream (check if zipped)
        try {

            InputStream is = getFileInputStream(file.toURL());

            Instant start = Instant.now();
            File downloadFile = writeInputStreamToFile(new File(pathFile, downloadFileName), is);
            Instant end = Instant.now();
            getLog().info(name + " downloaded in " + Duration.between(start, end));

            start = Instant.now();
            String downloadChecksum = ChecksumSHA1.getSHA1Checksum(downloadFile);
            end = Instant.now();
            getLog().info(downloadChecksum+ " " + name + " created in " + Duration.between(start, end));

            // compare new file to latest
            // if previous checksum exists
            if (latestFileChecksum.exists()) {

                getLog().debug("Previous version exists " + name );
                // read it
                String latestChecksum = readChecksum(latestFileChecksum);
                getLog().debug("Reading previous checksum " + name + ":" + latestChecksum);

                // create one for the downloaded file

                // if they are the same, the file hasn't changed
                if (latestChecksum.equals(downloadChecksum)) {
                    return new FileStatus(latestFile, false);
                }
                else {
                    // if they are different, copy the downloaded file to the latest file
                    FileCopyUtils.copy(downloadFile, latestFile );
                    // update the latest file checksum
                    writeChecksum(latestFileChecksum, downloadChecksum);
                    return new FileStatus(latestFile, true);
                }
            }
            else {
                FileCopyUtils.copy(downloadFile, latestFile );
                writeChecksum(latestFileChecksum, downloadChecksum);
                return new FileStatus(latestFile, true);
            }

        } catch (Exception e) {
            throw new FileUpdateServiceException("Failed to check " + name + " for updates from" + file.toString(), e);
        }
    }

    private File writeInputStreamToFile(File file, InputStream is) throws IOException {
        OutputStream os = new FileOutputStream(file);
        byte[] b = new byte[2048];
        int length;

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }

        is.close();
        os.close();
        return file;
    }

    private InputStream getFileInputStream(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        return connection.getInputStream() ;
//        if ("application/zip".equals(connection.getContentType())) {
//            final int BUFFER = 2048;
//            int count = 0;
//            byte data[] = new byte[BUFFER];
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            ZipInputStream zis = new ZipInputStream(connection.getInputStream());
//            while ((count = zis.read(data, 0, BUFFER)) != -1) {
//                out.write(data);
//            }
//            is = new ByteArrayInputStream(out.toByteArray());
//
//        }
//        else if ("application/gzip".equals(connection.getContentType())) {
//            is = new GZIPInputStream(connection.getInputStream());
//
//        }
//        else {
//            is = connection.getInputStream();
//        }
//        return is;
    }

    private String readChecksum (File fileCheck) throws IOException {
        InputStream is = new FileInputStream(fileCheck);
        byte [] content = Files.readAllBytes(fileCheck.toPath());
        return new String(content, "UTF-8");
    }

    private void writeChecksum (File fileCheck, String checksum) throws IOException {
        FileOutputStream out = new FileOutputStream(fileCheck);
        out.write(checksum.getBytes());
        out.close();
    }

    public class FileStatus {
        private File file;
        private boolean isNew;


        public FileStatus(File latestFile, boolean b) {
            this.file = latestFile;
            this.isNew = b;
        }

        public File getFile() {
            return file;
        }

        public boolean isNew() {
            return isNew;
        }
    }


}
