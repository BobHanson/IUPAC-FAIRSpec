package com.integratedgraphics.extract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.iupac.fairspec.common.IFSException;
import org.iupac.fairspec.common.IFSFindingAid;
import org.iupac.fairspec.common.IFSReference;
import org.iupac.fairspec.common.IFSRepresentation;
import org.iupac.fairspec.spec.nmr.IFSNMRSpecDataRepresentation;

import com.integratedgraphics.util.Util;

import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.PT;

public class Extractor {

	public final static int EXTRACT_MODE_CHECK_ONLY = 0;
	public final static int EXTRACT_MODE_CREATE_CACHE = 1;
	public final static int EXTRACT_MODE_REPACKAGE_ZIP = 2;
		
	
	public Extractor() {
		clearZipCache();
	}

	private String extractVersion;
	private List<String> objects;
	private IFSFindingAid findingAid;
	private String sourceDir;
	
	protected static Pattern objectDefPattern = Pattern.compile("\\{([^:]+)::([^}]+)\\}");
	protected static Pattern pStarDotStar;

	private String zipPath;
	private File targetDir;

	/**
	 * files matched will be cached in the target directory
	 */
	protected Pattern cachePattern;

	/**
	 * a memory cache of representations
	 */
	protected List<IFSRepresentation> cache;

	public void setCachePattern(String s) {
		cachePattern = Pattern.compile(s);
		cache = new ArrayList<>();
	}
	
	/**
	 * files matched will be cached as zip files
	 */
	protected Pattern rezipCachePattern;
	protected List<IFSRepresentation> rezipCache;
	private Pattern rezipCacheExcludePattern;
	
	/**
	 * Set the file match zip cache pattern.
	 * 
	 * @param procs
	 * @param toExclude 
	 */
	public void setRezipCachePattern(String procs, String toExclude) {
		rezipCachePattern = Pattern.compile(procs);
		rezipCacheExcludePattern = Pattern.compile(toExclude);
		rezipCache = new ArrayList<>();
	}
	
	protected long cachedByteCount;
	private IFSRepresentation nextRezip;
	private String nextRezipName;
	private String rootPath;
	private Object lastRezipPath;
	
	public List<String> getObjectsForFile(File ifsExtractScript) throws IOException {
		System.out.println("Extracting " + ifsExtractScript.getAbsolutePath());
		return getObjectsForStream(ifsExtractScript.toURI().toURL().openStream());
	}

	public List<String> getObjectsForStream(InputStream is) throws IOException {
		byte[] bytes = Util.getLimitedStreamBytes(is, -1, null, true, true);
		String script = new String(bytes);
		return objects = parseScript(script);
	}

	/**
	 * 
	 * @param script
	 * @return parsed list of objects from an IFS-extract.js JSON
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private List<String> parseScript(String script) throws IOException {
		Map<String, Object> jp = (Map<String, Object>) new JSJSONParser().parse(script, false);
		if (debugging)
			System.out.println(jp);
		extractVersion = (String) jp.get("IFS-extract-version");
		System.out.println(extractVersion);
		List<Map<String, Object>> pathway = (List<Map<String, Object>>) jp.get("pathway");
		List<String> objects = getObjects(pathway);
		System.out.println(objects.size() + " objects found");
		return objects;
	}

	/**
	 * Make all variable substitutions in IFS-extract.js.
	 * 
	 * @return list of IFSObject definitions
	 */
	private List<String> getObjects(List<Map<String, Object>> pathway) {

		// input:

		// {"IFS-extract-version":"0.1.0-alpha","pathway":[
		// {"hash":"0c00571"},
		// {"pubid":"acs.orglett.{hash}"},
		// {"src":"IFS.finding.aid.source.publication.uri::https://doi.org/10.1021/{pubid}"},
		// {"data":"{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/{pubid}/suppl_file/ol{hash}_si_002.zip"},
		//
		// {"path":"{data}|FID for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}"},
		// {"objects":"{path}/{IFS.structure.representation.mol.2d::{id}.mol}"},
		// {"objects":"{path}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}"},
		// {"objects":"{path}/HRMS.zip|{IFS.ms.representation.pdf::**/*.pdf}"},
		// ]}

		// output:

		// [
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}"
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}"
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/HRMS.zip|{IFS.ms.representation.pdf::**/*.pdf}"
		// ]

		Lst<String> keys = new Lst<>();
		Lst<String> values = new Lst<>();
		List<String> objects = new ArrayList<>();
		for (int i = 0; i < pathway.size(); i++) {
			Map<String, Object> def = pathway.get(i);
			for (Entry<String, Object> e : def.entrySet()) {
				String key = e.getKey();
				String val = (String) e.getValue();
				if (val.indexOf("{") >= 0) {
					String s = PT.replaceStrings(val, keys, values);
					if (!s.equals(val)) {
						if (debugging)
							System.out.println(val + "\n" + s + "\n");
						e.setValue(s);
					}
					val = s;
				}
				if (key.equals("objects")) {
					objects.add("{IFS.finding.aid.object::" + val + "}");
				} else {
					keys.addLast("{" + key + "}");
					values.addLast(val);
				}
			}
		}
		return objects;

	}

	/**
	 * Find and extract all objects of interest from a ZIP file. 
	 * 
	 */
	public Set<String> extractObjects(File targetDir) throws IFSException, IOException {

		this.targetDir = targetDir;
		targetDir.mkdir();
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}"
		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.structure.representation.mol.2d::{id}.mol}"

// [parse first node]

		// {IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}

// [start a new finding aid and download this resource]

		// |

// [get file list]

		// (\Q^FID for
		// Publication/\E)(\Q{id=IFS.structure.param.compound.id::*}\E)(\Q.zip\E)

// [pass to StructureIterator]		
// [find matches and add structures to finding aid structure collection]

		// |

// [get file list]
		// [for each structure...]

		// \Q{id}/\E(\Q{IFS.structure.representation.mol.2d::{id}.mol}/E)"

		// add this representation to this structure

		// "{IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}|FID
		// for
		// Publication/{id=IFS.structure.param.compound.id::*}.zip|{id}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}"

		// [parse first node]

		// {IFS.finding.aid.source.data.uri::https://pubs.acs.org/doi/suppl/10.1021/acs.orglett.0c00571/suppl_file/ol0c00571_si_002.zip}

		// [start a new finding aid and download this resource]

		// |

		// [get file list]

		// FID for Publication/{id=IFS.structure.param.compound.id::*}.zip

		// [pass to StructureIterator]
		// [find matches and add structures to finding aid structure collection if
		// necessary

		// |

		// [get file list]

		// {id}/{IFS.nmr.representation.vender.dataset::{IFS.nmr.param.expt::*}-NMR.zip}

		// [pass to SpecDataIterator]
		// [find matches and add NMR spec data to finding aid spec data collection; also
		// add struc+spec to finding aid StrucSpecCollection]

		// bruker directories identified with / just before vendor dataset closing }
		// standard ^....$
		// * at end just removes $
		// *. becomes [^.]
		// . becomes [.]
		// **/*.pdf becomes (?:[^/]+/)*).+\Q.pdf\E

//		String s = "test/ok/here/1c.pdf";    // test/**/*.pdf
//		Pattern p = Pattern.compile("^\\Qtest\\E/(?:[^/]+/)*(.+\\Q.pdf\\E)$");
//		Matcher m = p.matcher(s);
//		System.out.println(m.find() ? m.groupCount() + " " + m.group(0) + " -- " + m.group(1) : "");

		System.out.println("=====");

		if (sourceDir != null)
			System.out.println("extractObjects from " + sourceDir);
		System.out.println("extractObjects to " + targetDir.getAbsolutePath());

		int[] pt = new int[1];

		for (int i = 0; i < objects.size(); i++) {
			String sObj = objects.get(i);
			System.out.println("found object " + sObj);
			
			// next should never be a problem, as we do this wrapping ourselves
			pt[0] = 0;
			String sAid = getValue(sObj, "IFS.finding.aid.object", pt);
			if (sAid == null)
				throw new IFSException("no IFS.finding.aid.object:" + sObj);
			
			// must have a source
			pt[0] = 0;
			String sUrl = getValue(sAid, "IFS.finding.aid.source.data.uri", pt);
			if (sUrl == null)
				throw new IFSException("no IFS.finding.aid.source.data.uri:" + sAid);		
			String sData = sAid.substring(pt[0] + 1); // skip first "|"
			if (findingAid == null)
				findingAid = new IFSFindingAid(sObj, sUrl);
			sUrl = localizeURL(sUrl);
			if (debugging)
				System.out.println("opening " + sUrl);
			zipPath = sUrl.substring(sUrl.lastIndexOf(":") + 1);
			rootPath = new File(zipPath).getName();
			lastRezipPath = null;
			Map<String, ZipEntry> files = getZipContents(sUrl);
			if (rezipCache != null && rezipCache.size() > 0) {
				// this will drain the rezipCache
				getNextRezipName();
				readZipContentsIteratively(new URL(sUrl).openStream(), null, "", true);
			}
			zipPath = null;
			findingAid.getZipContents().putAll(files);
			int nFound = processObject(sData, files.keySet());
			System.out.println("found " + nFound + " objects");
		}
		findingAid.finalizeExtraction();		
		return findingAid.getZipContents().keySet();
	}

	private int processObject(String sData, Set<String> set) throws IFSException {
		ParseObject po = new ParseObject(sData);
		int n = 0;
		//System.out.println(sData);
		//System.out.println(po.p);
		for (String fname : set) {
			//System.out.println(fname);
			Matcher m = po.p.matcher(fname);
			if (m.find()) {
				n++;
				findingAid.beginAddObject(fname);
				if (debugging)
					System.out.println("found " + fname);
				for (String key : po.keys.keySet()) {
					String param = po.keys.get(key);
					String value = m.group(key);
					findingAid.addObject(param, value);
					if (debugging)
						System.out.println("found " + param + " " + value);;
				}
				findingAid.endAddObject();

			}
		}
		return n;
	}

	/**
	 * For testing (or for whatever reason zip files are local or should not use the
	 * given source paths), replace https://......./ with sourceDir/
	 * 
	 * @param sUrl
	 * @return localized URL
	 */
	private String localizeURL(String sUrl) {
		if (sourceDir != null) {
			int pt = sUrl.lastIndexOf("/");
			sUrl = sourceDir + sUrl.substring(pt);
		}
		return sUrl;
	}

	private static String getValue(String sObj, String key, int[] pt) throws IFSException {
		key = "{" + key + "::";
		int p = sObj.indexOf(key, pt[0]);
		if (p < 0)
			return null;
		int q = -1;
		int nBrace = 1;
		p += key.length();
		int len = sObj.length();
		for (int i = p; i < len && nBrace > 0; i++) {
			switch (sObj.charAt(i)) {
			case '{':
				q = i;
				nBrace++;
				break;
			case '}':
				if (--nBrace < 0) {
					throw new IFSException("unopened '}' in " + sObj + " at char " + i);
				}
				q = i;
				break;
			}
		}
		if (nBrace > 0) {
			throw new IFSException("unclosed '{' in " + sObj + " at char " + q);
		}
		pt[0] = q;
		return sObj.substring(p, pt[0]++);
	}

	private static Map<String, Map<String, ZipEntry>> htZipContents = new HashMap<>();

	static boolean debugging;

	/**
	 * zip contents caching can save time in complex loading.
	 * 
	 */
	public static void clearZipCache() {
		htZipContents.clear();
	}

//	/**
//	 * Retrieve the ZipEntry for a file in a given zip file.
//	 * 
//	 * Note that in JavaScript only, the ZipEntry returned contains a reference to
//	 * the underlying ByteArrayInputStream ultimately backing this zip file. This
//	 * allows us to retrieve the file data directly from the ZipEntry using
//	 * jsutil.getZipBytes(zipEntry).
//	 * 
//	 * Names starting with "." are taken as case-insensitive extensions
//	 * 
//	 * 
//	 * @param zipFile
//	 * @param fileName   zip entry name or lower-case extension starting with "."
//	 * @param isContains fileName is a fragment of the entry name, not the other way
//	 *                   around
//	 * @return the ZipEntry for this file, possibly cached.
//	 */
//	private static ZipEntry findZipEntry(String zipFile, String fileName, boolean isContains) {
//		Map<String, ZipEntry> contents = getZipContents(zipFile);
//		if (contents == null)
//			return null;
//		if (!isContains) {
//			return contents.get(fileName);
//		}
//		boolean isLCExt = fileName.startsWith(".");
//		for (Entry<String, ZipEntry> entry : contents.entrySet()) {
//			String key = entry.getKey();
//			if ((isLCExt ? key.toLowerCase() : key).indexOf(fileName) >= 0)
//				return entry.getValue();
//		}
//		return null;
//	}

	/**
	 * Gets the contents of a zip file.
	 * 
	 * @param zipPath the path to the zip file
	 * @return a set of file names in alphabetical order
	 * @throws IOException
	 */
	public Map<String, ZipEntry> getZipContents(String zipPath) throws IOException {
		URL url = new URL(zipPath);// getURLWithCachedBytes(zipPath); // BH carry over bytes if we have them
									// already
		Map<String, ZipEntry> fileNames = htZipContents.get(url.toString());
		if (fileNames != null)
			return fileNames;
		// Scan URL zip stream for files.
		fileNames = new LinkedHashMap<String, ZipEntry>();
		htZipContents.put(url.toString(), fileNames);
		return readZipContentsIteratively(url.openStream(), fileNames, "", false);
	}

	private Map<String, ZipEntry> readZipContentsIteratively(InputStream is, Map<String, ZipEntry> fileNames,
			String baseName, boolean doRezip) throws IOException {
		if (debugging && baseName.length() > 0)
			System.out.println("opening " + baseName);
		boolean isTopLevel = (baseName.length() == 0);
		ZipInputStream zis = new ZipInputStream(is);
		ZipEntry zipEntry = null;
		int n = 0;
		ZipEntry rezipEntry = null;
		while ((zipEntry = (rezipEntry == null ? zis.getNextEntry() : rezipEntry)) != null) {
			rezipEntry = null;
			if (!zipEntry.isDirectory() && zipEntry.getSize() == 0)
				continue;
			n++;
			String zipname = baseName + zipEntry.getName();
			if (zipname.indexOf("MACOSX") >= 0) // Test 9: acs.orglett.0c01153/22284726,22284729
				continue;
			if (debugging)
				System.out.println(zipname);
			if (fileNames != null)
				fileNames.put(zipname, zipEntry); // Java has no use for the ZipEntry, but JavaScript can read it.
			if (zipname.endsWith(".zip")) {
				readZipContentsIteratively(zis, fileNames, zipname + "|", doRezip);
			} else if (doRezip) {
				if (zipname.equals(nextRezipName)) {
					rezipEntry = rezip(zis, zipname, zipEntry);
					getNextRezipName();
				}
			} else {	
				checkToCache(zipname, zis, zipEntry);
			}
		}
		if (isTopLevel)
			zis.close();
		return fileNames;
	}


	private ZipEntry rezip(ZipInputStream zis, String fname, ZipEntry entry) throws IOException {
		System.out.println("rezipping " + fname + " for " + entry + " " + new File(entry.getName()).getName());
		File outFile = getFileTarget(fname + ".zip");
		FileOutputStream fos = new FileOutputStream(outFile);
		ZipOutputStream zos = new ZipOutputStream(fos);
		String dirName = entry.getName();
		String parent = new File(entry.getName()).getParent();
		int lenOffset = (parent == null ? 0 : parent.length() + 1);
		while (entry != null && entry.getName().startsWith(dirName)) {
			String outName = entry.getName().substring(lenOffset);
			if (!entry.isDirectory()
					&& (rezipCacheExcludePattern == null || !rezipCacheExcludePattern.matcher(outName).find())) {
				zos.putNextEntry(new ZipEntry(outName));
				if (entry.getSize() != 0)
					Util.getLimitedStreamBytes(zis, entry.getSize(), zos, false, false);
				zos.closeEntry();
			}
			entry = zis.getNextEntry();
		}
		zos.close();
		fos.close();
		return entry;
	}

	private void getNextRezipName() {
		if (rezipCache.size() == 0) {
			nextRezipName = null;
			nextRezip = null;
		} else {
			nextRezipName = (nextRezip = rezipCache.remove(0)).getRef().getRef();
		}
	}

	private void checkToCache(String zipname, InputStream zis, ZipEntry zipEntry) throws FileNotFoundException, IOException {
		if (cachePattern != null && cachePattern.matcher(zipname).find()) {
			File f = getFileTarget(zipname);
			String fname = rootPath + "|" + zipname;
			long len = zipEntry.getSize();
			Util.getLimitedStreamBytes(zis, len, new FileOutputStream(f), false, true);
			String type = IFSNMRSpecDataRepresentation.getNMRTypeFromName(fname);
			System.out.println("! caching " + type + " " + len + " " + fname);
			cachedByteCount += len;
			cache.add(new IFSRepresentation(type, new IFSReference(fname), f));
		}

		Matcher m;
		if (rezipCachePattern != null && (m = rezipCachePattern.matcher(zipname)).find()) {
			String fname = m.group("path");
			if (fname.equals(lastRezipPath)) {
				System.out.println("duplicate path " + fname);
			} else {
				lastRezipPath = fname;
				IFSRepresentation ref = new IFSRepresentation("bruker.zip", new IFSReference(fname), fname);
				rezipCache.add(ref);
				System.out.println("! rezip added " + fname);
			}
		}

	}
	
	private File getFileTarget(String fname) {
		return new File(targetDir + "/" + (rootPath + "|" + fname).replace('|', '_').replace('/', '_').replace(' ', '_'));
	}

	public void setSourceDir(String sourceDir) {
		this.sourceDir = sourceDir;
	}

	/**
	 * A class for parsing the object string and using regex to match filenames.
	 * 
	 * @author hansonr
	 *
	 */

	static class ParseObject {

		private String sData;

		private Pattern p;

		private List<String> regexList;

		private Map<String, String> keys;

		public ParseObject(String sData) throws IFSException {
			this.sData = sData;
			init();
		}

		/**
		 * Prepare pattern and match.
		 * 
		 * @throws IFSException
		 * 
		 * 
		 */
		private void init() throws IFSException {
			// Using Java and JavaScript named capture groups for in-line defining.
			//
			// {regex::[a-z]} is left unchanged and becomes \\E[a-z]\\Q
			//
			// **/ becomes \\E(?:[^/]+/)*)\\Q
			//
			// *-* becomes \\E([^-]+(?:-[^-]+)*)\\Q and matches a-b-c
			//
			// * becomes \\E.+\\Q
			//
			// {id=IFS.nmr.param.expt::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			//
			// {IFS.nmr.param.expt::xxx} becomes \\E(?<IFS0nmr0param0expt>\\Qxxx\\E)\\Q
			//
			// <id> becomes \\k<id>
			//
			// generally ... becomes ^\\Q...\\E$
			//
			// \\Q\\E in result is removed
			//
			// so:
			//
			// {IFS.nmr.param.expt::*} becomes \\E(?<IFS0nmr0param0expt>.+)\\Q
			//
			// {IFS.nmr.representation.vender.dataset::{IFS.structure.param.compound.id::*-*}-{IFS.nmr.param.expt::*}.jdf}
			//
			// becomes:
			//
			// ^(?<IFS0nmr0representation0vender0dataset>(?<IFS0structure0param0compound0id>([^-](?:-[^-]+)*))\\Q-\\E(?<IFS0nmr0param0expt>.+)\\Q.jdf\\E)$
			//
			// {id=IFS.structure.param.compound.id::*}.zip|{IFS.nmr.representation.vender.dataset::{id}_{IFS.nmr.param.expt::*}/}
			//
			// becomes:
			//
			// ^(?<id>*)\\Q.zip|\\E(?<IFS0nmr0representation0vender0dataset>\\k<id>\\Q_\\E(<IFS0nmr0param0expt>*)\\Q/\\E)$

			// so....

			// {regex::[a-z]} is left unchanged and becomes \\E[a-z]\\Q

			String s = fixRegex(null);

			// **/ becomes \\E(?:[^/]+/)*\\Q

			s = PT.rep(s, "**/", "\\E(?:[^/]+/)\2\\Q");

			Matcher m;
			// *-* becomes \\E([^-]+(?:-[^-]+)*)\\Q and matches a-b-c
			if (s.indexOf("*") != s.lastIndexOf("*")) {
				if (pStarDotStar == null)
					pStarDotStar = Pattern.compile("\\*(.)\\*");
				while ((m = pStarDotStar.matcher(s)).find()) {
					String schar = m.group(1);
					s = PT.rep(s, "*" + schar + "*",
							"\\E([^" + schar + "]+(?:" + schar + "[^" + schar + "]+)\2)\\Q");
				}
			}
			// * becomes \\E.+\\Q

			s = PT.rep(s, "*", "\\E[^|/]+\\Q");

			// {id=IFS.nmr.param.expt::xxx} becomes \\E(?<id>\\Qxxx\\E)\\Q
			// {IFS.nmr.param.expt::xxx} becomes \\E(?<IFS0nmr0param0expt>\\Qxxx\\E)\\Q
			// <id> becomes \\k<id>


			s = compileIFSDefs(s, true, true);
			
			// restore '*'
			s = s.replace('\2', '*');

			// restore regex
			// wrap with quotes and constraints ^\\Q...\\E$

			s = "^\\Q" + fixRegex(s) + "\\E$";

			// \\Q\\E in result is removed

			s = PT.rep(s, "\\Q\\E", "");

			System.out.println("pattern: " + s);
			p = Pattern.compile(s);
//			m = p.matcher("FID for Publication/S6.zip|S6/HRMS.zip|HRMS/67563_hazh180_maxis_pos.pdf");
//			System.out.println(m.find());
		}

		/**
		 * Find and regex-ify all {id=IFS.param::value} or {IFS.param::value}.
		 * 
		 * @param s growing regex string
		 * @return regex string with all {...} fixed
		 * @throws IFSException
		 */
		private String compileIFSDefs(String s, boolean isFull, boolean replaceK) throws IFSException {
			if (isFull)
				replaceK = (s.indexOf("<") >= 0);
			while (s.indexOf("::") >= 0) {
				Matcher m = objectDefPattern.matcher(s);
				if (!m.find())
					return s;
				String param = m.group(1);
				String val = m.group(2);
				String pv = "{" + param + "::" + val + "}";
				if (val.indexOf("::") >= 0)
					val = compileIFSDefs(val, false, replaceK);
				int pt = param.indexOf("=");
				if (pt == 0)
					throw new IFSException("bad {def=key::val} expression: " + param + "::" + val);
				if (keys == null)
					keys = new HashMap<String, String>();
				String key;
				if (pt > 0) {
					key = param.substring(0, pt);
					param = param.substring(pt + 1);
				} else {
					key = param.replace('.', '0');
				}
				keys.put(key, param);
				// escape < and > here
				s = PT.rep(s, pv, (replaceK ? "\\E(?\3" + key + "\4\\Q" : "\\E(?<" + key + ">\\Q") + val + "\\E)\\Q");
			}
			if (replaceK) {
				// now fix k< references and revert \3 \4
				s = PT.rep(s, "<", "\\E\\k<");
				s = PT.rep(s, ">", ">\\Q").replace('\3', '<').replace('\4', '>');
			}
			return s;
		}

		private String fixRegex(String s) throws IFSException {
			if (sData.indexOf("{regex::") < 0)
				return (s == null ? sData : s);
			if (s == null) {
				// init
				s = sData;
				regexList = new ArrayList<>();
				int[] pt = new int[1];
				int i = 0;
				while ((pt[0] = s.indexOf("{regex::")) >= 0) {
					// save regex and replace by \0n\1
					int p0 = pt[0];
					String rx = getValue(s, "regex", pt);
					regexList.add("\\E" + rx + "\\Q");
					s = s.substring(0, p0) + "\0" + (i++) + "\1" + s.substring(pt[0]);
				}
			} else {
				// restore regex
				int p;
				while ((p = s.indexOf('\0')) >= 0) {
					int p2 = s.indexOf('\1');
					int i = Integer.parseInt(s.substring(p + 1, p2));
					s = s.substring(0, p) + regexList.get(i) + s.substring(p2 + 1);
				}
			}
			return s;
		}

	}

}