package eu.mm.software.photofinder.common;

import eu.mm.software.photofinder.photosattribute.application.query.PhotosAttributesDto;
import eu.mm.software.photofinder.photosattribute.domain.PhotoAttribute;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public class FileUtils {

    public static final String JPG = "jpg";

    public static final int MAX_DIMENSION = 1024;

    public static byte[] imageToByte(File file) {
        return imageToByte(file, MAX_DIMENSION);
    }

    public static byte[] imageToByte(File file, int maxDimension) {
        return switch (FilenameUtils.getExtension(file.getName()).toUpperCase()) {
            case "JPG", "JPEG", "PNG" -> readJpg(file, maxDimension);
            case "NEF", "CR2", "DNG", "PEF", "ARW" -> convertToJpg(file, maxDimension);
            default -> throw new IllegalArgumentException("Unsupported file format");
        };
    }

    public static byte[] convertToJpg(File file, int maxDimension) {

        BufferedImage read;
        try {
            read = ImageIO.read(file);
            if (Objects.nonNull(read)) {

                Path tempFile = Files.createTempFile("photofinder-", ".jpg");
                try {
                    ImageIO.write(read, JPG, tempFile.toFile());
                    return readJpg(tempFile.toFile(), maxDimension);
                } finally {
                    Files.deleteIfExists(tempFile);
                }
            }
        } catch (IOException e) {
            log.error("File {} IOException: {}", file, " exception", e);
        }
        return null;
    }

    public static byte[] readJpg(File file, int maxDimension) {
    try {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            log.error("Could not read image from file: {}", file.getAbsolutePath());
            return null;
        }

        BufferedImage convertedImage = convertToSupportedFormat(image);
        BufferedImage resizedImage = resizeIfTooLarge(convertedImage, maxDimension);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam params = writer.getDefaultWriteParam();

        if (params.canWriteCompressed()) {
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.80f);
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(resizedImage, null, null), params);
        } finally {
            writer.dispose();
        }

        log.info(
                "Final image {}x{}, {} KB",
                resizedImage.getWidth(),
                resizedImage.getHeight(),
                baos.size() / 1024
        );

        return baos.toByteArray();

    } catch (IOException e) {
        log.error("File {} IOException", file, e);
        return null;
    }
}


    private static BufferedImage convertToSupportedFormat(BufferedImage originalImage) {
        if (originalImage.getType() == BufferedImage.TYPE_INT_RGB ||
                originalImage.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            return originalImage;
        }

        BufferedImage convertedImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = convertedImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, originalImage.getWidth(), originalImage.getHeight());
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        return convertedImage;
    }

    /**
     * Downscales the image so that its longer edge is at most {@code maxDimension},
     * preserving the aspect ratio. Returns the original image when it already fits.
     */
    private static BufferedImage resizeIfTooLarge(BufferedImage image, int maxDimension) {
        int width = image.getWidth();
        int height = image.getHeight();
        int longestEdge = Math.max(width, height);

        if (longestEdge <= maxDimension) {
            return image;
        }

        double scale = (double) maxDimension / longestEdge;
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();

        log.info("Resized image from {}x{} to {}x{}", width, height, newWidth, newHeight);
        return resized;
    }

    public static boolean fileNotExist(PhotoAttribute it) {
        return !new File(it.getPath().endsWith("/") ?
                it.getPath() + it.getFilename() : it.getPath() + "/" + it.getFilename()).exists();
    }

    public static boolean fileNotExist(PhotosAttributesDto it) {
        return !new File(it.path().endsWith("/") ?
                it.path() + it.filename() : it.path() + "/" + it.filename()).exists();
    }
}
