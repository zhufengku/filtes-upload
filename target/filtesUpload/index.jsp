<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>文件上传</title>
</head>
<body>
<form action="${pageContext.request.contextPath}/upload.do" method="post" enctype="multipart/form-data">
    <p>上传用户：<input type="text" name="name"></p>
    <p><input type="file" name="file1"></p>
    <p><input type="file" name="file1"></p>
    <p><input type="submit"> | <input type="reset"></p>

</form>
</body>
</html>
