package com.app.videostream.service;

import static com.app.videostream.constants.ApplicationConstants.ACCEPT_RANGES;
import static com.app.videostream.constants.ApplicationConstants.BYTES;
import static com.app.videostream.constants.ApplicationConstants.BYTE_RANGE;
import static com.app.videostream.constants.ApplicationConstants.CHUNK_SIZE;
import static com.app.videostream.constants.ApplicationConstants.CONTENT_LENGTH;
import static com.app.videostream.constants.ApplicationConstants.CONTENT_RANGE;
import static com.app.videostream.constants.ApplicationConstants.CONTENT_TYPE;
import static com.app.videostream.constants.ApplicationConstants.VIDEO_CONTENT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class VideoStreamService {


    private final Logger logger = LoggerFactory.getLogger(VideoStreamService.class);

    /**
     * Prepare the content.
     *
     * @param fileName String.
     * @param fileType String.
     * @param range    String.
     * @return ResponseEntity.
     */
    public ResponseEntity<byte[]> prepareContent(final String fileName, final String fileType, final String range) {

        try {
            final String fileKey = fileName + "." + fileType;
            long rangeStart = 0;
            long rangeEnd = CHUNK_SIZE;
            final Long fileSize = getFileSize(fileKey);
            if (range == null) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .header(CONTENT_TYPE, VIDEO_CONTENT + fileType)
                        .header(ACCEPT_RANGES, BYTES)
                        .header(CONTENT_LENGTH, String.valueOf(rangeEnd))
                        .header(CONTENT_RANGE, BYTES + " " + rangeStart + "-" + rangeEnd + "/" + fileSize)
                        .header(CONTENT_LENGTH, String.valueOf(fileSize))
                        .body(readByteRangeNew(fileKey, rangeStart, rangeEnd)); // Read the object and convert it as bytes
            }
            String[] ranges = range.split("-");
            rangeStart = Long.parseLong(ranges[0].substring(6));
            if (ranges.length > 1) {
                rangeEnd = Long.parseLong(ranges[1]);
            } else {
                rangeEnd = rangeStart + CHUNK_SIZE;
            }

            rangeEnd = Math.min(rangeEnd, fileSize - 1);
            final byte[] data = readByteRangeNew(fileKey, rangeStart, rangeEnd);
            final String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
            HttpStatus httpStatus = HttpStatus.PARTIAL_CONTENT;
            if (rangeEnd >= fileSize) {
                httpStatus = HttpStatus.OK;
            }
            return ResponseEntity.status(httpStatus)
                    .header(CONTENT_TYPE, VIDEO_CONTENT + fileType)
                    .header(ACCEPT_RANGES, BYTES)
                    .header(CONTENT_LENGTH, contentLength)
                    .header(CONTENT_RANGE, BYTES + " " + rangeStart + "-" + rangeEnd + "/" + fileSize)
                    .body(data);
        } catch (IOException e) {
            logger.error("Exception while reading the file {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }


    }

    /**
     * ready file byte by byte.
     *
     * @param filename String.
     * @param start    long.
     * @param end      long.
     * @return byte array.
     * @throws IOException exception.
     */
    public byte[] readByteRangeNew(String filename, long start, long end) throws IOException {
        Path path = Paths.get(getFilePath(), filename);
        byte[] data = Files.readAllBytes(path);
        byte[] result = new byte[(int) (end - start) + 1];
        System.arraycopy(data, (int) start, result, 0, (int) (end - start) + 1);
        return result;
    }


    public byte[] readByteRange(String filename, long start, long end) throws IOException {
        Path path = Paths.get(getFilePath(), filename);
        try (InputStream inputStream = (Files.newInputStream(path));
             ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream()) {
            byte[] data = new byte[BYTE_RANGE];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                bufferedOutputStream.write(data, 0, nRead);
            }
            bufferedOutputStream.flush();
            byte[] result = new byte[(int) (end - start) + 1];
            System.arraycopy(bufferedOutputStream.toByteArray(), (int) start, result, 0, result.length);
            return result;
        }
    }

    /**
     * Get the filePath.
     *
     * @return String.
     */
    private String getFilePath() {
    	String url = "C:/Users/Manoj/Development/Application/video-service/src/main/resources/video";
    	return url;
    }

    /**
     * Content length.
     *
     * @param fileName String.
     * @return Long.
     */
    public Long getFileSize(String fileName) {
        return Optional.ofNullable(fileName)
                .map(file -> Paths.get(getFilePath(), file))
                .map(this::sizeFromFile)
                .orElse(0L);
    }

    /**
     * Getting the size from the path.
     *
     * @param path Path.
     * @return Long.
     */
    private Long sizeFromFile(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ioException) {
            logger.error("Error while getting the file size", ioException);
        }
        return 0L;
    }

	public List<String> getAllFiles(String files) {
		List<String> list = new ArrayList<>();
		try (Stream<Path> stream = Files.list(Paths.get(getFilePath()))) {
			Set<String> set = stream.filter(file -> !Files.isDirectory(file)).map(Path::getFileName).map(Path::toString)
					.collect(Collectors.toSet());
			setFileNames(set, list);
		} catch (IOException ex) {

		}
		return list;
	}

	private void setFileNames(Set<String> set, List<String> list) {
		String url = "http://localhost:8080/video/stream/mp4/";
		for (String s : set) {
			String fileName = url + s.replaceFirst("[.][^.]+$", "");
			list.add(fileName);
		}
	}
	
}