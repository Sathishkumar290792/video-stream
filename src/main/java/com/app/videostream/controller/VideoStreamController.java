package com.app.videostream.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.app.videostream.service.VideoStreamService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/video")
public class VideoStreamController {

	private final VideoStreamService videoStreamService;

	private final Path basePath = Paths.get("./src/main/resources/video/");

	public VideoStreamController(VideoStreamService videoStreamService) {
		this.videoStreamService = videoStreamService;
	}

	@GetMapping("/stream/{fileType}/{fileName}")
	public Mono<ResponseEntity<byte[]>> streamVideo(ServerHttpResponse serverHttpResponse,
			@RequestHeader(value = "Range", required = false) String httpRangeList,
			@PathVariable("fileType") String fileType, @PathVariable("fileName") String fileName) {
		return Mono.just(videoStreamService.prepareContent(fileName, fileType, httpRangeList));
	}

	@GetMapping("/stream/{files}")
	public ResponseEntity<List<String>> streamVideo(@PathVariable("files") String files) {
		List<String> list = videoStreamService.getAllFiles(files);
		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(list);
	}

	@PostMapping("/stream/file/upload")
	public Mono<Void> uploadFile(@RequestPart("file") Mono<FilePart> file) {
		return file.doOnNext(fp -> System.out.println("Received File : " + fp.filename()))
				.flatMap(fp -> fp.transferTo(basePath.resolve(fp.filename()))).then();
	}

	
}
