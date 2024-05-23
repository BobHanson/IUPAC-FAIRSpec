package com.integratedgraphics.crawler;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.integratedgraphics.util.XmlReader;

import java.util.Stack;
import java.util.TreeMap;

import javajs.util.PT;
import javajs.util.Rdr;

/**
 * A DataCite metadata crawler, resulting in the production of an IUPAC FAIRSpec
 * FindingAid for a repository-based or distributed data collection.
 *
 * @author Bob Hanson
 *
 */

public class Crawler extends XmlReader {
	public final static String DOI_ORG = "https://doi.org/";
	public final static String DATACITE_METADATA = "https://data.datacite.org/application/vnd.datacite.datacite+xml/";

	private final static String testPID = "10.14469/hpc/10386";
	private String thisPID;
	private URL thisURL;
	private URL dataCiteMetadataURL;
	private int urlDepth;
	private List<String> ifdList;
	private String ifdLine = "";
	private int nReps;

	private boolean isTop = true;
	private boolean skipping = false;
	private boolean isSilent = false;

	private int xmlDepth = 0;
	private static String indent = "                              ";
	private Stack<Map<String, String>> thisAttrs = new Stack<>();
	private Stack<Map<String, List<String>>> thisRelated = new Stack<>();
	private String pidPath;
	private Runnable output;
	private File dataDir;

	public Crawler(String pid) {
		thisPID = (pid == null ? testPID : pid);
		try {
			thisURL = new URL(DOI_ORG + thisPID);
			dataCiteMetadataURL = getMetadataURL(testPID);
		} catch (MalformedURLException e) {
			addException(e);
		}
	}

	private boolean startCrawling(File dataDir, Runnable output) {
		this.dataDir = dataDir;
		this.output = output;
		log = new StringBuffer();
		ifdList = new ArrayList<String>();
		nextMetadata(null);
		return true;
		
	}

	private URL getMetadataURL(String pid) throws MalformedURLException {
		System.out.println(pid);
		return new URL(DATACITE_METADATA + pid);
	}

	private URL newURL(String s) throws MalformedURLException {
		s = PT.rep(s, "&amp;", "&");
		return new URL(s);
	}

	private boolean nextMetadata(URL url) {
		urlDepth++;
		if (url == null) {
			url = dataCiteMetadataURL;
		}
		thisURL = url;
		isTop = (urlDepth == 1);
		skipping = !isTop;
		String currentPath = pidPath;
		String s = url.toString().replace(DATACITE_METADATA, "");
		String sfile = cleanFileName(s) + ".xml";
		int pt = s.lastIndexOf('/');
		String dir = s.substring(0, pt + 1);
		String file = s.substring(pt + 1);
		newLine();
		if (pidPath == null) {
			pidPath = s + ">";
		} else if (s.indexOf(dir) == 0) {
			pidPath += ">" + file + ">";
		} else {
			pidPath += ">" + s + ">";
		}
		addAttr("open", url.toString());
		InputStream is;
		try {
			File dataFile = new File(dataDir, sfile);
			boolean haveData = dataFile.exists();
			System.out.println("reading " + (haveData ? dataFile : url.toString()));
			if (haveData) {
				is = new FileInputStream(dataFile);
			} else {
				URLConnection con = url.openConnection();
				is = con.getInputStream();
			}
			byte[] metadata = getBytesAndClose(is);
			if (metadata instanceof byte[]) {
				if (!haveData)
					putToFile(dataFile, (byte[]) metadata);
				is = new ByteArrayInputStream((byte[]) metadata);
				parseXML(is);
				is.close();
			} else {
				addException(new RuntimeException(metadata.toString()));
			}
		} catch (Exception e) {
			addException(e);
		}
		addAttr("close", url.toString());
		newLine();
		pidPath = currentPath;
		output.run();
		urlDepth--;
		return true;
	}

	private static byte[] getBytesAndClose(InputStream is) throws IOException {
		return (byte[]) Rdr.getStreamAsBytes(new BufferedInputStream(is), null);
	}

	private static String cleanFileName(String s) {
		return s.replaceAll("[\\/?&:+]", "_");
	}

	/**
	 * process a representation
	 * 
	 * @param s
	 * @throws IOException
	 */
	private void processRepresentation(URL url) throws IOException {
		urlDepth++;
		nReps++;
		newLine();
		ifdLine += "REP";
		System.out.println("getContentHeaders: " + url);
		File headerFile = new File(dataDir, cleanFileName(url.toString()) + ".txt");
		boolean haveFile = headerFile.exists();
		if (haveFile) {
			appendData(getBytesAndClose(new FileInputStream(headerFile)));
		} else {
			int len = ifdLine.length();
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("HEAD");
			Map<String, List<String>> map = con.getHeaderFields();
			String content = map.get("Content-Type").get(0);
			String length = null;
			List<String> item = map.get("Content-Disposition");
			if (item != null) {
				List<String> list = map.get("Content-Length");
				if (list != null && !list.isEmpty())
					length = list.get(0);
				content = PT.getQuotedOrUnquotedAttribute(item.toString(), "filename");
			}
			addData("filename", content);
			if (length != null)
				addData("length", length);
			addData("mediaType", content);
			addData("URL", url.toString());
			putToFile(headerFile, ifdLine.substring(len).getBytes());
		}
		newLine();
		urlDepth--;
	}

	private void parseXML(InputStream content) throws Exception {
		ifdLine = "";
		thisRelated.push(new LinkedHashMap<String, List<String>>());
		BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(content), "UTF-8"));
		//$FALL-THROUGH$
		parseXML(reader);
		newLine();
		content.close();
		processRelated(thisRelated.pop());
	}

	
	private void processRelated(Map<String, List<String>> map) {
		try {
			List<String> list = map.get("DOI");
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					nextMetadata(getMetadataURL(list.get(i)));					
				}
			}
			list = map.get("URL");
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					processRepresentation(newURL(list.get(i)));
				}
			}
		} catch (Exception e) {
			addException(e);
		}
	}

	@Override
	protected void processStartElement(String localName, String nodeName) {
		xmlDepth++;
		boolean isData = true;
		switch (localName) {
		case "creators":
		case "dates":
		case "sizes":
		case "formats":
		case "version":
			skipping = true;
			break;
		case "publisher":
		case "publicationYear":
		case "resourceType":
		case "contributors":
		case "rightsList":
			skipping = !isTop;
			break;
		case "relatedidentifiers":
		case "subjects":
		case "title":
			isData = false;
			skipping = false;
			break;
		case "relatedidentifier":
		case "subject":
		case "description":
			// deferring to end
			isData = false;
			skipping = false;
			break;
		}
		addAttr("item", localName);
		for (Entry<String, String> e : atts.entrySet()) {
			if (isData && !skipping) {
				if (!isTop)
					System.out.println(e.getKey());
				addData(e.getKey(), e.getValue());
			} else {
				addAttr(e.getKey(), e.getValue());
			}
		}
		switch (localName) {
		case "relatedidentifier":
		case "description":
		case "subject":
			thisAttrs.push(atts);
			break;
		}
	}

	@Override
	protected void processEndElement(String localName) {
		String s = chars.toString().trim();
		chars.setLength(0);
		Map<String, String> attrs;
		switch (localName) {
		case "description":
			if (s.length() > 0) {
				addData("description", s);
			}
			break;
		case "title":
			if (s.length() > 0) {
				addDataFirst("title", s);
			}
			System.out.println(localName + "=" + s);
			break;
		case "subject":
			attrs = thisAttrs.pop();
			String key = attrs.get("subjectscheme");
			if (key != null) {
				addData(key, s);
			}
			break;
		case "relatedidentifier":
			if (s.length() > 0) {
				addAttr("relatedidentifier", s);
			}
			attrs = thisAttrs.pop();
			String type = (attrs.get("relationtype").equals("HasPart") ?
				attrs.get("relatedidentifiertype") : null);
			if (type != null) {
				Map<String, List<String>> map = thisRelated.get(thisRelated.size() - 1);
				List<String> list = map.get(type);
				if (list == null)
					map.put(type, list = new ArrayList<String>());
				list.add(s);
			}
			break;
		default:
			if (!skipping && s.length() > 0) {
				addAttr("value", s);
				addData(localName, s);
			}
			break;
		}
		xmlDepth--;
	}

	@Override
	protected void endDocument() {
		// to something here?
	}

	private void appendData(byte[] bytes) {
		String s = new String(bytes);
		ifdLine += s;
		log.append(s);
	}

	private void addData(String key, String value) {
		ifdLine += "\t" + key + "=" + cleanData(value);
		addAttr(key, value);
	}
	
	private void addDataFirst(String key, String value) {
		ifdLine = key + "=" + cleanData(value) + "\t" + ifdLine;
		addAttr(key, value);
	}
	
	private static String cleanData(String s) {
		return s.replaceAll("\t", "\\\\t").replaceAll("\r\n", "\\\\n").replaceAll("\n", "\\\\n");
	}

	private void newLine() {
		if (ifdLine.length() > 0) {
			String line = (pidPath == null ? "" : pidPath) + ifdLine;
			ifdList.add(line);
			System.out.println(line);
		}
		ifdLine = "";
	}
	private void addAttr(String key, String value) {
		appendLog(key + "=" + value);

	}

	private void addException(Exception e) {
		e.printStackTrace();
		addAttr("exception", e.getClass().getName() + ": " + e.getMessage());
	}

	private void appendLog(String line) {
		if (isSilent || skipping)
			return;
		line = line.trim();
		if (xmlDepth * 2 > indent.length())
			indent += indent;
		log.append('\n').append(urlDepth).append(".").append(xmlDepth).append(indent.substring(0, xmlDepth * 2))
		.append(line);
	}

	public static void main(String[] args) {
		Crawler crawler = new Crawler(args.length > 0 ? args[0] : null);
		String outdir = args.length > 1 ? args[1] : "c:/temp/crawler";
		File parent = new File(outdir);
		File dataDir = new File(outdir, "metadata");
		dataDir.mkdirs();
		long t = System.currentTimeMillis();
		crawler.startCrawling(dataDir, () -> {
			File f = new File(parent, "crawler.log");
			System.out.println("writing " + crawler.log.length() + " bytes " + f.getAbsolutePath());
			putToFile(f, crawler.log.toString().getBytes());
			f = new File(parent, "crawler.ifd.txt");
			System.out.println("writing " + crawler.ifdList.size() + " entries " + f.getAbsolutePath());
			System.out.println(crawler.nReps + " representations");
			try {
				FileOutputStream fos = new FileOutputStream(f);
				for (int i = 0; i < crawler.ifdList.size(); i++) {
					fos.write(crawler.ifdList.get(i).getBytes());
					fos.write((byte)'\n');
				}
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		System.out.println("done " + (System.currentTimeMillis() - t)/1000 + " sec");
	}

	private static void putToFile(File f, byte[] bytes) {
		try {
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(bytes);
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
