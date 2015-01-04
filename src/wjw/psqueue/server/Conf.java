package wjw.psqueue.server;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * 配置文件
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "conf")
public class Conf {
	@XmlElement(required = true)
	public String bindAddress = "*"; //监听地址,*代表所有

	@XmlElement(required = true)
	public int bindPort = 1818; //监听端口

	@XmlElement(required = true)
	public int backlog = 200; //侦听 backlog 长度

	@XmlElement(required = true)
	public int soTimeout = 60; //HTTP请求的超时时间(秒)

	@XmlElement(required = true)
	public String defaultCharset = "UTF-8"; //缺省字符集
	@XmlTransient
	public Charset charsetDefaultCharset = Charset.forName(defaultCharset); //HTTP字符集

	@XmlElement(required = true)
	public int gcInterval = 30; //GC间隔时间(分钟)

	@XmlElement(required = true)
	public String adminUser = "admin"; //管理员用户名

	@XmlElement(required = true)
	public String adminPass = "123456"; //管理员口令

	@XmlElement(required = true)
	public int jmxPort = 1819; //JMX监听端口

	public static Conf load(String path) throws Exception {
		InputStream in = null;
		try {
			in = new FileInputStream(path);
			Conf conf = unmarshal(Conf.class, in);
			return conf;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
				}
			}
		}
	}

	public void store(String path) throws Exception {
		OutputStream out = null;
		try {
			out = new FileOutputStream(path);
			marshaller(this, out, true);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException ex) {
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T unmarshal(Class<T> docClass, InputStream inputStream) throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance(docClass);
		Unmarshaller u = jc.createUnmarshaller();
		T doc = (T) u.unmarshal(inputStream);
		return doc;
	}

	public static void marshaller(Object docObj, OutputStream pathname, boolean perttyFormat) throws JAXBException, IOException {
		JAXBContext context = JAXBContext.newInstance(docObj.getClass());
		Marshaller m = context.createMarshaller();
		if (perttyFormat) {
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		}
		m.marshal(docObj, pathname);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Conf [bindAddress=")
		    .append(bindAddress)
		    .append(", bindPort=")
		    .append(bindPort)
		    .append(", backlog=")
		    .append(backlog)
		    .append(", soTimeout=")
		    .append(soTimeout)
		    .append(", defaultCharset=")
		    .append(defaultCharset)
		    .append(", gcInterval=")
		    .append(gcInterval)
		    .append(", adminUser=")
		    .append(adminUser)
		    .append(", adminPass=")
		    .append("******")
		    .append(", jmxPort=")
		    .append(jmxPort)
		    .append("]");
		return builder.toString();
	}

}
