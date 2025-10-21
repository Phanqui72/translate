package com; // Thay đổi thành package của bạn

import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.cloud.speech.v1.*;
import com.google.cloud.speech.v1.LocationName;
import com.google.cloud.storage.*;
import com.google.cloud.translate.v3.*;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@WebServlet("/translate")
@MultipartConfig
public class TranslateServlet extends HttpServlet {

    private static final String PROJECT_ID = "translate-project-ute"; // THAY THẾ BẰNG PROJECT ID CỦA BẠN
    private static final String BUCKET_NAME = "translate-project-ute-files-qui"; // THAY THẾ BẰNG TÊN BUCKET CỦA BẠN

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String type = request.getParameter("type");
        String fromLang = request.getParameter("fromLang");
        String toLang = request.getParameter("toLang");

        try {
            switch (type) {
                case "text":
                    response.setCharacterEncoding("UTF-8");
                    String textResult = handleTextTranslation(request, fromLang, toLang);
                    response.getWriter().write(textResult);
                    break;
                case "image":
                    response.setCharacterEncoding("UTF-8");
                    String imageResult = handleImageTranslation(request, fromLang, toLang);
                    response.getWriter().write(imageResult);
                    break;
                case "audio":
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    String audioResult = handleAudioTranslation(request, fromLang, toLang);
                    response.getWriter().write(audioResult);
                    break;
                case "document":
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    String docResult = handleDocumentTranslation(request, fromLang, toLang);
                    response.getWriter().write(docResult);
                    break;
                case "web":
                    response.setContentType("text/html; charset=UTF-8");
                    String webResult = handleWebTranslation(request, toLang);
                    response.getWriter().write(webResult);
                    break;
                default:
                    response.getWriter().write("{\"error\": \"Chức năng này đang được phát triển.\"}");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Đã xảy ra lỗi phía máy chủ: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    // ================== HANDLERS ==================

    private String handleTextTranslation(HttpServletRequest request, String fromLang, String toLang) throws IOException {
        String inputText = request.getParameter("inputText");
        return translateText(inputText, fromLang, toLang);
    }

    private String handleImageTranslation(HttpServletRequest request, String fromLang, String toLang) throws IOException, ServletException {
        Part filePart = request.getPart("imageFile");
        if (filePart == null || filePart.getSize() == 0) return "No image file uploaded.";
        byte[] imageBytes = filePart.getInputStream().readAllBytes();
        String extractedText = detectTextInImage(imageBytes);
        if (extractedText == null || extractedText.trim().isEmpty()) return "No text found in the image.";
        return translateText(extractedText, fromLang, toLang);
    }

    private String handleAudioTranslation(HttpServletRequest request, String fromLang, String toLang) throws Exception {
        Part filePart = request.getPart("audioFile");
        if (filePart == null || filePart.getSize() == 0) return "{\"error\": \"No audio file received.\"}";
        String gcsUri = uploadToGcs(filePart);
        String transcribedText = transcribeAudio(gcsUri, fromLang);
        if (transcribedText.isEmpty()) return "{\"transcription\": \"(Could not recognize speech)\", \"translation\": \"\"}";
        String translatedText = translateText(transcribedText, fromLang.split("-")[0], toLang);
        return String.format("{\"transcription\": \"%s\", \"translation\": \"%s\"}", transcribedText.replace("\"", "'"), translatedText.replace("\"", "'"));
    }

    private String handleDocumentTranslation(HttpServletRequest request, String fromLang, String toLang) throws Exception {
        Part filePart = request.getPart("docFile");
        if (filePart == null || filePart.getSize() == 0) return "{\"error\": \"No document file received.\"}";
        String sourceGcsUri = uploadToGcs(filePart);
        String translatedDocGcsPath = translateDocument(sourceGcsUri, fromLang, toLang, filePart.getContentType());
        String downloadUrl = generateSignedUrl(translatedDocGcsPath);
        return String.format("{\"downloadUrl\": \"%s\", \"fileName\": \"translated-%s\"}", downloadUrl, filePart.getSubmittedFileName());
    }

    private String handleWebTranslation(HttpServletRequest request, String toLang) throws IOException {
        String url = request.getParameter("websiteUrl");
        System.out.println("Đang trích xuất và dịch văn bản từ trang web: " + url);

        // 1. Dùng Jsoup để tải và phân tích HTML từ URL
        Document doc = Jsoup.connect(url).get();

        // 2. Trích xuất TOÀN BỘ văn bản từ thẻ <body> của trang web.
        // Jsoup sẽ tự động thêm các dấu xuống dòng giữa các khối văn bản.
        String originalText = doc.body().text();

        // 3. Chỉ dịch nếu có văn bản thực sự
        if (originalText != null && !originalText.trim().isEmpty()) {
            System.out.println("Trích xuất thành công, đang gửi đến API dịch...");
            // 4. Dịch toàn bộ khối văn bản (để API tự phát hiện ngôn ngữ nguồn)
            String translatedText = translateText(originalText, "", toLang);
            System.out.println("Dịch trang web hoàn tất.");
            return translatedText;
        } else {
            return "Không tìm thấy nội dung văn bản nào trên trang web này.";
        }
    }

    // ================== GOOGLE CLOUD API HELPERS ==================

    private String uploadToGcs(Part filePart) throws IOException {
        Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
        String fileName = UUID.randomUUID() + "-" + filePart.getSubmittedFileName();
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(filePart.getContentType()).build();
        storage.create(blobInfo, filePart.getInputStream().readAllBytes());
        return "gs://" + BUCKET_NAME + "/" + fileName;
    }

    private String translateText(String text, String sourceLang, String targetLang) throws IOException {
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            LocationName parent = LocationName.of(PROJECT_ID, "global");
            TranslateTextRequest.Builder requestBuilder = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setTargetLanguageCode(targetLang)
                    .addContents(text);
            if (sourceLang != null && !sourceLang.isEmpty()) {
                requestBuilder.setSourceLanguageCode(sourceLang);
            }
            TranslateTextResponse response = client.translateText(requestBuilder.build());
            if (response.getTranslationsCount() > 0) {
                return response.getTranslations(0).getTranslatedText();
            }
            return "Could not translate the text.";
        }
    }

    private String detectTextInImage(byte[] imageBytes) throws IOException {
        try (ImageAnnotatorClient visionClient = ImageAnnotatorClient.create()) {
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
            AnnotateImageResponse response = visionClient.batchAnnotateImages(Collections.singletonList(request)).getResponses(0);
            if (response.hasError()) return "";
            return response.getFullTextAnnotation().getText();
        }
    }

    private String transcribeAudio(String gcsUri, String langCode) throws Exception {
        try (SpeechClient speechClient = SpeechClient.create()) {
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
                    .setSampleRateHertz(48000)
                    .setAudioChannelCount(1)
                    .setLanguageCode(langCode)
                    .setModel("default")
                    .setEnableAutomaticPunctuation(true)
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();
            OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
                    speechClient.longRunningRecognizeAsync(config, audio);
            while (!response.isDone()) {
                Thread.sleep(3000);
            }
            StringBuilder transcription = new StringBuilder();
            for (SpeechRecognitionResult result : response.get().getResultsList()) {
                transcription.append(result.getAlternativesList().get(0).getTranscript());
            }
            return transcription.toString();
        }
    }

    private String translateDocument(String gcsSourceUri, String sourceLang, String targetLang, String mimeType) throws IOException {
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            LocationName parent = LocationName.of(PROJECT_ID, "global");
            GcsSource gcsSource = GcsSource.newBuilder().setInputUri(gcsSourceUri).build();
            DocumentInputConfig inputConfig = DocumentInputConfig.newBuilder()
                    .setGcsSource(gcsSource)
                    .setMimeType(mimeType)
                    .build();
            String outputUriPrefix = "gs://" + BUCKET_NAME + "/translated_documents/" + UUID.randomUUID().toString() + "/";
            GcsDestination gcsDestination = GcsDestination.newBuilder().setOutputUriPrefix(outputUriPrefix).build();
            DocumentOutputConfig outputConfig = DocumentOutputConfig.newBuilder()
                    .setGcsDestination(gcsDestination)
                    .build();
            TranslateDocumentRequest request = TranslateDocumentRequest.newBuilder()
                    .setParent(parent.toString())
                    .setSourceLanguageCode(sourceLang)
                    .setTargetLanguageCode(targetLang)
                    .setDocumentInputConfig(inputConfig)
                    .setDocumentOutputConfig(outputConfig)
                    .build();
            client.translateDocument(request);
            Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
            String prefixToSearch = outputUriPrefix.replace("gs://" + BUCKET_NAME + "/", "");
            Page<Blob> blobs = storage.list(BUCKET_NAME, Storage.BlobListOption.prefix(prefixToSearch));
            for (Blob blob : blobs.iterateAll()) {
                return "gs://" + BUCKET_NAME + "/" + blob.getName();
            }
            throw new FileNotFoundException("Không tìm thấy file đã dịch trong thư mục: " + outputUriPrefix);
        } catch (Exception e) {
            throw new IOException("Failed to translate document.", e);
        }
    }

    private String generateSignedUrl(String gcsFilePath) {
        Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
        String objectName = gcsFilePath.replace("gs://" + BUCKET_NAME + "/", "");
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(BUCKET_NAME, objectName)).build();
        URL url = storage.signUrl(blobInfo, 15, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }
}