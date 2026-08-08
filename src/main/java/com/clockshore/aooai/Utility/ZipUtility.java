package com.clockshore.aooai.Utility;

import lombok.experimental.UtilityClass;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@UtilityClass
public class ZipUtility {

    public void addFileToZip(ZipOutputStream zos, String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);

        zos.putNextEntry(new ZipEntry(path.substring(path.lastIndexOf("/") + 1)));

        try (InputStream is = resource.getInputStream()) {
            StreamUtils.copy(is, zos);
        }

        zos.closeEntry();
    }
}
