package eu.mm.software.photofinder.common;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.makernotes.CanonMakernoteDirectory;
import com.drew.metadata.exif.makernotes.NikonType2MakernoteDirectory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.stream.StreamSupport;

@Slf4j
public class ExifExtractor {

    public static ExifData extract(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            if (log.isDebugEnabled()) {
                metadata.getDirectories().forEach(dir ->
                        log.debug("EXIF dir [{}]: {}", dir.getClass().getSimpleName(),
                                dir.getTags().stream()
                                        .map(t -> t.getTagName() + "=" + t.getDescription())
                                        .toList()));
            }

            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            NikonType2MakernoteDirectory nikon = metadata.getFirstDirectoryOfType(NikonType2MakernoteDirectory.class);
            CanonMakernoteDirectory canon = metadata.getFirstDirectoryOfType(CanonMakernoteDirectory.class);

            String make  = desc(ifd0, ExifIFD0Directory.TAG_MAKE);
            String model = desc(ifd0, ExifIFD0Directory.TAG_MODEL);


            String lens     = anyDesc(metadata, ExifSubIFDDirectory.TAG_LENS_MODEL);
            String focal    = parseFocalLength(anyDesc(metadata, ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
            String aperture = parseAperture(anyDesc(metadata, ExifSubIFDDirectory.TAG_FNUMBER));
            String exposure = parseExposure(anyDesc(metadata, ExifSubIFDDirectory.TAG_EXPOSURE_TIME));

            Integer iso = anyInt(metadata, ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
            if (iso == null) iso = parseIso(anyDesc(metadata, ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));

            if (iso == null) iso = anyInt(metadata, 34866);
            if (iso == null) iso = anyInt(metadata, 34867);

            if (iso == null && nikon != null) iso = nikonIso(nikon);
            if (iso == null && nikon != null) iso = parseIso(desc(nikon, NikonType2MakernoteDirectory.TAG_ISO_REQUESTED));


            if (lens == null && nikon != null) lens = desc(nikon, NikonType2MakernoteDirectory.TAG_LENS);

            if (lens == null && canon != null) lens = desc(canon, CanonMakernoteDirectory.TAG_LENS_MODEL);


            if (focal    == null && lens != null) focal    = parseFocalFromLens(lens);
            if (aperture == null && lens != null) aperture = parseApertureFromLens(lens);

            if (make == null && model == null && lens == null) return null;

            ExifData exif = new ExifData(make, model, lens, focal, aperture, exposure, iso);
            log.info("EXIF [{}]: camera={} | lens={} | focal={} | f/{} | {} | ISO {}",
                    file.getName(), exif.camera(), lens, focal, aperture, exposure, iso);
            return exif;

        } catch (Exception e) {
            log.debug("No EXIF in {}: {}", file.getName(), e.getMessage());
            return null;
        }
    }

    /** Przeszukuje wszystkie katalogi w poszukiwaniu danego tagu — niezależnie od klasy katalogu */
    private static Directory findAny(Metadata metadata, int tag) {
        return StreamSupport.stream(metadata.getDirectories().spliterator(), false)
                .filter(d -> d.containsTag(tag))
                .findFirst()
                .orElse(null);
    }

    private static String anyDesc(Metadata metadata, int tag) {
        return desc(findAny(metadata, tag), tag);
    }

    private static Integer anyInt(Metadata metadata, int tag) {
        return getInt(findAny(metadata, tag), tag);
    }

    private static String desc(Directory dir, int tag) {
        if (dir == null || !dir.containsTag(tag)) return null;
        String val = dir.getDescription(tag);
        if (val == null) return null;
        val = val.trim();
        return val.isEmpty() ? null : val;
    }

    private static Integer getInt(Directory dir, int tag) {
        if (dir == null || !dir.containsTag(tag)) return null;
        return dir.getInteger(tag);
    }


    private static String parseFocalLength(String raw) {
        if (raw == null) return null;
        String val = raw.replace(" mm", "").replace(",", ".").trim();
        // Usuń tylko KOŃCOWĄ część ".0" (np. "50.0" → "50"), nie zera w środku ("50.04" zostaje)
        return val.replaceAll("\\.0+$", "");
    }


    private static String parseAperture(String raw) {
        if (raw == null) return null;
        String val = raw.startsWith("f/") ? raw.substring(2) : raw;
        return val.replace(",", ".");
    }


    private static String parseExposure(String raw) {
        if (raw == null) return null;
        return raw.replace(" sec", "").trim();
    }


    private static String parseFocalFromLens(String lens) {
        int mm = lens.indexOf("mm");
        if (mm <= 0) return null;
        String focal = lens.substring(0, mm).trim();
        int start = focal.lastIndexOf(' ');
        if (start >= 0) focal = focal.substring(start + 1);
        return focal.replace(",", ".");
    }

    // "80-200mm f/2,8" → "2.8"
    private static String parseApertureFromLens(String lens) {
        int fi = lens.indexOf("f/");
        if (fi < 0) return null;
        String val = lens.substring(fi + 2).trim();
        int end = 0;
        while (end < val.length() && (Character.isDigit(val.charAt(end))
                || val.charAt(end) == '.' || val.charAt(end) == ',')) end++;
        return val.substring(0, end).replace(",", ".");
    }

    /**
     * Nikon TAG_ISO_1 format: starsze modele → direct ISO, nowsze → [flag, ISO*100].
     * Próbuje getIntArray: jeśli druhga wartość ≥ 10000 → dzieli przez 100.
     * Fallback: parseIso z opisu string.
     */
    private static Integer nikonIso(NikonType2MakernoteDirectory nikon) {
        try {
            int[] arr = nikon.getIntArray(NikonType2MakernoteDirectory.TAG_ISO_1);
            if (arr != null && arr.length >= 2 && arr[1] > 0) {
                return arr[1] >= 10000 ? arr[1] / 100 : arr[1];
            }
        } catch (Exception ignored) {}
        return parseIso(desc(nikon, NikonType2MakernoteDirectory.TAG_ISO_1));
    }

    // "ISO 200" → 200  |  "200" → 200  |  "0 100" → 100
    private static Integer parseIso(String raw) {
        if (raw == null) return null;
        // Prosty przypadek: "200" lub "ISO 200"
        try {
            return Integer.parseInt(raw.replace("ISO", "").trim());
        } catch (NumberFormatException ignored) {}
        // Format wielowartościowy "0 100" lub "Unknown (1 10000)" — bierz ostatnią dodatnią liczbę
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d+)\\b").matcher(raw);
        Integer last = null;
        while (m.find()) {
            int v = Integer.parseInt(m.group(1));
            if (v > 0) last = v;
        }
        return last;
    }
}
