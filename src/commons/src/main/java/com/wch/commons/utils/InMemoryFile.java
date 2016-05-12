package com.wch.commons.utils;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class InMemoryFile {
    private ChunkedMemoryStream fileStream;
    private String fileName;
    private String contentType;

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public InputChunkedMemoryStream getInputStream() throws Exception {
        if (fileStream != null) {
            return new InputChunkedMemoryStream(fileStream);
        } else {
            // dead code...
            return null;
        }
    }

    public InMemoryFile(MultipartFile multipartFile) {
        if (multipartFile != null) {
            this.fileName = multipartFile.getOriginalFilename();
            this.contentType = multipartFile.getContentType();

            if (Utils.isNullOrEmptyString(contentType)) {
                contentType = FileUtils.inferContentTypeFromFileName(fileName);
                log.info(String.format("InMemoryFile:  null contentType; filename is \"%s\", inferring contentType=\"%s\"", fileName, contentType));
            }

            // copy over the multipart file, in case we dispatch async on an ExecuteService
            try {
                fileStream = new ChunkedMemoryStream();
                StreamUtils.writeStreamToStream(multipartFile.getInputStream(), fileStream);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public InMemoryFile(ChunkedMemoryStream bms, String fileName, String contentType) {
        this.fileStream = bms;
        this.fileName = fileName;
        this.contentType = contentType;
    }

    public void dispose() {
        ChunkedMemoryStream.safeDispose(fileStream);

        fileStream = null;
    }

    /*
     * Due to the way we do uploads, 0-length files might get uploaded.
     */
    public static InMemoryFile[] create(MultipartFile[] mediaFiles) {
        List<InMemoryFile> files = new ArrayList<InMemoryFile>(mediaFiles.length);

        //InMemoryFile[] files = new InMemoryFile[mediaFiles.length];

        for (int i = 0; i < mediaFiles.length; i++) {
            if (mediaFiles[i].getSize() > 0) {
                files.add(new InMemoryFile(mediaFiles[i]));
            }
        }

        return Utils.toArray(InMemoryFile.class, files);
    }
}