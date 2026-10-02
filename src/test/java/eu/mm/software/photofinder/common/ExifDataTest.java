package eu.mm.software.photofinder.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExifDataTest {

    @Test
    void camera_returnsMakeAndModel_whenBothPresent() {
        ExifData exif = new ExifData("Sony", "Alpha 7 IV", null, null, null, null, null);
        assertThat(exif.camera()).isEqualTo("Sony Alpha 7 IV");
    }

    @Test
    void camera_returnsModelOnly_whenModelAlreadyStartsWithMake() {
        ExifData exif = new ExifData("Sony", "Sony Alpha 7 IV", null, null, null, null, null);
        assertThat(exif.camera()).isEqualTo("Sony Alpha 7 IV");
    }

    @Test
    void camera_returnsMake_whenModelIsNull() {
        // bug fix: gdy jest tylko marka bez modelu, nie powinna być gubiona
        ExifData exif = new ExifData("Sony", null, null, null, null, null, null);
        assertThat(exif.camera()).isEqualTo("Sony");
    }

    @Test
    void camera_returnsNull_whenBothNull() {
        ExifData exif = new ExifData(null, null, null, null, null, null, null);
        assertThat(exif.camera()).isNull();
    }

    @Test
    void toPromptString_returnsNull_whenAllFieldsNull() {
        ExifData exif = new ExifData(null, null, null, null, null, null, null);
        assertThat(exif.toPromptString()).isNull();
    }

    @Test
    void toPromptString_buildsCorrectString_withAllFields() {
        ExifData exif = new ExifData("Canon", "EOS R5", "RF 50mm f/1.8", "50", "1.8", "1/500", 400);
        assertThat(exif.toPromptString())
                .isEqualTo("Canon EOS R5, RF 50mm f/1.8, 50mm, f/1.8, 1/500s, ISO 400");
    }

    @Test
    void toPromptString_skipsNullFields() {
        ExifData exif = new ExifData(null, "EOS R5", null, "85", null, null, 200);
        assertThat(exif.toPromptString()).isEqualTo("EOS R5, 85mm, ISO 200");
    }

    @Test
    void toPromptString_returnsNull_whenOnlyCameraAndModelAreNullButLensIsNull() {
        ExifData exif = new ExifData(null, null, null, "50", null, null, null);
        assertThat(exif.toPromptString()).isEqualTo("50mm");
    }
}
