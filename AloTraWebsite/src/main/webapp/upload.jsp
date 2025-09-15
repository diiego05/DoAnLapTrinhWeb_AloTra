<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Upload ảnh lên Cloudinary</title>
</head>
<body>
<h2>Chọn ảnh để upload</h2>
<input type="file" id="fileInput" accept="image/*">
<br>
<img id="preview" style="max-width:200px; display:none; margin-top:10px;">
<br>
<button onclick="uploadImage()">Upload</button>
<div id="result" style="margin-top:20px;"></div>

<script>
document.getElementById('fileInput').addEventListener('change', function(){
    const file = this.files[0];
    if(file){
        const reader = new FileReader();
        reader.onload = function(e){
            const img = document.getElementById('preview');
            img.src = e.target.result;
            img.style.display = 'block';
        }
        reader.readAsDataURL(file);
    }
});

async function uploadImage(){
    const file = document.getElementById('fileInput').files[0];
    if(!file){
        alert("Vui lòng chọn ảnh trước!");
        return;
    }
    const formData = new FormData();
    formData.append('file', file);

    const res = await fetch('upload', { method: 'POST', body: formData });
    const text = await res.text();
    document.getElementById('result').innerHTML = text;
}
</script>
</body>
</html>
