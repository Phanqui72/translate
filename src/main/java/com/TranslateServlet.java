package com;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1.*;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Word;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation.DetectedBreak.BreakType;
import com.google.cloud.translate.v3.DocumentInputConfig;
import com.google.cloud.translate.v3.DocumentOutputConfig;
import com.google.cloud.translate.v3.GcsDestination;
import com.google.cloud.translate.v3.GcsSource;
import com.google.cloud.translate.v3.TranslateDocumentRequest;
import com.google.cloud.translate.v3.TranslateDocumentResponse;
import java.util.concurrent.TimeUnit;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.FileNotFoundException;

@WebServlet("/translate")
@MultipartConfig
public class TranslateServlet extends HttpServlet {

    // !!! THAY THẾ CÁC GIÁ TRỊ NÀY BẰNG THÔNG TIN CỦA BẠN !!!
    private static final String PROJECT_ID = "translate-project-ute";
    private static final String BUCKET_NAME = "translate-project-ute-files-qui";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String type = request.getParameter("type");
        String fromLang = request.getParameter("fromLang");
        String toLang = request.getParameter("toLang");

        try {
            if ("text".equals(type)) {
                response.setCharacterEncoding("UTF-8");
                String result = handleTextTranslation(request, fromLang, toLang);
                response.getWriter().write(result);
            } else if ("image".equals(type)) {
                response.setCharacterEncoding("UTF-8");
                String result = handleImageTranslation(request, fromLang, toLang); // Quay lại phiên bản đơn giản
                response.getWriter().write(result);
            } else if ("audio".equals(type)) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                String jsonResult = handleAudioTranslation(request, fromLang, toLang);
                response.getWriter().write(jsonResult);
            } else if ("document".equals(type)) {
                // **THÊM LOGIC XỬ LÝ DOCUMENT**
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                String jsonResult = handleDocumentTranslation(request, fromLang, toLang);
                response.getWriter().write(jsonResult);
            } else {
                response.getWriter().write("Chức năng này đang được phát triển.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Đã xảy ra lỗi phía máy chủ: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }


    private String handleTextTranslation(HttpServletRequest request, String fromLang, String toLang) throws IOException {
        String inputText = request.getParameter("inputText");
        return translateText(inputText, fromLang, toLang);
    }

    private String handleImageTranslation(HttpServletRequest request, String fromLang, String toLang) throws IOException, ServletException {
        Part filePart = request.getPart("imageFile");
        if (filePart == null || filePart.getSize() == 0) {
            return "No image file uploaded.";
        }
        try (InputStream fileContent = filePart.getInputStream()) {
            byte[] imageBytes = fileContent.readAllBytes();

            // **Sử dụng lại phương thức trích xuất văn bản đơn giản**
            String extractedText = detectTextInImage(imageBytes);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                return "No text found in the image.";
            }
            return translateText(extractedText, fromLang, toLang);
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to process the image.";
        }
    }

    private String handleAudioTranslation(HttpServletRequest request, String fromLang, String toLang) {
        try {
            Part filePart = request.getPart("audioFile");
            if (filePart == null || filePart.getSize() == 0) {
                return "{\"error\": \"No audio file received.\"}";
            }

            System.out.println("--- BẮT ĐẦU XỬ LÝ ÂM THANH ---");

            String gcsUri = uploadToGcs(filePart);

            String transcribedText = transcribeAudio(gcsUri, fromLang);

            // **LOG 1: IN RA KẾT QUẢ NHẬN DẠNG GỐC**
            System.out.println("--- BƯỚC 1: VĂN BẢN GỐC TỪ SPEECH-TO-TEXT ---");
            System.out.println("Văn bản nhận dạng được: '" + transcribedText + "'");

            if (transcribedText.isEmpty()) {
                return "{\"transcription\": \"(Could not recognize speech)\", \"translation\": \"\"}";
            }

            String sourceLangForTranslation = fromLang.split("-")[0];
            String translatedText = translateText(transcribedText, sourceLangForTranslation, toLang);

            // **LOG 2: IN RA KẾT QUẢ SAU KHI DỊCH**
            System.out.println("--- BƯỚC 2: VĂN BẢN SAU KHI DỊCH BẰNG TRANSLATION API ---");
            System.out.println("Văn bản đã dịch: '" + translatedText + "'");
            System.out.println("--- KẾT THÚC XỬ LÝ ÂM THANH ---");


            return String.format("{\"transcription\": \"%s\", \"translation\": \"%s\"}",
                    transcribedText.replace("\"", "'"),
                    translatedText.replace("\"", "'"));
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Server error: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
    private String handleDocumentTranslation(HttpServletRequest request, String fromLang, String toLang) throws Exception {
        Part filePart = request.getPart("docFile");
        if (filePart == null || filePart.getSize() == 0) {
            return "{\"error\": \"No document file received.\"}";
        }

        System.out.println("--- BẮT ĐẦU DỊCH TÀI LIỆU ---");

        // 1. Tải tệp gốc lên GCS
        String sourceGcsUri = uploadToGcs(filePart);
        System.out.println("Tệp gốc đã được tải lên: " + sourceGcsUri);

        // 2. Dịch tài liệu
        String translatedDocGcsPath = translateDocument(sourceGcsUri, fromLang, toLang, filePart.getContentType());
        System.out.println("Tài liệu đã được dịch và lưu tại: " + translatedDocGcsPath);

        // 3. Tạo URL tải xuống an toàn
        String downloadUrl = generateSignedUrl(translatedDocGcsPath);
        System.out.println("URL tải xuống được tạo: " + downloadUrl);

        return String.format("{\"downloadUrl\": \"%s\", \"fileName\": \"translated-%s\"}", downloadUrl, filePart.getSubmittedFileName());
    }

    private String handleWebTranslation(HttpServletRequest request, String toLang) throws IOException {
        String url = request.getParameter("websiteUrl");
        Document doc = Jsoup.connect(url).get();

        // Dịch các thành phần chính
        doc.title(translateText(doc.title(), "en", toLang)); // Giả định ngôn ngữ gốc là tiếng Anh
        for (Element element : doc.select("p, h1, h2, h3, h4, h5, h6, li, span, a, button")) {
            for (TextNode textNode : element.textNodes()) {
                String translated = translateText(textNode.text(), "en", toLang);
                textNode.text(translated);
            }
        }
        return doc.outerHtml();
    }


    // ======================================================================
    // HELPER METHODS - GỌI CÁC API CỦA GOOGLE CLOUD
    // ======================================================================

    /** Dịch một đoạn văn bản */
    private String translateText(String text, String sourceLang, String targetLang) throws IOException {
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            LocationName parent = LocationName.of(PROJECT_ID, "global");
            TranslateTextRequest request = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setSourceLanguageCode(sourceLang)
                    .setTargetLanguageCode(targetLang)
                    .addContents(text)
                    .build();

            TranslateTextResponse response = client.translateText(request);
            return response.getTranslations(0).getTranslatedText();
        }
    }

    /** Nhận dạng văn bản trong ảnh */
    private String detectTextInImage(byte[] imageBytes) throws IOException {
        try (ImageAnnotatorClient visionClient = ImageAnnotatorClient.create()) {
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image img = Image.newBuilder().setContent(imgBytes).build();

            // **1. Quay lại sử dụng TEXT_DETECTION**
            Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();

            AnnotateImageResponse response = visionClient.batchAnnotateImages(Collections.singletonList(request)).getResponses(0);
            if (response.hasError()) {
                System.err.println("Lỗi Vision API: " + response.getError().getMessage());
                return "";
            }

            // **2. Lấy toàn bộ văn bản, không xử lý định dạng**
            return response.getFullTextAnnotation().getText();
        }
    }

    /** Tải file lên Cloud Storage và trả về GCS URI */
    private String uploadToGcs(Part filePart) throws IOException {
        Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
        String fileName = UUID.randomUUID() + "-" + filePart.getSubmittedFileName();
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        try (InputStream is = filePart.getInputStream()) {
            storage.create(blobInfo, is.readAllBytes());
        }

        return "gs://" + BUCKET_NAME + "/" + fileName;
    }

    /** Chuyển giọng nói từ file audio trên GCS thành văn bản */
    private String transcribeAudio(String gcsUri, String langCode) throws IOException, ExecutionException, InterruptedException {
        try (SpeechClient speechClient = SpeechClient.create()) {

            System.out.println("Gọi Speech-to-Text với mã ngôn ngữ: " + langCode);

            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
                    .setSampleRateHertz(48000)
                    .setAudioChannelCount(1)
                    .setLanguageCode(langCode)

                    // *** THÊM CẤU HÌNH QUAN TRỌNG ĐỂ CẢI THIỆN ĐỘ CHÍNH XÁC ***
                    // Chỉ định rõ mô hình nhận dạng. "default" là một lựa chọn tốt và ổn định.
                    .setModel("default")

                    .setEnableAutomaticPunctuation(true)
                    .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

            OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
                    speechClient.longRunningRecognizeAsync(config, audio);

            while (!response.isDone()) {
                System.out.println("Đang chờ nhận dạng giọng nói...");
                Thread.sleep(3000); // Giảm thời gian chờ
            }

            StringBuilder transcription = new StringBuilder();
            for (SpeechRecognitionResult result : response.get().getResultsList()) {
                transcription.append(result.getAlternativesList().get(0).getTranscript());
            }

            return transcription.toString();
        }
    }
    private String translateDocument(String gcsSourceUri, String sourceLang, String targetLang, String mimeType)
            throws IOException {

        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            LocationName parent = LocationName.of(PROJECT_ID, "global");

            GcsSource gcsSource = GcsSource.newBuilder().setInputUri(gcsSourceUri).build();
            DocumentInputConfig inputConfig = DocumentInputConfig.newBuilder()
                    .setGcsSource(gcsSource)
                    .setMimeType(mimeType)
                    .build();

            // 1. Tạo một Job ID duy nhất cho lần dịch này
            String jobId = UUID.randomUUID().toString();
            // Chỉ định thư mục đích riêng biệt cho job này
            String outputUriPrefix = "gs://" + BUCKET_NAME + "/translated_documents/" + jobId + "/";

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

            System.out.println("Đang gửi yêu cầu dịch tài liệu...");
            // 2. Gọi API và chờ hoàn tất
            client.translateDocument(request);
            System.out.println("Dịch hoàn tất. Đang tìm kiếm file kết quả...");

            // 3. Tìm kiếm file kết quả thực sự trong thư mục đầu ra
            Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
            // Prefix để tìm kiếm (loại bỏ phần gs://bucket-name/)
            String prefixToSearch = "translated_documents/" + jobId + "/";

            Page<Blob> blobs = storage.list(BUCKET_NAME, Storage.BlobListOption.prefix(prefixToSearch));

            for (Blob blob : blobs.iterateAll()) {
                // Tìm thấy file! Đây chính là file đã dịch.
                String finalPath = "gs://" + BUCKET_NAME + "/" + blob.getName();
                System.out.println("Đã tìm thấy file kết quả: " + finalPath);
                return finalPath;
            }

            throw new FileNotFoundException("Không tìm thấy file đã dịch trong thư mục: " + outputUriPrefix);

        } catch (Exception e) {
            System.err.println("Lỗi chi tiết khi dịch tài liệu: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to translate document: " + e.getMessage(), e);
        }
    }

    private String generateSignedUrl(String gcsFilePath) throws IOException {
        // gcsFilePath có dạng gs://bucket-name/path/to/file.pdf
        // Chúng ta cần lấy ra objectName là "path/to/file.pdf"
        String bucketName = BUCKET_NAME;
        String objectName = gcsFilePath.replace("gs://" + bucketName + "/", "");

        Storage storage = StorageOptions.newBuilder().setProjectId(PROJECT_ID).build().getService();
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();

        // Tạo URL có hiệu lực trong 15 phút
        java.net.URL url = storage.signUrl(blobInfo, 15, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }

}