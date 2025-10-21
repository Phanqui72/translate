<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Multilingual AI Translator</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css"> <!-- Thêm Font Awesome cho icons -->
</head>
<body>

<div class="translator-container">
    <header>
        <div class="logo">
            <!-- Icon thay thế cho logo -->
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M4 15.25C4 13.3453 5.42381 11.8322 7.25 11.672V5.5C7.25 4.94772 7.69772 4.5 8.25 4.5H15.75C16.3023 4.5 16.75 4.94772 16.75 5.5V11.672C18.5762 11.8322 20 13.3453 20 15.25C20 17.1547 18.5762 18.6678 16.75 18.828V19.5C16.75 20.0523 16.3023 20.5 15.75 20.5H8.25C7.69772 20.5 7.25 20.0523 7.25 19.5V18.828C5.42381 18.6678 4 17.1547 4 15.25Z" stroke="#4a90e2" stroke-width="1.5"/>
                <path d="M9.5 8H14.5" stroke="#4a90e2" stroke-width="1.5" stroke-linecap="round"/>
                <path d="M9.5 15H14.5" stroke="#4a90e2" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            <h1>Multilingual AI Translator</h1>
        </div>
        <p>Translate across text, images, and speech with Gemini</p>
    </header>

    <div class="tabs">
        <button class="tab-link active" onclick="openTab(event, 'Text')"><i class="fas fa-font"></i> Text</button>
        <button class="tab-link" onclick="openTab(event, 'Image')"><i class="fas fa-image"></i> Image</button>
        <button class="tab-link" onclick="openTab(event, 'Audio')"><i class="fas fa-microphone"></i> Audio</button>
        <button class="tab-link" onclick="openTab(event, 'Document')"><i class="fas fa-file-alt"></i> Document</button>
        <button class="tab-link" onclick="openTab(event, 'Video')"><i class="fas fa-video"></i> Video</button>
        <button class="tab-link" onclick="openTab(event, 'Web')"><i class="fas fa-globe"></i> Web</button>
    </div>

       <!-- Text Translation Tab -->
        <div id="Text" class="tab-content" style="display: block;">
            <form id="text-translation-form" action="translate" method="post">
                <input type="hidden" name="type" value="text">
                <div class="language-selectors">
                    <div class="from-language">
                        <label for="from-lang-text">From</label>
                        <select id="from-lang-text" name="fromLang">
                            <option value="en">English</option>
                            <option value="vi">Vietnamese</option>
                            <!-- Thêm các ngôn ngữ khác ở đây -->
                        </select>
                    </div>

                    <!-- *** THAY ĐỔI 1: Sửa lại nút bấm để dùng icon Font Awesome *** -->
                    <button type="button" class="swap-button">
                        <i class="fas fa-exchange-alt"></i>
                    </button>

                    <div class="to-language">
                        <label for="to-lang-text">To</label>
                        <select id="to-lang-text" name="toLang">
                            <option value="vi">Vietnamese</option>
                            <option value="en">English</option>
                            <!-- Thêm các ngôn ngữ khác ở đây -->
                        </select>
                    </div>
                </div>
                <div class="text-areas">
                    <!-- Thêm id="inputText" để JavaScript dễ dàng truy cập -->
                    <textarea id="inputText" name="inputText" placeholder="Enter text..."></textarea>
                    <textarea id="outputText" name="outputText" placeholder="Translation will appear here..." readonly></textarea>
                </div>
                <button type="submit" class="translate-button">Translate</button>
            </form>
        </div>

    <!-- Image Translation Tab - **CÓ THAY ĐỔI** -->
        <div id="Image" class="tab-content">
            <!-- **THAY ĐỔI 1: Thêm id cho form và các phần tử con** -->
            <form id="image-translation-form" action="translate" method="post" enctype="multipart/form-data">
                <input type="hidden" name="type" value="image">
                <div class="language-selectors">
                    <div class="from-language">
                        <label for="from-lang-image">From (language in image)</label>
                        <select id="from-lang-image" name="fromLang">
                            <option value="en">English</option>
                            <option value="vi">Vietnamese</option>
                        </select>
                    </div>
                    <div class="to-language">
                        <label for="to-lang-image">To</label>
                        <select id="to-lang-image" name="toLang">
                            <option value="vi">Vietnamese</option>
                            <option value="en">English</option>
                        </select>
                    </div>
                </div>
                <div class="io-areas">
                    <div class="upload-area">
                        <input type="file" id="image-upload" name="imageFile" accept="image/png, image/jpeg, image/webp" style="display: none;">
                        <!-- Thêm id cho label để cập nhật tên tệp -->
                        <label for="image-upload" id="image-upload-label">
                            <i class="fas fa-upload fa-2x"></i>
                            <p>Click to upload an image</p>
                            <span>PNG, JPG, WEBP</span>
                        </label>
                    </div>
                    <!-- Thêm id cho vùng output để hiển thị kết quả -->
                    <div class="output-area" id="image-output-area">
                        <!-- Kết quả dịch từ ảnh sẽ xuất hiện ở đây -->
                    </div>
                </div>
                <button type="submit" class="translate-button">Translate Image</button>
            </form>
        </div>

    <!-- Audio Translation Tab - CÓ THAY ĐỔI -->
        <div id="Audio" class="tab-content">
             <div class="audio-container">
                <p class="audio-instruction">Click the microphone to start/stop recording.</p>
                <div class="language-selectors">
                     <div class="from-language">
                        <label for="from-lang-audio">I'm speaking</label>
                        <select id="from-lang-audio" name="fromLang">
                            <option value="en-US">English (US)</option>
                            <option value="vi-VN">Vietnamese</option>
                            <!-- Thêm mã ngôn ngữ khác của Speech-to-Text -->
                        </select>
                    </div>
                     <div class="to-language">
                        <label for="to-lang-audio">Translate to</label>
                        <select id="to-lang-audio" name="toLang">
                            <option value="vi">Vietnamese</option>
                            <option value="en">English</option>
                        </select>
                    </div>
                </div>
                <!-- Thêm id cho nút mic -->
                <button class="mic-button" id="mic-button">
                    <i class="fas fa-microphone fa-3x"></i>
                </button>
                <!-- **THAY ĐỔI 1: Tách thành 2 ô để hiển thị kết quả** -->
                <textarea class="transcription-area" id="transcription-area" placeholder="Transcription will appear here..." readonly></textarea>
                <textarea class="transcription-area" id="audio-translation-output" placeholder="Translation will appear here..." readonly></textarea>
             </div>
        </div>

    <!-- Document Translation Tab -->
    <div id="Document" class="tab-content">
        <form action="translate" method="post" enctype="multipart/form-data">
            <input type="hidden" name="type" value="document">
             <div class="language-selectors">
                <div class="from-language">
                    <label for="from-lang-doc">From (language in document)</label>
                    <select id="from-lang-doc" name="fromLang">
                        <option value="en">English</option>
                        <option value="vi">Vietnamese</option>
                    </select>
                </div>
                <div class="to-language">
                    <label for="to-lang-doc">To</label>
                    <select id="to-lang-doc" name="toLang">
                        <option value="vi">Vietnamese</option>
                        <option value="en">English</option>
                    </select>
                </div>
            </div>
            <div class="io-areas">
                <div class="upload-area">
                    <input type="file" id="doc-upload" name="docFile" accept=".pdf,.txt,.docx" style="display: none;">
                    <label for="doc-upload">
                        <i class="fas fa-file-upload fa-2x"></i>
                        <p>Click to upload a document</p>
                        <span>PDF, TXT, DOCX, etc.</span>
                    </label>
                </div>
                <div class="output-area">
                    <!-- Link tải về tài liệu đã dịch sẽ xuất hiện ở đây -->
                </div>
            </div>
            <button type="submit" class="translate-button">Translate Document</button>
        </form>
    </div>

    <!-- Video Translation Tab -->
    <div id="Video" class="tab-content">
         <form action="translate" method="post" enctype="multipart/form-data">
            <input type="hidden" name="type" value="video">
             <div class="language-selectors single-row">
                 <div class="from-language">
                    <label for="from-lang-video">Original Language</label>
                    <select id="from-lang-video" name="fromLang">
                        <option value="en">English</option>
                        <option value="vi">Vietnamese</option>
                    </select>
                </div>
                <div class="to-language">
                    <label for="to-lang-video">Translate to</label>
                    <select id="to-lang-video" name="toLang">
                        <option value="vi">Vietnamese</option>
                        <option value="en">English</option>
                    </select>
                </div>
            </div>
            <div class="upload-area large">
                 <input type="file" id="video-upload" name="videoFile" accept="video/*" style="display: none;">
                 <label for="video-upload">
                    <i class="fas fa-video fa-3x"></i>
                    <p>Click to upload a video</p>
                </label>
            </div>
             <button type="submit" class="translate-button">Translate Video</button>
        </form>
    </div>

    <!-- Web Translation Tab - CÓ THAY ĐỔI -->
        <div id="Web" class="tab-content">
           <form id="web-translation-form" action="translate" method="post">
               <input type="hidden" name="type" value="web">
               <div class="web-input-bar">
                   <div class="url-input">
                       <label for="website-url">Website URL</label>
                       <input type="url" id="website-url" name="websiteUrl" placeholder="https://example.com" required>
                   </div>
                   <div class="to-language">
                       <label for="to-lang-web">Translate To</label>
                        <select id="to-lang-web" name="toLang">
                            <option value="vi">Vietnamese</option>
                            <option value="en">English</option>
                        </select>
                   </div>
               </div>
               <button type="submit" class="translate-button">Translate Website</button>

               <!-- **THAY ĐỔI: Sử dụng TEXTAREA thay cho iframe** -->
               <textarea id="web-output-area" class="transcription-area" readonly placeholder="Translated website content will appear here..."></textarea>

           </form>
        </div>

    <footer>
        <p>Translate with Google Cloud Translate API by QUI </p>
    </footer>

</div>

<!-- ==========================================================================
   KHỐI JAVASCRIPT DUY NHẤT VÀ ĐÃ ĐƯỢC TỔ CHỨC LẠI
   ========================================================================== -->
<script>
    // ----------------- CHỨC NĂNG CHUNG -----------------

    // Hàm chuyển tab
    function openTab(evt, tabName) {
        let i, tabcontent, tablinks;
        tabcontent = document.getElementsByClassName("tab-content");
        for (i = 0; i < tabcontent.length; i++) {
            tabcontent[i].style.display = "none";
        }
        tablinks = document.getElementsByClassName("tab-link");
        for (i = 0; i < tablinks.length; i++) {
            tablinks[i].className = tablinks[i].className.replace(" active", "");
        }
        document.getElementById(tabName).style.display = "block";
        evt.currentTarget.className += " active";
    }

    // Hàm tráo đổi ngôn ngữ
    document.querySelectorAll('.swap-button').forEach(button => {
        button.addEventListener('click', (event) => {
            const container = event.currentTarget.closest('.tab-content');
            if (!container) return;
            const fromSelect = container.querySelector('.from-language select');
            const toSelect = container.querySelector('.to-language select');
            const tempLang = fromSelect.value;
            fromSelect.value = toSelect.value;
            toSelect.value = tempLang;
            const fromTextArea = container.querySelector('#inputText');
            const toTextArea = container.querySelector('#outputText');
            if (fromTextArea && toTextArea) {
                const tempText = fromTextArea.value;
                fromTextArea.value = toTextArea.value;
                toTextArea.value = tempText;
            }
        });
    });

    // ----------------- LOGIC CHO TỪNG TAB -----------------

    // --- TAB: TEXT ---
    document.getElementById('text-translation-form').addEventListener('submit', function (event) {
        event.preventDefault();
        const form = event.target;
        const formData = new FormData(form);
        const outputTextarea = document.getElementById('outputText');
        outputTextarea.value = 'Translating...';
        fetch(form.action, {
            method: form.method,
            body: new URLSearchParams(formData)
        })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.text();
        })
        .then(translatedText => {
            outputTextarea.value = translatedText;
        })
        .catch(error => {
            console.error('Error:', error);
            outputTextarea.value = 'Error during translation. Check console for details.';
        });
    });

    // --- TAB: IMAGE ---
    const imageUploadInput = document.getElementById('image-upload');
    const imageUploadLabel = document.getElementById('image-upload-label');
    imageUploadInput.addEventListener('change', function () {
        if (this.files && this.files.length > 0) {
            imageUploadLabel.innerHTML = '<i class="fas fa-file-image fa-2x"></i><p>' + this.files[0].name + '</p><span>Click again to change</span>';
        } else {
            imageUploadLabel.innerHTML = '<i class="fas fa-upload fa-2x"></i><p>Click to upload an image</p><span>PNG, JPG, WEBP</span>';
        }
    });

    document.getElementById('image-translation-form').addEventListener('submit', function (event) {
        event.preventDefault();
        const form = event.target;
        const formData = new FormData(form);
        const outputArea = document.getElementById('image-output-area');
        if (!formData.get('imageFile') || formData.get('imageFile').size === 0) {
            outputArea.textContent = 'Please select an image file first.';
            return;
        }
        outputArea.textContent = 'Analyzing image and translating...';
        fetch(form.action, {
            method: form.method,
            body: formData
        })
        .then(response => {
            if (!response.ok) throw new Error('Server error: ' + response.statusText);
            return response.text();
        })
        .then(translatedText => {
            outputArea.textContent = translatedText;
        })
        .catch(error => {
            console.error('Error:', error);
            outputArea.textContent = 'Error during translation. Check console for details.';
        });
    });

    // --- TAB: AUDIO ---
    const micButton = document.getElementById('mic-button');
    const transcriptionArea = document.getElementById('transcription-area');
    const audioTranslationOutput = document.getElementById('audio-translation-output');
    const fromLangAudioSelect = document.getElementById('from-lang-audio');
    const toLangAudioSelect = document.getElementById('to-lang-audio');
    let isRecording = false;
    let mediaRecorder;
    let audioChunks = [];

    micButton.addEventListener('click', () => {
        if (isRecording) {
            if (mediaRecorder && mediaRecorder.state === "recording") mediaRecorder.stop();
            micButton.classList.remove('recording');
            isRecording = false;
        } else {
            navigator.mediaDevices.getUserMedia({ audio: true })
                .then(stream => {
                    isRecording = true;
                    micButton.classList.add('recording');
                    audioChunks = [];
                    transcriptionArea.value = "";
                    audioTranslationOutput.value = "";
                    transcriptionArea.placeholder = 'Recording... Click again to stop.';
                    mediaRecorder = new MediaRecorder(stream);
                    mediaRecorder.start();
                    mediaRecorder.ondataavailable = event => audioChunks.push(event.data);
                    mediaRecorder.onstop = () => {
                        stream.getTracks().forEach(track => track.stop());
                        if (audioChunks.length === 0) {
                            transcriptionArea.placeholder = 'No audio recorded. Please try again.';
                            return;
                        }
                        const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
                        transcriptionArea.placeholder = 'Processing audio... Please wait.';
                        sendAudioToServer(audioBlob);
                    };
                })
                .catch(err => {
                    console.error("Error accessing microphone:", err);
                    transcriptionArea.placeholder = "Could not access microphone. Please grant permission.";
                });
        }
    });

    function sendAudioToServer(audioBlob) {
        const formData = new FormData();
        formData.append('type', 'audio');
        formData.append('fromLang', fromLangAudioSelect.value);
        formData.append('toLang', toLangAudioSelect.value);
        formData.append('audioFile', audioBlob, 'recording.webm');
        fetch('translate', { method: 'POST', body: formData })
            .then(response => response.json())
            .then(data => {
                if (data.error) throw new Error(data.error);
                transcriptionArea.value = "Transcription: " + data.transcription;
                audioTranslationOutput.value = "Translation: " + data.translation;
            })
            .catch(error => {
                console.error('Lỗi trong quá trình xử lý âm thanh:', error);
                transcriptionArea.value = 'Lỗi: ' + error.message;
                audioTranslationOutput.value = '';
            });
    }

    // --- TAB: DOCUMENT ---
    const docUploadInput = document.getElementById('doc-upload');
    const docUploadLabel = docUploadInput.nextElementSibling;
    docUploadInput.addEventListener('change', function () {
        if (this.files && this.files.length > 0) {
            docUploadLabel.innerHTML = '<i class="fas fa-file-alt fa-2x"></i><p>' + this.files[0].name + '</p><span>Click again to change</span>';
        } else {
            docUploadLabel.innerHTML = '<i class="fas fa-file-upload fa-2x"></i><p>Click to upload a document</p><span>PDF, TXT, DOCX, etc.</span>';
        }
    });

    document.querySelector('#Document form').addEventListener('submit', function (event) {
        event.preventDefault();
        const form = event.target;
        const formData = new FormData(form);
        const outputArea = form.querySelector('.output-area');
        if (!formData.get('docFile') || formData.get('docFile').size === 0) {
            outputArea.innerHTML = '<p style="color: #ffcccc;">Please select a document file first.</p>';
            return;
        }
        outputArea.innerHTML = '<p>Uploading and translating document... This may take a moment.</p>';
        fetch('translate', { method: 'POST', body: formData })
            .then(response => response.json())
            .then(data => {
                if (data.error) throw new Error(data.error);
                outputArea.innerHTML = '<p>Translation complete!</p><a href="' + data.downloadUrl + '" download="' + data.fileName + '" class="download-link">Click here to download</a>';
            })
            .catch(error => {
                console.error('Error:', error);
                outputArea.innerHTML = '<p style="color: #ffcccc;">Error: ' + error.message + '</p>';
            });
    });

    // --- TAB: WEB ---
        // **THAY ĐỔI LOGIC JAVASCRIPT CHO TAB WEB**
        document.getElementById('web-translation-form').addEventListener('submit', function (event) {
            event.preventDefault();
            const form = event.target;
            const formData = new FormData(form);
            const outputArea = document.getElementById('web-output-area'); // Lấy textarea

            outputArea.value = 'Extracting and translating website... Please wait.';

            fetch('translate', {
                method: 'POST',
                body: new URLSearchParams(formData)
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok: ' + response.statusText);
                }
                return response.text(); // Lấy kết quả dưới dạng văn bản thuần túy
            })
            .then(translatedText => {
                // Đưa văn bản đã dịch vào textarea
                outputArea.value = translatedText;
            })
            .catch(error => {
                console.error('Error:', error);
                outputArea.value = 'Error during translation: ' + error.message;
            });
        });
</script>

</body>
</html>