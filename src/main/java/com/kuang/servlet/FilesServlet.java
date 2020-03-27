package com.kuang.servlet;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.UUID;

//网络传输，pojo，实体类，都需要对象序列化，implements Serializable
public class FilesServlet extends HttpServlet implements Serializable {


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //判断上传的文件时普通表单还是带文件的表单
        if (!ServletFileUpload.isMultipartContent(req)) {
            return; //终止方法运行，说明这是一个上传普通表单，直接返回
        }

        //创建文件上传保存路径，建议在WEB-INF路径下，保证安全，用户无法直接访问上传的文件
        String realPath = this.getServletContext().getRealPath("/WEB-INF/uploadFile/upload");
        File uploadFile = new File(realPath);

        //判断上传文件的保存目录是否存在
        if (!uploadFile.exists() && !uploadFile.isDirectory()) {
            System.out.println(realPath + "目录不存在，需要创建！");
            //创建目录
            uploadFile.mkdirs();
        }

        //缓存，临时路径，假如文件超过了预期的大小，我们就将文件放到临时文件夹中，过几天自动删除，或提示用户转为永久
        String tmpPath = this.getServletContext().getRealPath("/WEB-INF/uploadFile/tmp");
        File tmpFile = new File(tmpPath);
        if (!tmpFile.exists() && !tmpFile.isDirectory()) {
            System.out.println(tmpPath + "目录不存在，需要创建！");
            //创建临时目录
            tmpFile.mkdirs();
        }


        //处理上传文件，一般需要通过流来处理，我们可以使用request.getInputStream(),原生的文件上传流获取，十分麻烦
        //建议使用Apache的文件上传组件来实现，common-fileupload,他需要依赖commons-io组件
        //消息提示
        String message = "";
        try {
            //1.创建DiskFileItemFactory对象，处理文件上传路径或者大小的限制
            DiskFileItemFactory factory = new DiskFileItemFactory();
            //通过这个工厂设置一个缓冲区，当上传的文件大于这个缓冲区的时候，将他放到临时文件夹中
            factory.setSizeThreshold(1024 * 1024);//缓存区大小为1M
            factory.setRepository(tmpFile);

            //2.创建一个文件上传解析器
            ServletFileUpload upload = new ServletFileUpload(factory);

            //监听文件上传进度：
            upload.setProgressListener(new ProgressListener() {
                //pBytesRead:已经读取到的文件大小
                //pContentLength：文件大小
                @Override
                public void update(long pBytesRead, long pContentLength, int pItems) {
                    System.out.println("总大小：" + pContentLength + "已上传：" + pBytesRead);
                }
            });
            //解决中文乱码
            upload.setHeaderEncoding("UTF-8");
            //设置单个文件内的最大值
            upload.setFileSizeMax(1024 * 1024 * 10);
            //设置总共能够上传的文件大小
            // ((1024=1kb) * 1024 = 1M) *10 =10M
            upload.setSizeMax(1024 * 1024 * 10);


            //3.处理上传文件，使用ServletFileUpload解析器解析上传数据，解析结果返回的是一个List<FileItem>集合，每一个FileItem对应一个Form表单的输入项
            List<FileItem> FileItemList = upload.parseRequest(req);
            for (FileItem fileItem : FileItemList) {
                //如果fileItem中封装的是普通输入项的数据
                if (fileItem.isFormField()) {
                    //获取字段名字
                    String name = fileItem.getFieldName();
                    //解决普通输入项中中文乱码问题
                    String value = fileItem.getString("UTF-8");//value = new String(value.getBytes("iso8859-1"),"UTF-8");
                    System.out.println(name + ":" + value);
                } else {
                    //如果表单中提交的是上传文件
                    //获得上传的文件名称
                    String filename = fileItem.getName();
                    System.out.println("上传的文件名"+filename);
                    if (filename == null || filename.trim().equals("")) {
                        continue;
                    }
                    //注意：不同的浏览器提交的文件名称是不一样的，有些浏览器提交的文件会带有路径，如“D:\\project\WebRoot\hello.jsp”，有一些是单纯的文件名：hello.jsp
                    //去掉获取到文件名中的路径名，保留单纯的文件名
                    filename = filename.substring(filename.lastIndexOf("\\") + 1);
                    //获取文件的后缀名
                    String fileExtName = filename.substring(filename.lastIndexOf(".") + 1);
                    //可以使用UUID保证文件名唯一
                    String uuID = UUID.randomUUID().toString();
                    String realUploadPath = realPath+"/"+  uuID ;
                    File realPathFile = new File(realUploadPath);
                    if (!realPathFile.exists() && !realPathFile.isDirectory()) {
                        System.out.println(realPathFile + "目录不存在，需要创建！");
                        //创建目录
                        realPathFile.mkdirs();
                    }

                    //4.获取fileItem中的上传文件的输入流
                    InputStream in = fileItem.getInputStream();
                    //创建一个文件输入流
                    FileOutputStream out = new FileOutputStream(realUploadPath + "\\" + filename);
                    //创建一个缓冲区
                    byte buffer[] = new byte[1024];
                    //判断输入流中的数据是否已经读取完毕的标志位
                    int len = 0;
                    //循环将输入流读入到缓冲区当中，（len = in.read(buffer)>0）就表示in里面还有数据存在
                    while ((len = in.read(buffer)) > 0) {
                        //使用FileOutputStream输出流将缓冲区的数据写入到指定的目录（path+"\\"+filename）当中
                        out.write(buffer, 0, len);

                    }
                    //关闭输入流
                    in.close();
                    //关闭输出流
                    out.close();
                    //删除处理文件上传生成的临时文件
                    fileItem.delete(); //上传成功，清除临时文件
                    message = "文件上传成功!";
                }
            }
        } catch (FileUploadException e) {
            message = "文件上传失败！";
            e.printStackTrace();
        }

        req.setAttribute("message", message);
        req.getRequestDispatcher("info.jsp").forward(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }
}
