package com.gxa.wyq.utils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;

/**
 * 如果你的项目是JDK7以下的话，需要谨慎传入包名，防止永久代溢出。
 * 比如说传入的是 com ， 由于com可能会有很多不相关的类被加载，JDK7以下是需要占用永久代空间的。
 * 而且永久代是固定的，很容易由于 class 过多而OOM溢出
 */
public class MyClassScannerUtil {
    private static List<Class<?>> classList = new ArrayList<>();
    private static List<Class<?>> jarList = new ArrayList<>();

    public static List<Class<?>> getClassList() {
        return classList;
    }

    public static List<Class<?>> getJarList() {
        return jarList;
    }

    public static void clearClassList(){
        classList.clear();
    }

    private MyClassScannerUtil(){}

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        classScanner("com.tuling.mapper");
        for (Class<?> aClass : getClassList()) {
            System.out.println(aClass);
        }
    }
    /**
     * 包扫描
     * @param basePackageStr
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void classScanner(String basePackageStr) throws IOException, ClassNotFoundException {
        if (basePackageStr == null || "".equals(basePackageStr)){
            return;
        }
        if (basePackageStr.contains(".")){
            //如果给的扫描路径中包含.  则将.替换成系统文件路径符
            basePackageStr = basePackageStr.replaceAll("\\.","/");
        }
        Enumeration<URL> basePackage = Thread.currentThread().getContextClassLoader().getResources(basePackageStr);
        while (basePackage.hasMoreElements()){
            URL url = basePackage.nextElement();
            //该url类型可能是jar文件也可能是file目录
            if (url.getProtocol().equals("file")){
                List<File> classes = new ArrayList<>();
                //遍历到所有的class文件的路径
                fileList(new File(url.getFile()),classes);
                loadClasses(classes, basePackageStr);
            }else if (url.getProtocol().equals("jar")){
                //如果是jar文件类型 我们利用JarURLConnection 类来遍历jar里面文件
                JarURLConnection jarURLConnection = (JarURLConnection)url.openConnection();
                Enumeration<JarEntry> entries = jarURLConnection.getJarFile().entries();
                //遍历得到的jar文件
                while (entries.hasMoreElements()){
                    // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                    JarEntry jarEntry = entries.nextElement();
                        //得到该jar文件下面的类实体
                    if (jarEntry.getName().endsWith(".class")) {
                        //把路径当中的\\替换成/
                        String allPath = jarEntry.getName().replaceAll("\\\\","/");
                        //处理去掉盘符得到真正的比如com/tuling/ 这样的一个路径
                        String relPath = allPath.substring(allPath.lastIndexOf(basePackageStr));
                        //去掉后缀 再把路径替换成com.tuling这样的一个真实全限定名
                        String className = relPath.replace(".class", "").replaceAll("/", ".");
                        jarList.add(Class.forName(className));
                    }
                }
            }
        }
    }
    /**
     * 递归扫描basePackage中的class文件
     * @param dir
     * @param fileList
     */
    private static void fileList(File dir, List<File> fileList){
        if (dir.isDirectory()){
            //如果是目录
            for (File f : dir.listFiles()) {
                //递归扫描
                fileList(f,fileList);
            }
        }else {
            //如果不是目录（则代表是文件）
            if (dir.getName().endsWith(".class")){
                //则将class文件添加到list集合中
                fileList.add(dir);
            }
        }
    }

    /**
     * 将得到的真正路径收集起来加到集合当中
     * @param fileList
     * @param basePackageStr
     * @return
     */
    private static void loadClasses(List<File> fileList,String basePackageStr){
        List<Class> classes = new ArrayList<>();
        for (File file : fileList) {
            //获取每一个文件的绝对路径，把系统分隔符替换成类路径的”.“
            String allPath = file.getAbsolutePath().replaceAll("\\\\", "/");
            //去掉前面的盘符   该路径则是类的全路径名
            String relPath = allPath.substring(allPath.lastIndexOf(basePackageStr));
            //去掉后缀.class  并且将真实路径替换成.这样的全限定名
            String className = relPath.replace(".class", "").replaceAll("/", ".");
            //添加到类的集合中
            try {
                classList.add(Class.forName(className));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
