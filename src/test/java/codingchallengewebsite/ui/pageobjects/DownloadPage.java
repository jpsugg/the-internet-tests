package codingchallengewebsite.ui.pageobjects;

import static codingchallengewebsite.ui.UITest.*;
import codingchallengewebsite.ui.UITest;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class DownloadPage {

    @FindBy(how = How.XPATH, using = "//h3[normalize-space()='File Downloader']")
    private WebElement pageTitle;
    @FindBy(how = How.XPATH, using = "//a[normalize-space()='some-file.txt']")
    private WebElement downloadLink;
    private final UITest caller;
    private final String pageUrl;
    private final Path relativePathToReferenceFile = Paths.get("src/test/resources", "some-file.txt");

    public DownloadPage(RemoteWebDriver driver, UITest caller) {
        this.caller = caller;
        this.caller.setDriver(driver);
        this.pageUrl = this.caller.getBaseUrl() + "/download";
        this.caller.getDriver().get(this.pageUrl);
        PageFactory.initElements(driver, this);
    }

    public boolean isPageOpen() {
        return caller.getDriver().getCurrentUrl().equals(this.pageUrl) && this.pageTitle.getText().contains("File Downloader");
    }

    public boolean fileDownload() {
        String downloadHref = downloadLink.getAttribute("href");
        String downloadFileName = Paths.get(downloadHref).getFileName().toString();
        String tempFileName = downloadFileName.split("\\.")[0] + ".crdownload";
        Path pathToReferenceFile = Paths.get(relativePathToReferenceFile.toString());
        Path tempDownloadedFilePath = Paths.get(downloadsFolder, tempFileName);
        Path expectedDownloadedFilePath = Paths.get(downloadsFolder, downloadFileName);
        File expectedFile = new File(expectedDownloadedFilePath.toAbsolutePath().toString());
        File expectedTmpFile = new File(tempDownloadedFilePath.toAbsolutePath().toString());
        long bytes=-1, newBytes=0;

        // Case: There's no reference file to compare the file against to
        if (!pathToReferenceFile.toAbsolutePath().toFile().exists()) { return false; }

        // Case: The file to be downloaded or a .crdownload from it already exist in the download folder - delete it
        try { Files.deleteIfExists(expectedFile.toPath()); Files.deleteIfExists(expectedTmpFile.toPath()); }
        catch (IOException e) { }
        // Start downloading
        downloadLink.click();
        //WebDriverWait wait = new WebDriverWait(caller.getDriver(), Duration.ofSeconds(10));
        //wait.until(d -> (expectedFile.exists() || expectedTmpFile.exists()));

        // Give it some buffer time...
        try { Thread.sleep(5000); } catch (InterruptedException e) { throw new RuntimeException(e); }

        // Case: The file is still downloading - keeping track of it's size until the download finishes
        while ((bytes != newBytes) && expectedTmpFile.exists() && !expectedFile.exists()) {
            try { newBytes = Files.size(tempDownloadedFilePath); } catch (IOException e) { }
            bytes = newBytes;
            // Check again in 1 sec...
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
        }

        // Case: The file arrived
        if (expectedFile.exists()) {
            try {
                return compareByMemoryMappedFiles(expectedDownloadedFilePath.toAbsolutePath(), pathToReferenceFile.toAbsolutePath());
            } catch (IOException e) { throw new RuntimeException(e); }
        }

        // None of the above - download failed
        return false;
    }

    public static boolean compareByMemoryMappedFiles(Path path1, Path path2) throws IOException {
        try (RandomAccessFile randomAccessFile1 = new RandomAccessFile(path1.toFile(), "r");
             RandomAccessFile randomAccessFile2 = new RandomAccessFile(path2.toFile(), "r")) {

            FileChannel ch1 = randomAccessFile1.getChannel();
            FileChannel ch2 = randomAccessFile2.getChannel();
            if (ch1.size() != ch2.size()) {
                return false;
            }
            long size = ch1.size();
            MappedByteBuffer m1 = ch1.map(FileChannel.MapMode.READ_ONLY, 0L, size);
            MappedByteBuffer m2 = ch2.map(FileChannel.MapMode.READ_ONLY, 0L, size);

            return m1.equals(m2);
        }
    }

}