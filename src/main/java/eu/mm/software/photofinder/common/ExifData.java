package eu.mm.software.photofinder.common;

import java.util.ArrayList;
import java.util.List;

public record ExifData(
        String cameraMake,
        String cameraModel,
        String lens,
        String focalLength,
        String aperture,
        String shutterSpeed,
        Integer iso
) {
    public String camera() {
        if (cameraMake != null && cameraModel != null) {
            return cameraModel.startsWith(cameraMake) ? cameraModel : cameraMake + " " + cameraModel;
        }
        return cameraModel != null ? cameraModel : cameraMake;
    }

    public String toPromptString() {
        List<String> parts = new ArrayList<>();
        if (camera() != null) parts.add(camera());
        if (lens != null) parts.add(lens);
        if (focalLength != null) parts.add(focalLength + "mm");
        if (aperture != null) parts.add("f/" + aperture);
        if (shutterSpeed != null) parts.add(shutterSpeed + "s");
        if (iso != null) parts.add("ISO " + iso);
        return parts.isEmpty() ? null : String.join(", ", parts);
    }
}
