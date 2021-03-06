package fr.apiscol.content.fileSystemAccess;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fr.apiscol.content.languageDetection.ITextExtrator;
import fr.apiscol.content.languageDetection.TikaTextExtractor;
import fr.apiscol.content.mimeDetection.IMimeTypeDetector;
import fr.apiscol.content.mimeDetection.TikaMimeDetector;
import fr.apiscol.utils.FileUtils;
import fr.apiscol.utils.LogUtility;

public class ResourceDirectoryInterface {

	private static final float PREVIEWS_IMAGE_MAX_HEIGHT_PROPORTION = 0.75f;

	private static Logger logger = null;

	private static String fileRepoPath;
	private static String temporaryFilesPrefix;
	private static IMimeTypeDetector mimeTypeDetector;
	private static ITextExtrator textExtractor;

	public static ArrayList<String> getFileNamesList(String resourceId)
			throws ResourceDirectoryNotFoundException {
		return getFileNamesList(resourceId, true);
	}

	public static ArrayList<String> getFileNamesList(String resourceId,
			boolean filterIgnoredFiles)
			throws ResourceDirectoryNotFoundException {
		ArrayList<String> list = new ArrayList<String>();
		File resourceDirectory = getResourceDirectory(resourceId);
		ArrayList<File> fileList = getFileList(resourceDirectory,
				filterIgnoredFiles);
		try {
			for (File file : fileList) {
				list.add(resourceDirectory.toURI().relativize(file.toURI())
						.getPath());
			}
		} catch (NullPointerException e) {
			throw new ResourceDirectoryNotFoundException(resourceId);
		}
		return list;
	}

	public static boolean isIgnoredFile(String fileName) {
		return fileName.contains(temporaryFilesPrefix);
	}

	public static ArrayList<File> getFileList(File resourceDirectory,
			boolean filterIgnoredFiles)
			throws ResourceDirectoryNotFoundException {
		ArrayList<File> list = new ArrayList<File>();
		for (File child : resourceDirectory.listFiles()) {
			if (".".equals(child.getName()) || "..".equals(child.getName())
					|| (filterIgnoredFiles && isIgnoredFile(child.getName()))) {
				continue;
			}
			if (child.isDirectory()) {
				list.addAll(getFileList(child, filterIgnoredFiles));
			} else
				list.add(child);
		}

		return list;
	}

	public static ArrayList<File> getFileListInSnapshot(String resourceId,
			String snapshotId)
			throws ResourceSnapshotDirectoryNotFoundException {
		ArrayList<File> list = new ArrayList<File>();
		File resourceDirectory = null;
		resourceDirectory = getResourceSnapshotDirectory(resourceId, snapshotId);
		for (File child : resourceDirectory.listFiles()) {
			if (".".equals(child.getName()) || "..".equals(child.getName())) {
				continue;
			}
			list.add(child);
		}

		return list;
	}

	private static File getResourceDirectory(String resourceId)
			throws ResourceDirectoryNotFoundException {
		File file = new File(FileUtils.getFilePathHierarchy(fileRepoPath,
				resourceId.toString()));
		if (!file.exists() || !file.isDirectory()) {
			logger.error("Directory does not exist : " + file.getAbsolutePath());
			throw new ResourceDirectoryNotFoundException(resourceId);
		}
		return file;
	}

	private static File getResourceSnapshotDirectory(String resourceId,
			String snapshotId)
			throws ResourceSnapshotDirectoryNotFoundException {
		File file = new File(getSnapshotDirectoryPath(resourceId, snapshotId));
		if (!file.exists() || !file.isDirectory()) {
			logger.error("Snapshot directory does not exist : "
					+ file.getAbsolutePath());
			throw new ResourceSnapshotDirectoryNotFoundException(resourceId);
		}
		return file;
	}

	public static boolean existsResourceDirectory(String resourceId) {
		File file = new File(FileUtils.getFilePathHierarchy(fileRepoPath,
				resourceId.toString()));
		return file.exists() && file.isDirectory();
	}

	public static void initialize(String fileRepoPath,
			String temporaryFilesPrefix) {
		ResourceDirectoryInterface.fileRepoPath = fileRepoPath;
		ResourceDirectoryInterface.temporaryFilesPrefix = temporaryFilesPrefix;
		initializeLogger();
		logger.info("The resource directory is " + fileRepoPath);
		mimeTypeDetector = new TikaMimeDetector();
		textExtractor = new TikaTextExtractor();
	}

	private static void initializeLogger() {
		if (logger == null) {
			logger = LogUtility.createLogger(ResourceDirectoryInterface.class
					.getCanonicalName());
		}

	}

	public static boolean isInitialized() {
		return fileRepoPath != null && !fileRepoPath.isEmpty();
	}

	public static ArrayList<String> getResourcesList() throws Exception {
		ArrayList<String> list = new ArrayList<String>();
		File dir = new File(fileRepoPath);
		if (!dir.exists())
			throw new Exception(
					"Please create static content directory specified by service.properties :"
							+ fileRepoPath);
		for (File child1 : dir.listFiles()) {
			if (".".equals(child1.getName()) || "..".equals(child1.getName())
					|| child1.isFile()) {
				continue;
			}
			for (File child2 : child1.listFiles()) {
				if (".".equals(child2.getName())
						|| "..".equals(child2.getName()) || child2.isFile()) {
					continue;
				}
				for (File child3 : child2.listFiles()) {
					if (".".equals(child3.getName())
							|| "..".equals(child3.getName()) || child3.isFile()) {
						continue;
					}
					for (File child4 : child3.listFiles()) {
						if (".".equals(child4.getName())
								|| "..".equals(child4.getName())
								|| child4.isFile()
								|| isIgnoredFile(child4.getName())) {
							continue;
						}
						String fileName = (new StringBuilder()
								.append(child1.getName())
								.append(child2.getName())
								.append(child3.getName()).append(child4
								.getName())).toString();
						list.add(fileName);
					}
				}
			}
		}

		return list;
	}

	public static boolean deleteFile(String resourceId, String fileName)
			throws ResourceDirectoryNotFoundException {
		// workaround for windows
		System.gc();
		File resourceDirectory = getResourceDirectory(resourceId.toString());
		File file = new File(resourceDirectory.getAbsolutePath() + "/"
				+ fileName);
		if (!file.exists() || !file.isFile())
			return false;
		return file.delete();
	}

	public static Boolean existsFile(String resourceId, String fileName)
			throws ResourceDirectoryNotFoundException {
		File resourceDirectory = getResourceDirectory(resourceId.toString());
		File file = new File(resourceDirectory.getAbsolutePath() + "/"
				+ fileName);
		return file.exists() && file.isFile();
	}

	public static void createDirectory(UUID resourceId)
			throws FileSystemAccessException {
		File file = new File(FileUtils.getFilePathHierarchy(fileRepoPath,
				resourceId.toString()));

		if (!file.exists()) {
			boolean succes = file.mkdirs();
			if (succes) {
				logger.info(String.format(
						"The directory %s has been created on the disk",
						file.getAbsolutePath()));
				file.setReadable(true, false);
				file.setWritable(true, false);
			}

			else {
				String message = "Failed to create directory "
						+ file.getAbsolutePath();
				logger.error(message);
				throw new FileSystemAccessException(message);
			}

		} else {
			logger.error(String
					.format("Very strange, the directory %s cannot be created because it exists",
							file.getAbsolutePath()));
		}
	}

	public static String getFilePath(String resourceId, String fileName) {
		return new StringBuilder()
				.append(FileUtils.getFilePathHierarchy(fileRepoPath,
						resourceId.toString())).append("/").append(fileName)
				.toString();
	}

	public static boolean deleteResourceDirectory(String resourceId)
			throws ResourceDirectoryNotFoundException {
		// workaround for windows
		System.gc();
		boolean success = true;
		File resourceDirectory = getResourceDirectory(resourceId.toString());
		File parent = resourceDirectory.getParentFile();
		File grandParent = parent.getParentFile();
		File grandgrandParent = grandParent.getParentFile();

		success &= FileUtils.deleteDir(resourceDirectory);

		if (success && parent.list().length == 0) {
			success &= FileUtils.deleteDir(parent);
			if (success && grandParent.list().length == 0) {
				success &= FileUtils.deleteDir(grandParent);
				if (success && grandgrandParent.list().length == 0) {
					success &= FileUtils.deleteDir(grandgrandParent);
				}
			}
		}
		return success;
	}

	public static boolean resourceHasFiles(String resourceId)
			throws ResourceDirectoryNotFoundException {
		return getResourceDirectory(resourceId) != null
				&& !getFileNamesList(resourceId).isEmpty();
	}

	public static void registerFile(String resourceId, String fileName)
			throws MissingIncomingFileException,
			OverWritingExistingFileException,
			ResourceDirectoryNotFoundException {
		File resourceDirectory = getResourceDirectory(resourceId);
		File providedFile = new File(String.format("%s/%s%s",
				resourceDirectory.getAbsolutePath(), temporaryFilesPrefix,
				fileName));
		if (!providedFile.exists())
			throw new MissingIncomingFileException(resourceId, fileName);
		File newFile = new File(String.format("%s/%s",
				resourceDirectory.getAbsolutePath(), fileName));
		if (newFile.exists())
			throw new OverWritingExistingFileException(resourceId, fileName);
		providedFile.renameTo(newFile);

	}

	public static void registerArchive(String resourceId, String archiveName)
			throws ResourceDirectoryNotFoundException,
			MissingIncomingFileException, FileSystemAccessException,
			BadFileTypeException {
		File resourceDirectory = getResourceDirectory(resourceId);
		File providedFile = new File(String.format("%s/%s%s",
				resourceDirectory.getAbsolutePath(), temporaryFilesPrefix,
				archiveName));
		if (!providedFile.exists())
			throw new MissingIncomingFileException(resourceId, archiveName);
		try {
			if (!FileUtils.isZip(providedFile))
				throw new BadFileTypeException(resourceId, archiveName);
		} catch (IOException e1) {
			throw new MissingIncomingFileException(resourceId, archiveName);
		}
		try {
			FileUtils.unzipFile(providedFile);
		} catch (IOException e) {
			throw new FileSystemAccessException(String.format(
					"Unable to decompress the archive %s",
					providedFile.getAbsolutePath()));
		}
		providedFile.delete();
	}

	public static void deleteTemporaryFile(String resourceId, String fileName)
			throws ResourceDirectoryNotFoundException {
		// workaround for windows
		System.gc();
		File resourceDirectory = getResourceDirectory(resourceId);
		File providedFile = new File(String.format("%s/%s%s",
				resourceDirectory.getAbsolutePath(), temporaryFilesPrefix,
				fileName));
		boolean success = providedFile.delete();
		if (success)
			logger.warn(String
					.format("Impossible to handle temporary file %s for resource %s, it has been deleted",
							providedFile.getAbsolutePath(), resourceId));
		else
			logger.warn(String
					.format("Impossible to handle temporary file %s for resource %s, be we failed to delete it",
							providedFile.getAbsolutePath(), resourceId));

	}

	public static void createSnapShot(String resourceId, String snapshotId,
			Document manifest) throws IOException,
			ResourceDirectoryNotFoundException {
		File resourceDirectory = getResourceDirectory(resourceId);
		ArrayList<File> filesToCopy = getFileList(resourceDirectory, true);
		File snapShotDirectory = new File(getSnapshotDirectoryPath(resourceId,
				snapshotId));
		snapShotDirectory.mkdir();
		Iterator<File> it = filesToCopy.iterator();
		File copy, original;
		while (it.hasNext()) {
			original = it.next();
			copy = new File(String.format("%s/%s", snapShotDirectory
					.getAbsolutePath(),
					resourceDirectory.toURI().relativize(original.toURI())));
			copy.getParentFile().mkdirs();
			FileUtils.copyFile(original, copy);
		}
		File manifestFile = new File(String.format("%s/imsmanifest.xml",
				snapShotDirectory.getAbsolutePath()));
		try {
			FileUtils.writeXmlFile(manifest, manifestFile);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	private static String getSnapshotDirectoryPath(String resourceId,
			String snapshotId) {
		return String.format("/%s%s",
				FileUtils.getFilePathHierarchy(fileRepoPath, resourceId),
				temporaryFilesPrefix, snapshotId);
	}

	public static void zipSnapshot(String resourceId, String snapshotId)
			throws IOException, ResourceSnapshotDirectoryNotFoundException {
		File resourceSnapshotDirectory = getResourceSnapshotDirectory(
				resourceId, snapshotId);
		FileUtils.zipFiles(
				new File(getTemporaryZipPath(resourceId, snapshotId)),
				resourceSnapshotDirectory);

	}

	private static String getTemporaryZipPath(String resourceId,
			String snapshotId) {
		return new StringBuilder().append(fileRepoPath).append("/")
				.append(temporaryFilesPrefix).append(snapshotId).append("-")
				.append(resourceId).append(".zip").toString();
	}

	private static String getZipPath(String resourceId) {
		return new StringBuilder()
				.append(FileUtils
						.getFilePathHierarchy(fileRepoPath, resourceId))
				.append(".zip").toString();
	}

	public static void commitZip(String resourceId, String snapshotId) {
		File tempZip = new File(getTemporaryZipPath(resourceId, snapshotId));
		File zip = new File(getZipPath(resourceId));
		// workaround for windows
		System.gc();
		if (zip.exists())
			zip.delete();
		tempZip.renameTo(zip);
	}

	public static void deleteSnapshot(String resourceId, String snapshotId)
			throws ResourceSnapshotDirectoryNotFoundException {
		// workaround for windows
		System.gc();
		FileUtils
				.deleteDir(getResourceSnapshotDirectory(resourceId, snapshotId));
	}

	public static boolean deleteResourceArchive(String resourceId) {
		// workaround for windows
		System.gc();
		return new File(getZipPath(resourceId)).delete();
	}

	public static boolean existResourceArchive(String resourceId) {
		return new File(getZipPath(resourceId)).exists();
	}

	public static ArrayList<File> getFileList(String resourceId,
			boolean filterIgnoredFiles)
			throws ResourceDirectoryNotFoundException {
		return getFileList(getResourceDirectory(resourceId), filterIgnoredFiles);
	}

	public static void deleteAllFiles() {
		// workaround for windows
		System.gc();
		File resourceDir = new File(fileRepoPath);
		for (File dir : resourceDir.listFiles()) {
			if (!dir.getName().equals("..") && !dir.getName().equals("."))
				FileUtils.deleteDir(dir);
		}
	}

	public static void saveToDisk(String resourceId, String serialization) {
		File securityFile = getSecurityFile(resourceId);
		writeToFile(serialization, securityFile);
	}

	private static void writeToFile(String string, File file) {
		if (!file.exists())
			file.getParentFile().mkdirs();
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(string);
			out.close();
		} catch (IOException e) {
			logger.warn("Impossible to write to file " + file.getAbsolutePath());
			e.printStackTrace();
		}

	}

	private static File getSecurityFile(String resourceId) {
		return new File(String.format(
				"%s/%sdata",
				FileUtils.getFilePathHierarchy(fileRepoPath,
						resourceId.toString()), temporaryFilesPrefix));
	}

	public static String getSerializedData(String resourceId)
			throws FileSystemAccessException {
		File securityFile = getSecurityFile(resourceId);
		return readFileAsString(securityFile);
	}

	private static String readFileAsString(File file)
			throws FileSystemAccessException {
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			char[] buf = new char[1024];
			int numRead = 0;

			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
		} catch (IOException e) {
			throw new FileSystemAccessException(String.format(
					"Unable to read from file %s", file.getAbsolutePath()));
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return fileData.toString();
	}

	public static Map<String, Point> getImagesList(String resourceId,
			String mainFileName) throws ResourceDirectoryNotFoundException {
		logger.info("> Looking for image list, file : " + mainFileName
				+ " , resource id : " + resourceId);
		File dir = getResourceDirectory(resourceId);
		File pointedDir = dir;
		File unzipDir;

		if (!StringUtils.isEmpty(mainFileName)) {
			File mainFile = new File(getFilePath(resourceId, mainFileName));
			Boolean izZip = false;
			Boolean izPdf = false;
			if (mainFile.exists() && !mainFile.isDirectory()) {
				try {
					izZip = FileUtils.isZip(mainFile);
				} catch (Exception e) {
					logger.error("impossible to check if this resource file is a zip "
							+ mainFile.getAbsolutePath()
							+ " with error "
							+ e.getLocalizedMessage());
				}
				try {
					izPdf = getMimeType(resourceId, mainFileName).contains(
							"pdf");
				} catch (Exception e) {
					logger.error("impossible to check if this resource file is a pdf "
							+ mainFile.getAbsolutePath()
							+ " with error "
							+ e.getLocalizedMessage());
				}

			}
			try {
				if (izZip) {
					String unzipDirName = new StringBuilder()
							.append(temporaryFilesPrefix).append("unzip")
							.toString();
					unzipDir = new File(dir, unzipDirName);
					unzipDir.mkdir();
					try {
						FileUtils.unzipFile(mainFile,
								unzipDir.getAbsolutePath());
					} catch (IllegalArgumentException e) {
						logger.error("It seems impossible to open this zip file :"
								+ mainFile.getAbsolutePath());

						return Collections.emptyMap();
					}
					pointedDir = unzipDir;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (izPdf) {
				String thumbsExtractionDirName = new StringBuilder()
						.append(temporaryFilesPrefix).append("unzip")
						.toString();
				unzipDir = new File(dir, thumbsExtractionDirName);
				unzipDir.mkdir();
				extractThumbsFromPdf(mainFile.getAbsolutePath(), unzipDir);

				pointedDir = unzipDir;
			}

		}
		return extractImagesFromDirectory(pointedDir, dir);
	}

	private static boolean extractThumbsFromPdf(String fileName, File outputDir) {
		try {
			String[] commande = { "pdfimages", "-j", fileName, "thumb" };
			logger.info("***commande****" + commande);
			logger.info("******************" + outputDir.getAbsolutePath());
			String[] envp = {};
			Process p = Runtime.getRuntime().exec(commande, envp, outputDir);
			BufferedReader output = getOutput(p);
			BufferedReader error = getError(p);
			String ligne = "";
			while ((ligne = output.readLine()) != null) {
				logger.info("***********pdf extraction err. " + ligne);
			}

			while ((ligne = error.readLine()) != null) {
				logger.info("***********pdf extraction. " + ligne);
			}

			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		try {
			String[] commande = { "convert", "*.ppm", "image%d.jpg" };
			String[] envp = {};
			Process p = Runtime.getRuntime().exec(commande, envp, outputDir);
			BufferedReader output = getOutput(p);
			BufferedReader error = getError(p);
			String ligne = "";
			while ((ligne = output.readLine()) != null) {
				logger.info("***********ppm conversion err. " + ligne);
			}

			while ((ligne = error.readLine()) != null) {
				logger.info("***********ppm extraction. " + ligne);
			}

			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	protected static BufferedReader getOutput(Process p) {
		return new BufferedReader(new InputStreamReader(p.getInputStream()));
	}

	protected static BufferedReader getError(Process p) {
		return new BufferedReader(new InputStreamReader(p.getErrorStream()));
	}

	private static Map<String, Point> extractImagesFromDirectory(
			File pointedDir, File baseDir) {
		if (!pointedDir.exists())
			return Collections.emptyMap();
		Map<String, Point> imageList = new HashMap<String, Point>();
		byte pngMagicNum[] = new byte[] { (byte) 0x89, (byte) 0x50,
				(byte) 0x4e, (byte) 0x47, (byte) 0x0d, (byte) 0x0a,
				(byte) 0x1a, (byte) 0x0a };
		byte jpgMagicNum[] = new byte[] { (byte) 0xff, (byte) 0xd8,
				(byte) 0xff, (byte) 0xe0 };
		Collection<File> files = org.apache.commons.io.FileUtils.listFiles(
				pointedDir, FileFilterUtils.magicNumberFileFilter(pngMagicNum),
				TrueFileFilter.INSTANCE);
		files.addAll(org.apache.commons.io.FileUtils.listFiles(pointedDir,
				FileFilterUtils.magicNumberFileFilter(jpgMagicNum),
				TrueFileFilter.INSTANCE));
		Iterator<File> it = files.iterator();
		while (it.hasNext()) {
			File file = (File) it.next();
			Point point = new Point();
			try {
				BufferedImage image = ImageIO.read(file);
				point.setLocation(image.getWidth(), image.getHeight());
			} catch (IOException e) {
				e.printStackTrace();
			}
			String path = baseDir.toURI().relativize(file.toURI()).getPath();
			imageList.put(path, point);
		}
		return imageList;
	}

	public static Map<String, Point> getImagesInPreviewDirectoryList(
			String previewsDirectoryPath) {
		File previewDir = new File(previewsDirectoryPath);
		return extractImagesFromDirectory(previewDir, previewDir);
	}

	public static String getMimeType(String resourceId, String fileName)
			throws ResourceDirectoryNotFoundException {
		File resourceDirectory = getResourceDirectory(resourceId);
		File mainFile = new File(String.format("%s/%s",
				resourceDirectory.getAbsolutePath(), fileName));
		if (!mainFile.exists()) {
			logger.error("Impossible to find main file for resource "
					+ resourceId
					+ " while trying to determine mime type with path :"
					+ mainFile.getAbsolutePath() + " for file " + fileName);
			return null;
		}
		return mimeTypeDetector.detectType(mainFile);

	}

	public static long calculateResourceSize(String resourceId)
			throws ResourceDirectoryNotFoundException {
		ArrayList<File> fileList = getFileList(
				getResourceDirectory(resourceId), true);
		Iterator<File> it = fileList.iterator();
		long size = 0;
		File file;
		while (it.hasNext()) {
			file = (File) it.next();
			size += file.length();
		}
		return size;
	}

	public static String getTextContent(String resourceId, String fileName)
			throws ResourceDirectoryNotFoundException {
		File resourceDirectory = getResourceDirectory(resourceId);
		File mainFile = new File(String.format("%s/%s",
				resourceDirectory.getAbsolutePath(), fileName));
		if (!mainFile.exists()) {
			logger.error("Impossible to find main file for resource "
					+ resourceId
					+ " while trying to extract text content with path :"
					+ mainFile.getAbsolutePath() + " for file " + fileName);
			return StringUtils.EMPTY;
		}
		return textExtractor.extractText(mainFile);
	}

	public static String getTextContent(String resourceId, URL url) {
		return textExtractor.extractText(url);
	}

	public static boolean cropImage(String filePath) {
		logger.info(String.format("Trying to resize imafe file %s", filePath));
		BufferedImage largeImage;
		try {
			largeImage = ImageIO.read(new File(filePath));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		int width, height;
		height = largeImage.getHeight();
		width = largeImage.getWidth();

		logger.info(String
				.format("The image size is %d x %d px", width, height));
		if (height < width * PREVIEWS_IMAGE_MAX_HEIGHT_PROPORTION)
			return true;
		height = Math.round((float) width
				* PREVIEWS_IMAGE_MAX_HEIGHT_PROPORTION);
		logger.info(String.format(
				"The image will be resized to a height of %d ", height));

		if (height == 0 || width == 0) {
			logger.warn("Impossible to compute image dimensions");
			return false;
		}

		BufferedImage smallImage = largeImage.getSubimage(0, 0, width, height);

		return writeToDisk(smallImage, filePath);
	}

	private static boolean writeToDisk(BufferedImage image, String filePath) {
		File outputFile = new File(filePath);
		if (outputFile.exists())
			outputFile.delete();
		outputFile.mkdirs();
		try {
			ImageIO.write(image, "png", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean deleteResourcePreview(final String resourceId,
			final String previewsRepoPath) {
		String previewDirectoryPath = FileUtils.getFilePathHierarchy(
				previewsRepoPath, resourceId);

		// workaround for windows
		System.gc();
		boolean success = true;
		File previewDirectory = new File(previewDirectoryPath);
		File parent = previewDirectory.getParentFile();
		File grandParent = parent.getParentFile();
		File grandgrandParent = grandParent.getParentFile();
		success &= FileUtils.deleteDir(previewDirectory);
		if (!success) {
			logger.error(String.format(
					"Unable to delete preview directory %s for resource %s",
					previewDirectoryPath, resourceId));
		}
		if (success && parent.list().length == 0) {
			success &= FileUtils.deleteDir(parent);
			if (success && grandParent.list().length == 0) {
				success &= FileUtils.deleteDir(grandParent);
				if (success && grandgrandParent.list().length == 0) {
					success &= FileUtils.deleteDir(grandgrandParent);
				}
			}
		}
		return success;
	}

	public static String downloadRemoteFileInResourceDirectory(
			String resourceId, String url)
			throws ResourceDirectoryNotFoundException {
		File resourceDirectory = getResourceDirectory(resourceId);
		String fileName = new StringBuilder().append(temporaryFilesPrefix)
				.append(UUID.randomUUID()).append(".pdf").toString();
		File destinationFile = new File(resourceDirectory, fileName);
		boolean success = FileUtils.downloadFileFromURL(url, destinationFile);
		if (success) {
			return fileName;
		}
		return null;
	}
}
